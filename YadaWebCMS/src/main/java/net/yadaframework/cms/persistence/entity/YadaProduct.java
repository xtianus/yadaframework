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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

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
	@Column(columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
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
	protected Map<Locale, String> name; // localized because it could be different for different languages

	@ElementCollection
	@Column(length=128)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> subtitle; // a kind of short description
	
	@ElementCollection
	@Column(length=8192)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> description; // a kind of small description
	
	@ElementCollection
	@Column(length=128)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> materials;
	
	protected int category;
	
	protected int subCategory;
	
	protected boolean isAccessory; // Useful to know if this is an accessory without a join on the YadaProduct_accessories table
	
	@ManyToMany
	@JoinTable(name="YadaProduct_accessories")
	protected List<YadaProduct> accessories;
	
	@ManyToMany(mappedBy="accessories")
	protected List<YadaProduct> accessoryOf;
	
	@OneToMany(mappedBy="product")
	protected List<YadaArticle> articles; // The version of a product with a specific color, size, etc.

	protected boolean published;
	
	@OneToMany(cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaProduct_galleryImages")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> galleryImages;

	@OneToMany(cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaProduct_attachments")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> attachments;
	
	/**
	 * The main image to show in lists etc.
	 */
	@OneToOne(cascade=CascadeType.REMOVE, orphanRemoval=true)
	protected YadaAttachedFile image;


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
