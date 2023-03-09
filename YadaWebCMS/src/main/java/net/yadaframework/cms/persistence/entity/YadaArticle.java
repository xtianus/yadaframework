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
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.springframework.context.i18n.LocaleContextHolder;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.CloneableFiltered;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaMoneyConverter;
import net.yadaframework.persistence.entity.YadaAttachedFile;

/**
 * An Article is the actual physical item that can be produced and sold, so it will have a specific color, size, price, etc.
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaArticle implements CloneableFiltered, Serializable {
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
	
	protected String internalName;

	@ElementCollection
	@Column(length=64)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> name = new HashMap<>(); // localized because it could be different for different languages

	@Column(length=32, unique=true)
	protected String sku; // Stock Keeping Unit aka internal company code

	@ElementCollection
	@Column(length=32)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> color = new HashMap<>();

	@Embedded
	protected YadaDimension dimension;

	@Convert(converter = YadaMoneyConverter.class)
	protected YadaMoney unitPrice;

	@ManyToOne
	protected YadaProduct product;

	protected boolean published;

	@OneToMany(cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaArticle_galleryImages")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> galleryImages;

	@OneToMany(cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaArticle_silhouetteImages")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> silhouetteImages;

	@OneToMany(cascade=CascadeType.REMOVE, orphanRemoval=true)
	@JoinTable(name="YadaArticle_attachments")
	@OrderBy("sortOrder")
	protected List<YadaAttachedFile> attachments;

	/**
	 * The main image to show in lists etc.
	 */
	@OneToOne(cascade=CascadeType.REMOVE, orphanRemoval=true)
	protected YadaAttachedFile image;

	@Transient
	protected Long chosenProductId;

	////////////////////////////////////////////////////////////////////77

	/**
	 * Returns the localized name in the current request locale
	 * @return
	 */
	public String getLocalName() {
		return YadaUtil.getLocalValue(name);
	}

	public void setLocalName(String name) {
		this.name.put(LocaleContextHolder.getLocale(), name);
	}

	public void setLocalName(Locale locale, String name) {
		this.name.put(locale, name);
	}
	
	/**
	 * Returns the localized color in the current request locale
	 * @return
	 */
	public String getLocalColor() {
		return YadaUtil.getLocalValue(color);
	}

	public void setLocalcolor (String color) {
		this.color.put(LocaleContextHolder.getLocale(), color);
	}

	/**
	 *
	 * @param locale
	 * @return
	 */
	public String getColor(Locale locale) {
		return YadaUtil.getLocalValue(color, locale);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String code) {
		this.sku = code;
	}

	public Map<Locale, String> getColor() {
		return color;
	}

	public void setColor(Map<Locale, String> description) {
		this.color = description;
	}

	public YadaDimension getDimension() {
		return dimension;
	}

	public void setDimension(YadaDimension dimension) {
		this.dimension = dimension;
	}

	public YadaMoney getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(YadaMoney unitPrice) {
		this.unitPrice = unitPrice;
	}

	public YadaProduct getProduct() {
		return product;
	}

	public void setProduct(YadaProduct product) {
		this.product = product;
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

	public List<YadaAttachedFile> getSilhouetteImages() {
		return silhouetteImages;
	}

	public void setSilhouetteImages(List<YadaAttachedFile> silhouetteImages) {
		this.silhouetteImages = silhouetteImages;
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

	public Map<Locale, String> getName() {
		return name;
	}

	public void setName(Map<Locale, String> name) {
		this.name = name;
	}

	@Override
	public Field[] getExcludedFields() {
		// TODO Auto-generated method stub
		return null;
	}

	public Long getChosenProductId() {
		return chosenProductId;
	}

	public void setChosenProductId(Long chosenProductId) {
		this.chosenProductId = chosenProductId;
	}

	public String getInternalName() {
		return internalName;
	}

	public void setInternalName(String internalName) {
		this.internalName = internalName;
	}

}
