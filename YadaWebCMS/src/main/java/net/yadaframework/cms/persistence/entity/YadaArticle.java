package net.yadaframework.cms.persistence.entity;

import java.io.Serializable;
import java.util.Date;
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
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaMoneyConverter;
import net.yadaframework.web.YadaJsonView;

/**
 * An Article is the actual physical item that can be produced and sold, so it will have a specific color, size, price, etc.
 *
 */
@Entity
public class YadaArticle implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// For synchronization with external databases
	@Column(columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified;
	
	// For optimistic locking
	@Version
	protected long version;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@JsonView(YadaJsonView.WithLocalizedStrings.class)
	@ElementCollection
	@Column(length=64)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> name; // localized because it could be different for different languages

	@Column(length=32)
	protected String code;
	
	@JsonView(YadaJsonView.WithLocalizedStrings.class)
	@ElementCollection
	@Column(length=32)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> color;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Embedded
	protected YadaDimension dimension;
	
	@Convert(converter = YadaMoneyConverter.class)
	protected YadaMoney unitPrice;
	
	@ManyToOne
	protected YadaProduct product;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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

}
