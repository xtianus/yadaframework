package net.yadaframework.persistence.entity;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.CloneableDeep;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.raw.YadaIntDimension;

/**
 * A "pointer" to a file that has been uploaded to the "uploads" folder.
 * Needed when implementing a "file pool" for reusing files (aka "media manager").
 * Use case 1: upload an image, then use the same image on different products.
 * Use case 2: upload an image, ask the user to create many cropped versions, then delete the original image.
 */

// This class is not in the YadaCSS project because it has to be used by YadaUtil.copyEntity()

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaManagedFile implements CloneableDeep {

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

	/**
	 * Folder where the file is stored, relative to the uploads folder
	 */
	protected String relativeFolderPath; // Relative to the "uploads" folder

	/**
	 * The name of the file in the folder
	 */
	protected String filename;

	/**
	 * The original name that the file had when it was loaded, or the name that it will have when downloaded by a user.
	 */
	protected String clientFilename;

	@Column(length=512)
	protected String description; // Typed by the uploader

	@Temporal(TemporalType.TIMESTAMP)
	protected Date uploadTimestamp;

	/**
	 * Image width and height
	 */
	protected YadaIntDimension dimension;

	protected Long sizeInBytes;

	/**
	 * A temporary file will be deleted after some time.
	 */
	protected boolean temporary = true;

	@Transient
	private YadaConfiguration config; // Spring can not autowire an Entity

	public YadaManagedFile() {
		// Substitute for autowiring
		this.config = (YadaConfiguration) YadaUtil.getBean("config");
	}

	/**
	 * Returns the file extension without dot, or null
	 * @return
	 */
	public String getFileExtension() {
		return YadaUtil.INSTANCE.getFileExtension(filename);
	}

	/**
	 * Returns width and height of a loaded image
	 * @return
	 */
	public YadaIntDimension getDimension() {
		return dimension;
	}

	/**
	 * Returns the url to show this file on the web
	 * @return
	 */
	@Transient
	public String getUrl() {
		StringBuilder result = new StringBuilder(config.getUploadsUrl());
		result.append(StringUtils.isBlank(relativeFolderPath)?"":"/"+relativeFolderPath).append("/").append(filename);
		return result.toString();
	}

	/**
	 * Returns the absolute file on the filesystem
	 * @return
	 */
	@Transient
	public File getAbsoluteFile() {
		File result = config.getUploadsFolder();
		if (StringUtils.isNotBlank(relativeFolderPath)) {
			result = new File(result, relativeFolderPath);
		}
		result = new File(result, filename);
		return result;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getRelativeFolderPath() {
		return relativeFolderPath;
	}

	public void setRelativeFolderPath(String relativeFolderPath) {
		this.relativeFolderPath = relativeFolderPath;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFilename() {
		return filename;
	}

	public Long getSizeInBytes() {
		return sizeInBytes;
	}

	public void setSizeInBytes(Long bytes) {
		this.sizeInBytes = bytes;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}

	public void setDimension(YadaIntDimension dimension) {
		this.dimension = dimension;
	}

}
