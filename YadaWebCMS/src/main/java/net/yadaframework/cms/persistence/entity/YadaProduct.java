package net.yadaframework.cms.persistence.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.springframework.context.i18n.LocaleContextHolder;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.CloneableFiltered;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

/**
 * A Product is an "abstract" item because it groups similar objects that differ in color, size or other attributes.
 * So a "Paris T-Shirt" is a very specific product but it comes in different sizes, so it doesn't exist unless
 * you specify the size.
 * The Medium Paris T-Shirt is an article that actually exists and can be sold.
 * Uses joined inheritance so that subclasses have their own table; the subclass id, which must not be declared in java but exists in the table, gets the same value as the YadaProduct id.
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaProduct implements CloneableFiltered, Serializable {
	private static final long serialVersionUID = 1L;

	// For synchronization with external databases
	@Column(insertable = false, updatable = false, columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified;

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	protected int year; // Production year

	@ElementCollection
	@Column(length=64)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> name = new HashMap<>(); // localized because it could be different for different languages

	@ElementCollection
	@Column(length=128)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> subtitle = new HashMap<>(); // a kind of short description

	@ElementCollection
	@Column(length=8192)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> description = new HashMap<>(); // a kind of small description

	@ElementCollection
	@Column(length=128)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> materials = new HashMap<>();

	// For Thymeleaf to convert between the enum string and the list of YadaPersistentEnum, you need to configure an "editor"
	// in your @Controller:
	//
	//	@InitBinder
	//	public void initBinder(WebDataBinder binder) {
	//		binder.registerCustomEditor(YadaPersistentEnum.class, new YadaPersistentEnumEditor(new Class[] {EnumCategory.class}));
	//	}
	@ManyToMany
	@JoinTable(
		name="YadaProduct_categories",
		uniqueConstraints = @UniqueConstraint(columnNames={"YadaProduct_id", "categories_id"})
	)
	protected List<YadaPersistentEnum<?>> categories = new ArrayList<>();

	@ManyToMany
	@JoinTable(
		name="YadaProduct_subcategories",
		uniqueConstraints = @UniqueConstraint(columnNames={"YadaProduct_id", "subcategories_id"})
	)
	protected List<YadaPersistentEnum<?>> subcategories = new ArrayList<>();

	/**
	 * true if the YadaProduct is an accessory
	 */
	protected boolean accessoryFlag; // Useful to know if this is an accessory without a join on the YadaProduct_accessories table

	@ManyToMany
	@JoinTable(name="YadaProduct_accessories")
	protected List<YadaProduct> accessories = new ArrayList<>();

	@ManyToMany(mappedBy="accessories")
	protected List<YadaProduct> accessoryOf = new ArrayList<>();

	@OneToMany(mappedBy="product")
	protected List<YadaArticle> articles = new ArrayList<>(); // The version of a product with a specific color, size, etc.

	protected boolean published;

	// A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance
	@OneToMany // (cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaProduct_galleryImages")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> galleryImages = new ArrayList<>();

	// A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance
	@OneToMany // (cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaProduct_attachments")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> attachments = new ArrayList<>();

	/**
	 * The main image to show in lists etc. (for example a thumbnail)
	 */
	@OneToOne(cascade=CascadeType.REMOVE, orphanRemoval=true)
	protected YadaAttachedFile image;

	/***********************************************************************/
//	/* Lazy localized value fetching                                       */
//
//	/* These getters can be called outside of a transaction (e.g. in the view)
//	 * to fetch the localized value for the current request locale
//	 */
//
//	protected @Transient YadaLocaleDao yadaLocaleDao;
//	protected @Transient String cacheName = null;
//	protected @Transient String cacheSubtitle = null;
//	protected @Transient String cacheDescription = null;
//	protected @Transient String cacheMaterials = null;
//
//	public YadaProduct() {
//		yadaLocaleDao = (YadaLocaleDao) YadaUtil.getBean(YadaLocaleDao.class);
//	}
//
//	/**
//	 * Fetches the localized name for the current request locale or the default configured locale
//	 * @return the name or the empty string if no value has been defined
//	 */
//	@Transient
//	public String getLocalName() {
//		if (cacheName==null) {
//			cacheName = yadaLocaleDao.getLocalValue(id, YadaProduct.class, "name", null);
//		}
//		return cacheName;
//	}
//
//	/**
//	 * Fetches the localized subtitle for the current request locale or the default configured locale
//	 * @return the name or the empty string if no value has been defined
//	 */
//	@Transient
//	public String getLocalSubtitle() {
//		if (cacheSubtitle==null) {
//			cacheSubtitle = yadaLocaleDao.getLocalValue(id, YadaProduct.class, "subtitle", null);
//		}
//		return cacheSubtitle;
//	}
//
//	/**
//	 * Fetches the localized description for the current request locale or the default configured locale
//	 * @return the name or the empty string if no value has been defined
//	 */
//	@Transient
//	public String getLocalDescription() {
//		if (cacheDescription==null) {
//			cacheDescription = yadaLocaleDao.getLocalValue(id, YadaProduct.class, "description", null);
//		}
//		return cacheDescription;
//	}
//
//	/**
//	 * Fetches the localized materials for the current request locale or the default configured locale
//	 * @return the name or the empty string if no value has been defined
//	 */
//	@Transient
//	public String getLocalMaterials() {
//		if (cacheMaterials==null) {
//			cacheMaterials = yadaLocaleDao.getLocalValue(id, YadaProduct.class, "materials", null);
//		}
//		return cacheMaterials;
//	}


	/**
	 * Returns the localized name in the current request locale
	 * @return
	 */
	@Transient
	public String getLocalName() {
		return YadaUtil.getLocalValue(name);
	}

	@Transient
	public void seLocalName(String name) {
		this.name.put(LocaleContextHolder.getLocale(), name);
	}
	/**
	 * Returns the localized subtitle in the current request locale
	 * @return
	 */
	@Transient
	public String getLocalSubtitle() {
		return YadaUtil.getLocalValue(subtitle);
	}

	@Transient
	public void seLocalSubtitle(String subtitle) {
		this.subtitle.put(LocaleContextHolder.getLocale(), subtitle);
	}

	/**
	 * Returns the localized description in the current request locale
	 * @return
	 */
	@Transient
	public String getLocalDescription() {
		return YadaUtil.getLocalValue(description);
	}

	@Transient
	public void seLocalDescription(String description) {
		this.description.put(LocaleContextHolder.getLocale(), description);
	}

	/**
	 * Returns the localized materials in the current request locale
	 * @return
	 */
	@Transient
	public String getLocalMaterials() {
		return YadaUtil.getLocalValue(materials);
	}

	@Transient
	public void seLocalMaterials (String materials) {
		this.materials.put(LocaleContextHolder.getLocale(), materials);
	}

	/***********************************************************************/
	/* Plain getter / setter                                               */

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public Map<Locale, String> getName() {
		return name;
	}

	public void setName(Map<Locale, String> name) {
		this.name = name;
	}

	public Map<Locale, String> getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(Map<Locale, String> subtitle) {
		this.subtitle = subtitle;
	}

	public Map<Locale, String> getDescription() {
		return description;
	}

	public void setDescription(Map<Locale, String> description) {
		this.description = description;
	}

	public Map<Locale, String> getMaterials() {
		return materials;
	}

	public void setMaterials(Map<Locale, String> materials) {
		this.materials = materials;
	}

	public List<YadaProduct> getAccessories() {
		return accessories;
	}

	public void setAccessories(List<YadaProduct> accessories) {
		this.accessories = accessories;
	}

	public List<YadaProduct> getAccessoryOf() {
		return accessoryOf;
	}

	public void setAccessoryOf(List<YadaProduct> accessoryOf) {
		this.accessoryOf = accessoryOf;
	}

	public List<YadaArticle> getArticles() {
		return articles;
	}

	public void setArticles(List<YadaArticle> articles) {
		this.articles = articles;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public long getVersion() {
		return version;
	}

	public List<YadaAttachedFile> getGalleryImages() {
		return galleryImages;
	}

	public void setGalleryImages(List<YadaAttachedFile> galleryImages) {
		this.galleryImages = galleryImages;
	}

	public List<YadaAttachedFile> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<YadaAttachedFile> attachments) {
		this.attachments = attachments;
	}

	public YadaAttachedFile getImage() {
		return image;
	}

	public void setImage(YadaAttachedFile image) {
		this.image = image;
	}

	@Override
	public Field[] getExcludedFields() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAccessoryFlag() {
		return accessoryFlag;
	}


	public void setAccessoryFlag(boolean accessoryFlag) {
		this.accessoryFlag = accessoryFlag;
	}

	public List<YadaPersistentEnum<?>> getCategories() {
		return categories;
	}

	public void setCategories(List<YadaPersistentEnum<?>> categories) {
		this.categories = categories;
	}

	public List<YadaPersistentEnum<?>> getSubcategories() {
		return subcategories;
	}

	public void setSubcategories(List<YadaPersistentEnum<?>> subcategories) {
		this.subcategories = subcategories;
	}


}
