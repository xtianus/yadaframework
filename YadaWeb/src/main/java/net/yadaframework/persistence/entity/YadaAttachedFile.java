package net.yadaframework.persistence.entity;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MapKeyColumn;
import javax.persistence.PostPersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.CloneableDeep;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.raw.YadaIntDimension;

/**
 * A "pointer" to a file that has been copied into the "contents" folder.
 * When an uploaded file is associated to an object, an instance of this class is created and a copy of the file is made
 * from the "uploads" folder to the "contents" folder.
 * The file is also copied in different sizes for desktop and mobile.
 * The original files can still exist after the object has been deleted, and can be re-attached to many objects using different titles, sort orders etc.
 * NOTE: this class is not part of YadaWebCMS because it's used by YadaWebSecurity and by YadaUtil.copyEntity()
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaAttachedFile implements CloneableDeep {

	public enum YadaAttachedFileType {
		DESKTOP,
		MOBILE,
		DEFAULT;
	}

	protected final static String COUNTER_SEPARATOR="_";

	// For synchronization with external databases
	@Column(columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified = new Date();

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

// Removed because the id is not unique in the database: we would need the class name too. But anyway, this id is not needed.
//	protected Long attachedToId; // Id of the Entity to which this file is attached

	/**
	 * Value for ordering files of the same type (e.g. gallery images)
	 */
	// On object creation it can be set the same value as the id, so that no other element will have the same position.
	// During reordering, values just have to be swapped.
	protected long sortOrder=-1;

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
	 * Image width and height (for images)
	 */
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="width", column=@Column(name="widthDesktop")),
		@AttributeOverride(name="height", column=@Column(name="heightDesktop"))
	})
	protected YadaIntDimension desktopImageDimension = YadaIntDimension.UNSET;
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="width", column=@Column(name="widthMobile")),
		@AttributeOverride(name="height", column=@Column(name="heightMobile"))
	})
	protected YadaIntDimension mobileImageDimension = YadaIntDimension.UNSET;
	@Embedded
	protected YadaIntDimension imageDimension = YadaIntDimension.UNSET;

	/**
	 * Only for non-images, or when no alternative size is specified (will be the same as filenameDesktop)
	 */
	protected String filename;

	/**
	 * The original name that the file had when it was loaded, or the name that it will have when downloaded by a user.
	 */
	protected String clientFilename;

	@ElementCollection
	@Column(length=1024)
	@MapKeyColumn(name="locale", length=32)
	protected Map<Locale, String> title;

	@ElementCollection
	@Column(length=8192)
	@MapKeyColumn(name="locale", length=32)
	protected Map<Locale, String> description;

	@Temporal(TemporalType.TIMESTAMP)
	protected Date uploadTimestamp;

	protected boolean published = false; // Application-defined flag

	/**
	 * When set, the file is available only for the locale specified
	 */
	protected Locale forLocale;

	@Transient
	private YadaConfiguration config; // Spring can not autowire an Entity

	public YadaAttachedFile() {
		// Substitute for autowiring
		this.config = (YadaConfiguration) YadaUtil.getBean("config");
	}

	@PostPersist
	// Sets the sortOrder equal to the id, so that ordering can occur by just swapping sortOrder values
	public void ensureSortOrder() {
		if (sortOrder==-1 && id!=null) {
			sortOrder = id;
		}
	}

	/**
	 * Computes the file to create, given the parameters, and sets it.
	 * @param namePrefix string to attach at the start of the filename, can be null
	 * @param targetExtension the needed file extension without dot, can be null if no conversion has to be performed
	 * @param targetDimension the needed image size (only width is considered), null if no resize has to be performed
	 * @param type the type of file
	 * @param targetFolder where the file has to be stored
	 * @return
	 */
	@Transient
	@Deprecated
	public File calcAndSetTargetFile(String namePrefix, String targetExtension, YadaAttachedFileType type, YadaIntDimension targetDimension, YadaConfiguration config) {
		return calcAndSetTargetFile(namePrefix, targetExtension, type, targetDimension);
	}

	/**
	 * Computes the file to create, given the parameters, and sets it.
	 * @param namePrefix string to attach at the start of the filename, can be null
	 * @param targetExtension the needed file extension without dot, can be null if no conversion has to be performed
	 * @param targetDimension the needed image size (only width is considered), null if no resize has to be performed
	 * @param type the type of file
	 * @param targetFolder where the file has to be stored
	 * @return
	 */
	@Transient
	public File calcAndSetTargetFile(String namePrefix, String targetExtension, YadaAttachedFileType type, YadaIntDimension targetDimension) {
		Integer targetWidth = targetDimension == null ? null : targetDimension.getWidth();
		return calcAndSetTargetFile(namePrefix, targetExtension, targetWidth, type);
	}

	/**
	 * Computes the file to create, given the parameters, and sets it.
	 * @param namePrefix string to attach at the start of the filename, can be null
	 * @param targetExtension the needed file extension without dot, can be null if no conversion has to be performed
	 * @param targetWidth the needed image width, null if no resize has to be performed
	 * @param type the type of file
	 * @param targetFolder where the file has to be stored
	 * @return
	 */
	@Transient
	@Deprecated
	public File calcAndSetTargetFile(String namePrefix, String targetExtension, Integer targetWidth, YadaAttachedFileType type, YadaConfiguration config) {
		return calcAndSetTargetFile(namePrefix, targetExtension, targetWidth, type);
	}

	/**
	 * Computes the file to create, given the parameters, and sets it.
	 * @param namePrefix string to attach at the start of the filename, can be null
	 * @param targetExtension the needed file extension without dot, can be null if no conversion has to be performed
	 * @param targetWidth the needed image width, null if no resize has to be performed
	 * @param type the type of file
	 * @param targetFolder where the file has to be stored
	 * @return
	 */
	@Transient
	public File calcAndSetTargetFile(String namePrefix, String targetExtension, Integer targetWidth, YadaAttachedFileType type) {
		File result = null;
		if (this.id==null) {
			throw new YadaInvalidUsageException("YadaAttachedFile instance must be saved before");
		}
		if (StringUtils.isBlank(this.clientFilename)) {
			throw new YadaInvalidUsageException("YadaAttachedFile instance must have the client filename set");
		}
		File targetFolder = new File(config.getContentPath(), StringUtils.trimToEmpty(this.relativeFolderPath));
		String[] filenameParts = YadaUtil.splitFileNameAndExtension(this.clientFilename);
		String origFilename = filenameParts[0];
		String origExtension = filenameParts[1]; // e.g. jpg or pdf
		String anticache = String.format("%x", System.currentTimeMillis()); // To prevent cache issues when changing image file
		String targetFilenamePrefix = StringUtils.trimToEmpty(namePrefix) + origFilename + COUNTER_SEPARATOR + this.id + COUNTER_SEPARATOR + anticache; // product_2631_38f74g
		targetFilenamePrefix = YadaUtil.reduceToSafeFilename(targetFilenamePrefix, true);
		if (targetExtension==null) {
			targetExtension = origExtension;
		}
		boolean imageExtensionChanged = targetExtension.compareToIgnoreCase(origExtension)!=0;
		boolean requiresTransofmation = imageExtensionChanged || targetWidth!=null;
		if (!requiresTransofmation) {
			result = new File(targetFolder, targetFilenamePrefix + "." + targetExtension);
		}
		result = new File(targetFolder, targetFilenamePrefix + COUNTER_SEPARATOR + targetWidth + "." + targetExtension);
		switch (type) {
		case DESKTOP:
			this.filenameDesktop = result.getName();
			break;
		case MOBILE:
			this.filenameMobile = result.getName();
			break;
		case DEFAULT:
			this.filename = result.getName();
			break;
		default:
			throw new YadaInvalidUsageException("Invalid type: " + type);
		}
		return result;
	}

	/**
	 * Returns the absolute file on the filesystem
	 * @param type the version of the file: desktop, mobile or default
	 * @param config
	 * @return
	 */
	@Transient
	@Deprecated
	public File getAbsoluteFile(YadaAttachedFileType type, YadaConfiguration config) {
		return getAbsoluteFile(type);
	}

	/**
	 * Returns the absolute file on the filesystem
	 * @param type the version of the file: desktop, mobile or default
	 * @param config
	 * @return
	 */
	@Transient
	public File getAbsoluteFile(YadaAttachedFileType type) {
		File result = config.getContentsFolder();
		// Add the relative path if any
		if (StringUtils.isNotBlank(relativeFolderPath)) {
			result = new File(result, relativeFolderPath);
		}
		switch (type) {
		case DESKTOP:
			if (StringUtils.isBlank(filenameDesktop)) {
				return null;
			}
			return new File(result, filenameDesktop);
		case MOBILE:
			if (StringUtils.isBlank(filenameMobile)) {
				return null;
			}
			return new File(result, filenameMobile);
		case DEFAULT:
			if (StringUtils.isBlank(filename)) {
				return null;
			}
			return new File(result, filename);
		}
		throw new YadaInvalidUsageException("Invalid type: " + type);
	}

//	/**
//	 * Create an instance and attach to an Entity
//	 * @param attachedToId the id of the owning entity
//	 */
//	public YadaAttachedFile(Long attachedToId) {
//		this.attachedToId = attachedToId;
//	}

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

//	public Long getAttachedToId() {
//		return attachedToId;
//	}
//
//	public void setAttachedToId(Long attachedToId) {
//		this.attachedToId = attachedToId;
//	}

	public String getClientFilename() {
		return clientFilename;
	}

	public void setClientFilename(String clientFilename) {
		this.clientFilename = clientFilename;
	}

	@Override
	public Field[] getExcludedFields() {
		try {
			return new Field[] {
				this.getClass().getField("modified"),
				this.getClass().getField("version")
			};
		} catch (Exception e) {
			return null;
		}
	}

	public YadaIntDimension getImageDimension() {
		return imageDimension;
	}

	public void setImageDimension(YadaIntDimension dimension) {
		this.imageDimension = dimension;
	}

	/**
	 * Returns true if the file is an image
	 */
	public boolean isImage() {
		return !imageDimension.isUnset() || !desktopImageDimension.isUnset() || !mobileImageDimension.isUnset();
	}

	public boolean isDesktopImage() {
		return this.filenameDesktop!=null && !desktopImageDimension.isUnset();
	}

	public boolean isMobileImage() {
		return this.filenameMobile!=null && !mobileImageDimension.isUnset();
	}

	public YadaIntDimension getDesktopImageDimension() {
		return desktopImageDimension;
	}

	public void setDesktopImageDimension(YadaIntDimension desktopImageDimension) {
		this.desktopImageDimension = desktopImageDimension;
	}

	public YadaIntDimension getMobileImageDimension() {
		return mobileImageDimension;
	}

	public void setMobileImageDimension(YadaIntDimension mobileImageDimension) {
		this.mobileImageDimension = mobileImageDimension;
	}

}
