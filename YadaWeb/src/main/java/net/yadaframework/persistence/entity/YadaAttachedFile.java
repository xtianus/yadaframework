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
 * A "pointer" to a file that has been copied into the "contents" folder.
 * When an uploaded file is associated to an object, an instance of this class is created and a copy of the file is made
 * from the "uploads" folder to the "contents" folder.
 * The file is also copied in different sizes for desktop and mobile.
 * The original files can still exist after the object has been deleted, and can be re-attached to many objects using different titles, sort orders etc.
 * NOTE: this class is not part of YadaWebCMS because it's used by YadaWebSecurity
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaAttachedFile {
	
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
	
	protected Long attachedToId; // If of the Entity to which this file is attached
	
	/**
	 * Value for ordering files of the same type (e.g. gallery images)
	 */
	// On object creation it can be set the same value as the id, so that no other element will have the same position.
	// During reordering, values just have to be swapped.
	protected long sortOrder;

	/**
	 * Folder where the file is stored, relative to the contents folder
	 */
	protected String relativeFolderPath; // Relative to the "contents" folder
	
	/**
	 * When the CMS creates a mobile version of an image, the name is found here.
	 * NOTE: to have different images for portrait/landscape, you need to upload different files hence have different instances of this class
	 */
	protected String filenameMobile; // only for images on mobile, null for no specific image 
	
	/**
	 * The desktop version of an image is here
	 */
	protected String filenameDesktop; // only for images on desktop, null for non-images
	
	/**
	 * Only for non-images, or when no alternative size is specified (will be the same as filenameDesktop)
	 */
	protected String filename;
	
	/**
	 * The original name that the file had when it was loaded, or the name that it will have when downloaded by a user.
	 */
	protected String clientFilename;
	
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

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
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

	public String getRelativeFolderPath() {
		return relativeFolderPath;
	}

	public void setRelativeFolderPath(String relativeFolderPath) {
		this.relativeFolderPath = relativeFolderPath;
	}

	public String getFilenameMobile() {
		return filenameMobile;
	}

	public void setFilenameMobile(String filenameMobile) {
		this.filenameMobile = filenameMobile;
	}

	public String getFilenameDesktop() {
		return filenameDesktop;
	}

	public void setFilenameDesktop(String filenameDesktop) {
		this.filenameDesktop = filenameDesktop;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Long getAttachedToId() {
		return attachedToId;
	}

	public void setAttachedToId(Long attachedToId) {
		this.attachedToId = attachedToId;
	}

	public String getClientFilename() {
		return clientFilename;
	}

	public void setClientFilename(String clientFilename) {
		this.clientFilename = clientFilename;
	}

}
