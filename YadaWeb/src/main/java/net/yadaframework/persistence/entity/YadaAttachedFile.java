package net.yadaframework.persistence.entity;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MapKeyColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

/**
 * A "pointer" to a file that has been uploaded into the "contents" folder.
 * When a file is associated to an object, an instance of this class is created.
 * Files can still exist after the object has been deleted, and can be re-attached to many objects using different titles, sort orders etc.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaAttachedFile {
	
	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	/**
	 * Value for ordering files of the same type (e.g. gallery images)
	 */
	// On object creation it can be set the same value as the id, so that no other element will have the same position.
	// During reordering, values just have to be swapped.
	protected long sortOrder;

	/**
	 * Folder where the file is stored, relative to the contents folder
	 */
	protected String relativePath; 
	
	@ElementCollection
	@Column(length=64)
	@MapKeyColumn(name="locale", length=32)
	protected Map<Locale, String> title;

	@ElementCollection
	@Column(length=512)
	@MapKeyColumn(name="locale", length=32)
	protected Map<Locale, String> description;
	
	@Temporal(TemporalType.TIMESTAMP)
	protected Date uploadTimestamp;
	
	protected boolean published;
	
	/**
	 * When set, the file is available only for the locale specified
	 */
	protected Locale forLocale;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public Map<Locale, String> getTitle() {
		return title;
	}

	public void setTitle(Map<Locale, String> title) {
		this.title = title;
	}

	public Map<Locale, String> getDescription() {
		return description;
	}

	public void setDescription(Map<Locale, String> description) {
		this.description = description;
	}

	public Date getUploadTimestamp() {
		return uploadTimestamp;
	}

	public void setUploadTimestamp(Date uploadTimestamp) {
		this.uploadTimestamp = uploadTimestamp;
	}

	public long getVersion() {
		return version;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public Locale getForLocale() {
		return forLocale;
	}

	public void setForLocale(Locale forLocale) {
		this.forLocale = forLocale;
	}

	public long getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(long sortOrder) {
		this.sortOrder = sortOrder;
	}

}
