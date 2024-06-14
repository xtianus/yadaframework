package net.yadaframework.persistence.entity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.CloneableDeep;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.exceptions.YadaInvalidValueException;
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
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());

	public enum YadaAttachedFileType {
		DESKTOP,
		MOBILE,
		PDF,
		DEFAULT;
	}

	protected final static String COUNTER_SEPARATOR="_";

	// For synchronization with external databases
	@Column(insertable = false, updatable = false, columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified = new Date();

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	protected Long id;

// Removed because the id is not unique in the database: we would need the class name too. But anyway, this id is not needed.
//	protected Long attachedToId; // Id of the Entity to which this file is attached

	/**
	 * Value for ordering files of the same type (e.g. gallery images)
	 */
	// On object creation it can be set the same value as the id, so that no other element will have the same position.
	// During reordering, values just have to be swapped.
	// Use @OrderBy("sortOrder") in the owning entity
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
	 * The pdf version of an image is here
	 */
	protected String filenamePdf; // only for images in pdf, null for non-images

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
	@AttributeOverrides({
		@AttributeOverride(name="width", column=@Column(name="widthPdf")),
		@AttributeOverride(name="height", column=@Column(name="heightPdf"))
	})
	protected YadaIntDimension pdfImageDimension = YadaIntDimension.UNSET;

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

	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date uploadTimestamp;

	protected boolean published = false; // Application-defined flag

	/**
	 * When set, the file is available only for the locale specified
	 */
	protected Locale forLocale;

	@Column(length=1024)
	protected String metadata; // Can be anything related to the file

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
	 * Set the same title for all configured locales
	 * @param uniqueTitle a single title for all locales
	 */
	public void setAllTitles(String uniqueTitle) {
		this.title = new HashMap<>();
        config.getLocales().forEach(a -> {title.put(a, uniqueTitle);});
	}

	/**
	 * Ensures that the current entity is sorted before the parameter
	 * @param toComeAfter the entity that must come after in an ascending sort order
	 */
	public void orderBefore(YadaAttachedFile toComeAfter) {
		if (this.sortOrder > toComeAfter.sortOrder) {
			long currentSortOrder = this.sortOrder;
			this.sortOrder = toComeAfter.sortOrder;
			toComeAfter.sortOrder = currentSortOrder;
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
	@Deprecated // Do not use config
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
	@Deprecated // Do not use config
	public File calcAndSetTargetFile(String namePrefix, String targetExtension, Integer targetWidth, YadaAttachedFileType type, YadaConfiguration config) {
		return calcAndSetTargetFile(namePrefix, targetExtension, targetWidth, type);
	}

	/**
	 * Computes the file to create, given the parameters, and sets it.
	 * @param namePrefix string to attach at the start of the filename, can be null
	 * @param targetExtension the needed file extension without dot, can be null if no conversion has to be performed
	 * @param targetWidth the needed image width, null if no resize has to be performed
	 * @param type the type of file
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
		} else {
			result = new File(targetFolder, targetFilenamePrefix + COUNTER_SEPARATOR + targetWidth + "." + targetExtension);
		}
		switch (type) {
		case DESKTOP:
			this.filenameDesktop = result.getName();
			break;
		case MOBILE:
			this.filenameMobile = result.getName();
			break;
		case PDF:
			this.filenamePdf = result.getName();
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
	@Deprecated // Do not use config
	public File getAbsoluteFile(YadaAttachedFileType type, YadaConfiguration config) {
		return getAbsoluteFile(type);
	}
	
	/**
	 * Moves all files to a different relative path
	 * @param newRelativeFolderPath
	 * @throws IOException
	 */
	public void move(String newRelativeFolderPath) throws IOException {
		// Ensure the target folder exists
		File targetFolder = config.getContentsFolder();
		if (StringUtils.isNotBlank(newRelativeFolderPath)) {
			targetFolder = new File(targetFolder, newRelativeFolderPath);
		}
		targetFolder.mkdirs();
		//
		File sourceFileMobile = getAbsoluteFile(YadaAttachedFileType.MOBILE);
		File sourceFileDesktop = getAbsoluteFile(YadaAttachedFileType.DESKTOP);
		File sourceFilePdf = getAbsoluteFile(YadaAttachedFileType.PDF);
		File sourceFile = getAbsoluteFile(YadaAttachedFileType.DEFAULT);
		this.setRelativeFolderPath(newRelativeFolderPath);
		if (sourceFileMobile!=null && sourceFileMobile.canRead()) {
			Files.move(sourceFileMobile, getAbsoluteFile(YadaAttachedFileType.MOBILE));
		}
		if (sourceFileDesktop!=null && sourceFileDesktop.canRead()) {
			Files.move(sourceFileDesktop, getAbsoluteFile(YadaAttachedFileType.DESKTOP));
		}
		if (sourceFilePdf!=null && sourceFilePdf.canRead()) {
			Files.move(sourceFilePdf, getAbsoluteFile(YadaAttachedFileType.PDF));
		}
		if (sourceFile!=null && sourceFile.canRead()) {
			Files.move(sourceFile, getAbsoluteFile(YadaAttachedFileType.DEFAULT));
		}
	}
	
	/**
	 * Rename a file in the same folder. Do not use to move to a different folder.
	 * @param newName the new exact name, with no path. Will overwrite an existing file with the same name.
	 * @param type
	 * @return true if the file was renamed successfully or if the new name is the same as the old one
	 */
	public boolean rename(String newName, YadaAttachedFileType type) {
		newName = StringUtils.trimToNull(newName);
		if (newName==null) {
			throw new YadaInvalidValueException("Invalid filename for rename: " + newName);
		}
		File source = getAbsoluteFile(type);
		File target = new File(source.getParentFile(), newName);
		if (source.equals(target)) {
			return true;
		}
		try {
			Files.move(source, target);
			switch (type) {
			case DESKTOP:
				filenameDesktop=newName;
				break;
			case MOBILE:
				filenameMobile=newName;
				break;
			case PDF:
				filenamePdf=newName;
				break;
			case DEFAULT:
				filename=newName;
				break;
			default:
				throw new YadaInvalidUsageException("Invalid type: " + type);
			}
			return true;
		} catch (IOException e) {
			log.error("Can't rename file from {} to {}: {}", source, target, e.getMessage());
			return false;
		}
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
		case PDF:
			if (StringUtils.isBlank(filenamePdf)) {
				return null;
			}
			return new File(result, filenamePdf);
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

	/**
	 * Returns the localized title in the current request locale
	 * @return
	 */
	public String getLocalTitle() {
		return YadaUtil.getLocalValue(title);
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

	/**
	 * Returns the localized title in the current request locale
	 * @return
	 */
	public String getLocalDescription() {
		return YadaUtil.getLocalValue(description);
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

	public boolean isPdfImage() {
		return this.filenamePdf!=null && !pdfImageDimension.isUnset();
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

	public String getFilenamePdf() {
		return filenamePdf;
	}

	public void setFilenamePdf(String filenamePdf) {
		this.filenamePdf = filenamePdf;
	}

	public YadaIntDimension getPdfImageDimension() {
		return pdfImageDimension;
	}

	public void setPdfImageDimension(YadaIntDimension pdfImageDimension) {
		this.pdfImageDimension = pdfImageDimension;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	@Override
	public int hashCode() {
		if (id!=null) {
			// Two entities with the same id are the same.
			// This prevents lazy init exceptions too.
			return id.hashCode();
		}
		// This is for when the object is not persisted yet
		return Objects.hash(clientFilename, description, desktopImageDimension, filename, filenameDesktop,
				filenameMobile, filenamePdf, forLocale, id, imageDimension, metadata, mobileImageDimension, modified,
				pdfImageDimension, published, relativeFolderPath, sortOrder, title, uploadTimestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		YadaAttachedFile other = (YadaAttachedFile) obj;
		if (id!=null) {
			// Two entities with the same id are the same.
			// This prevents lazy init exceptions too.
			return id.equals(other.id);
		}
		// This is for when the object is not persisted yet
		return Objects.equals(clientFilename, other.clientFilename) && Objects.equals(description, other.description)
				&& Objects.equals(desktopImageDimension, other.desktopImageDimension)
				&& Objects.equals(filename, other.filename) && Objects.equals(filenameDesktop, other.filenameDesktop)
				&& Objects.equals(filenameMobile, other.filenameMobile)
				&& Objects.equals(filenamePdf, other.filenamePdf) && Objects.equals(forLocale, other.forLocale)
				&& Objects.equals(id, other.id) && Objects.equals(imageDimension, other.imageDimension)
				&& Objects.equals(metadata, other.metadata)
				&& Objects.equals(mobileImageDimension, other.mobileImageDimension)
				&& Objects.equals(modified, other.modified)
				&& Objects.equals(pdfImageDimension, other.pdfImageDimension) && published == other.published
				&& Objects.equals(relativeFolderPath, other.relativeFolderPath) && sortOrder == other.sortOrder
				&& Objects.equals(title, other.title) && Objects.equals(uploadTimestamp, other.uploadTimestamp);
	}

	
}
