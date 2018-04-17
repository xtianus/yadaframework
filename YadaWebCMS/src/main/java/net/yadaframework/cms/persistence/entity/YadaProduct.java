package net.yadaframework.cms.persistence.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.core.CloneableFiltered;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.web.YadaJsonView;

/**
 * A Product is an "abstract" item because it groups similar objects that differ in color and size.
 * So a "Paris T-Shirt" is a very specific product but it comes in different sizes, so it doesn't exist unless
 * you specify the size. 
 * The Medium Paris T-Shirt is an article that actually exists and can be sold.
 *
 */
@Entity
public class YadaProduct implements CloneableFiltered, Serializable {
	private static final long serialVersionUID = 1L;
	
	// For synchronization with external databases
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Column(columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified;
	
	// For optimistic locking
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Version
	protected long version;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected int year; // Production year

	//@JsonView(YadaJsonView.WithLocalizedStrings.class)
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
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected int category;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected int subCategory;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected boolean isAccessory; // Useful to know if this is an accessory without a join on the YadaProduct_accessories table
	
	@JsonView(YadaJsonView.WithLazyAttributes.class)
	@ManyToMany
	@JoinTable(name="YadaProduct_accessories")
	protected List<YadaProduct> accessories;
	
	@JsonView(YadaJsonView.WithLazyAttributes.class)
	@ManyToMany(mappedBy="accessories")
	protected List<YadaProduct> accessoryOf;
	
	@JsonView(YadaJsonView.WithLazyAttributes.class)
	@OneToMany(mappedBy="product")
	protected List<YadaArticle> articles; // The version of a product with a specific color, size, etc.

	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected boolean published;
	
	@JsonView(YadaJsonView.WithLazyAttributes.class)
	@OneToMany(cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaProduct_galleryImages")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> galleryImages;

	@JsonView(YadaJsonView.WithLazyAttributes.class)
	@OneToMany(cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaProduct_attachments")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> attachments;
	
	/**
	 * The main image to show in lists etc.
	 */
	@JsonView(YadaJsonView.WithEagerAttributes.class)
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
	@JsonView(YadaJsonView.WithLocalizedValue.class)
	public String getLocalName() {
		return name.get(LocaleContextHolder.getLocale());
	}
	
	
	public void seLocalName(String name) {
		this.name.put(LocaleContextHolder.getLocale(), name);
	}
	/**
	 * Returns the localized subtitle in the current request locale
	 * @return
	 */
	@JsonView(YadaJsonView.WithLocalizedValue.class)
	public String getLocalSubtitle() {
		return subtitle.get(LocaleContextHolder.getLocale());
	}
	
	public void seLocalSubtitle(String subtitle) {
		this.subtitle.put(LocaleContextHolder.getLocale(), subtitle);
	}
	
	/**
	 * Returns the localized description in the current request locale
	 * @return
	 */
	@JsonView(YadaJsonView.WithLocalizedValue.class)
	public String getLocalDescription() {
		return description.get(LocaleContextHolder.getLocale());
	}
	
	public void seLocalDescription(String description) {
		this.description.put(LocaleContextHolder.getLocale(), description);
	}
	
	/**
	 * Returns the localized materials in the current request locale
	 * @return
	 */
	@JsonView(YadaJsonView.WithLocalizedValue.class)
	public String getLocalMaterials() {
		return materials.get(LocaleContextHolder.getLocale());
	}
	
	public void seLocalMaterials (String materials) {
		this.materials.put(LocaleContextHolder.getLocale(), materials);
	}
	
	/***********************************************************************/
	/* Id for DataTables                                                   */
	
	@Transient
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@JsonProperty("DT_RowId")
	public String getDT_RowId() {
		return this.getClass().getSimpleName()+"#"+this.id; // YadaProduct#142
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

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public int getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(int subCategory) {
		this.subCategory = subCategory;
	}

	public boolean getIsAccessory() {
		return isAccessory;
	}

	public void setIsAccessory(boolean isAccessory) {
		this.isAccessory = isAccessory;
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

}
