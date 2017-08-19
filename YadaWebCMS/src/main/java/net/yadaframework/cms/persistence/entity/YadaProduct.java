package net.yadaframework.cms.persistence.entity;

import java.io.Serializable;
import java.util.Date;
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

import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.persistence.repository.YadaLocaleDao;
import net.yadaframework.web.YadaJsonView;

/**
 * A Product is an "abstract" item because it groups similar objects that differ in color and size.
 * So a "Paris T-Shirt" is a very specific product but it comes in different sizes, so it doesn't exist unless
 * you specify the size. 
 * The Medium Paris T-Shirt is an article that actually exists and can be sold.
 *
 */
@Entity
public class YadaProduct implements Serializable {
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

	@JsonView(YadaJsonView.WithLocalizedStrings.class)
	@ElementCollection
	@Column(length=64)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> name; // localized because it could be different for different languages

	@JsonView(YadaJsonView.WithLocalizedStrings.class)
	@ElementCollection
	@Column(length=128)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> subtitle; // a kind of short description
	
	@JsonView(YadaJsonView.WithLocalizedStrings.class)
	@ElementCollection
	@Column(length=8192)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> description; // a kind of small description
	
	@JsonView(YadaJsonView.WithLocalizedStrings.class)
	@ElementCollection
	@Column(length=128)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> materials;
	
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
	/* Useful methods                                                      */

	// TODO remove
	
	protected @Transient YadaLocaleDao yadaLocaleDao;

	public YadaProduct() {
		yadaLocaleDao = (YadaLocaleDao) YadaUtil.getBean(YadaLocaleDao.class);
	}
	
	@Transient
	public String getLocalName() {
		return yadaLocaleDao.getLocalValue(id, YadaProduct.class, "name", null);
	}

	@Transient
	public String getLocalSubtitle() {
		return yadaLocaleDao.getLocalValue(id, YadaProduct.class, "subtitle", null);
	}

	@Transient
	public String getLocalDescription() {
		return yadaLocaleDao.getLocalValue(id, YadaProduct.class, "description", null);
	}
	
	@Transient
	public String getLocalMaterials() {
		return yadaLocaleDao.getLocalValue(id, YadaProduct.class, "materials", null);
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

	public boolean isAccessory() {
		return isAccessory;
	}

	public void setAccessory(boolean isAccessory) {
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
	
	
}
