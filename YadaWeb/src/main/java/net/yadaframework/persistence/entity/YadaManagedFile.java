package net.yadaframework.persistence.entity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.CloneableDeep;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.raw.YadaIntDimension;

/**
 * A "pointer" to a file that has been uploaded to the "uploads" folder.
 * Needed when implementing a "file pool" for reusing files (aka "media manager").
 * Use case 1: upload an image, then use the same image on different products.
 * Use case 2: upload an image, ask the user to create many cropped versions, then delete the original image.
 */

// This class is not in the YadaWebCMS project because it has to be used by YadaUtil.copyEntity()

@Entity
// @Inheritance(strategy = InheritanceType.JOINED)
public class YadaManagedFile implements CloneableDeep {
	
	@Transient
	@Autowired
	private YadaWebUtil yadaWebUtil;
	@Transient @Autowired
	private YadaUtil yadaUtil;

	// For synchronization with external databases
	@Column(insertable = false, updatable = false, columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	protected Date modified = new Date();

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	protected Long id;

	/**
	 * Folder where the file is stored, relative to the basePath folder
	 */
	protected String relativeFolderPath; // null if the file is in the basePath folder, but this should never happen

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

	protected Date uploadTimestamp;
	
	/**
	 * Image width and height
	 */
	protected YadaIntDimension dimension;

	protected Long sizeInBytes;
	
	/**
	 * A temporary file can be deleted when not needed anymore (application specific).
	 */
	protected boolean temporary = true;

	/**
	 * When false, the file is accessible from the public contents folder directly by apache
	 */
	protected boolean privateAccess = false;

	/**
	 * Linked to cropped images
	 */
	@OneToMany(cascade= CascadeType.REMOVE)
	@MapKeyColumn(name="assetKey") // key
	protected Map<String, YadaManagedFile> derivedAssets;
	
	/**
	 * When not null, this file can be deleted after the specified timestamp (application specific).
	 */
	protected Date expirationTimestamp;

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
	 * Moves this file to the target file, overwriting the target if it exists. The filename may change as a result.
	 * @param targetFile
	 * @return the targetFile
	 * @throws IOException 
	 */
	@Transient
	public YadaManagedFile move(File targetFile) throws IOException {
		Path targetPath = targetFile.toPath();
		File currentFile = this.getAbsoluteFile();
		Files.move(currentFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
		this.filename = targetFile.getName();
		this.setRelativeFolderPath(yadaUtil.relativize(config.getBasePath(), targetPath.getParent()));
		return this;
	}

	/**
	 * Returns the url to show this file on the web, but only if it is publicly visible (in a subfolder of the "contents" folder)
	 * @return
	 */
	@Transient
	public String getUrl() {
		if (privateAccess) {
			throw new YadaInvalidUsageException("Trying to get a url of a private file: {}", this.getAbsoluteFile());
		}
		Path relativePath = config.getContentsFolder().toPath().relativize(this.getAbsoluteFile().toPath());
		return yadaWebUtil.makeUrl(config.getContentUrl(), relativePath.toString());
	}

	/**
	 * Returns the absolute file on the filesystem
	 * @return
	 */
	@Transient
	public File getAbsoluteFile() {
		File result = config.getBasePath().toFile();
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

	/**
	 * Returns the path of the folder where this file is stored, relative to the "base path"
	 * @return
	 */
	public String getRelativeFolderPath() {
		return relativeFolderPath;
	}

	/**
	 * Sets the path of the folder where this file is stored, relative to the "base path"
	 * @param relativeFolderPath
	 */
	public void setRelativeFolderPath(String relativeFolderPath) {
		this.relativeFolderPath = relativeFolderPath;
	}
	
	/**
	 * Sets the path of the folder where this file is stored, relative to the "base path"
	 * @param relativeFolderPath
	 */
	@Transient
	public void setRelativeFolderPath(Path relativeFolderPath) {
		this.relativeFolderPath = relativeFolderPath!=null?relativeFolderPath.toString():null;
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

	public boolean isExpired() {
		return temporary || (expirationTimestamp!=null && !expirationTimestamp.after(new Date()));
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

	public boolean isPrivateAccess() {
		return privateAccess;
	}

	public void setPrivateAccess(boolean privateAccess) {
		this.privateAccess = privateAccess;
	}

	public Map<String, YadaManagedFile> getDerivedAssets() {
		return derivedAssets;
	}

	public void setDerivedAssets(Map<String, YadaManagedFile> derivedAssets) {
		this.derivedAssets = derivedAssets;
	}

	public Date getExpirationTimestamp() {
		return expirationTimestamp;
	}

	public void setExpirationTimestamp(Date expirationTimestamp) {
		this.expirationTimestamp = expirationTimestamp;
	}

	public void setVersion(long version) {
		this.version = version;
	}

}
