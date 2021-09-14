package net.yadaframework.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.yadaframework.components.YadaFileManager;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.exceptions.YadaSystemException;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.entity.YadaManagedFile;
import net.yadaframework.persistence.repository.YadaAttachedFileDao;
import net.yadaframework.raw.YadaIntDimension;

/**
 * Kept in HttpSession, it stores all info needed to crop images.
 *
 */
public class YadaCropImage {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	// Autowiring must be performed by yadaUtil.autowireAndInitialize()
	private @Autowired YadaAttachedFileDao yadaAttachedFileDao;
	private @Autowired YadaUtil yadaUtil;
	// private @Autowired YadaConfiguration config;

	private YadaManagedFile imageToCrop = null;			// Uploaded file to crop
	private YadaAttachedFile yadaAttachedFile = null;	// File to replace or to create

	private String titleKey; // key in messages.properties for the title of the crop page

	private boolean cropDesktop = false; 	// True to crop for desktop image
	private boolean cropMobile = false;		// True to crop for mobile image
	private boolean cropPdf = false;		// True to crop for pdf image

	private YadaIntDimension targetDesktopDimension;	// After crop, the image will be shrunk to this value
	private YadaIntDimension targetMobileDimension;		// After crop, the image will be shrunk to this value
	private YadaIntDimension targetPdfDimension;		// After crop, the image will be shrunk to this value

	/**
	 * Folder where to place cropped images, relative to the contents folder
	 */
	private String targetRelativeFolder; 	// e.g. "/images/products"
	/**
	 * Prefix to use for the target file, can be empty or null
	 */
	private String targetNamePrefix;		// e,g. "urban-"

	String clientFilename = null; // The name the file had on the client

	/**
	 * Package-accessible constructor used by YadaCropQueue
	 * @param imageToCrop
	 * @param targetDimensions image target desktop and mobile dimensions. Use null when a shrink is not needed, use a null dimension when that crop is not needed.
	 * @throws IOException 
	 */
	// Package visibility
	YadaCropImage(YadaFileManager yadaFileManager, YadaManagedFile imageToCrop, YadaIntDimension[] targetDimensions, String targetRelativeFolder, String targetNamePrefix) {
		try {
			// Move the image from the private uploads folder to the public temp folder
			yadaFileManager.moveToTemp(imageToCrop);
		} catch (IOException e) {
			log.error("Failed to move image to temp folder", e);
			throw new YadaSystemException("File copy error", e);
		}
		this.imageToCrop = imageToCrop;
		this.targetRelativeFolder = targetRelativeFolder;
		this.targetNamePrefix = targetNamePrefix;
		if (targetDimensions!=null) {
			this.targetDesktopDimension = targetDimensions[0];
			this.targetMobileDimension = targetDimensions[1];
			this.targetPdfDimension = targetDimensions[2];
		}
		this.clientFilename = this.imageToCrop.getClientFilename();
		cropDesktop |= targetDesktopDimension!=null;
		cropMobile |= targetMobileDimension!=null;
		cropPdf |= targetPdfDimension!=null;
	}

	/**
	 * Used on the browser
	 * @return
	 */
	public double getTargetMobileProportions() {
		if (targetMobileDimension!=null) {
			return (double)targetMobileDimension.getWidth()/(double)targetMobileDimension.getHeight();
		}
		return 1;
	}

	/**
	 * Used on the browser
	 * @return
	 */
	public double getTargetDesktopProportions() {
		if (targetDesktopDimension!=null) {
			return (double)targetDesktopDimension.getWidth()/(double)targetDesktopDimension.getHeight();
		}
		return 1;
	}

	/**
	 * Used on the browser
	 * @return
	 */
	public double getTargetPdfProportions() {
		if (targetPdfDimension!=null) {
			return (double)targetPdfDimension.getWidth()/(double)targetPdfDimension.getHeight();
		}
		return 1;
	}

	/**
	 * Returns the url of the image to be cropped
	 * @return
	 */
	public String getSourceUrl() {
		if (imageToCrop!=null) {
			return imageToCrop.getUrl();
		}
		return null;
	}

	/**
	 * Link to an Entity by means of a new YadaAttachedFile. Useful when adding a new image to a list.
	 * @param attachedToId the id of the entity owning the new YadaAttachedFile
	 * @param clientFilename
	 * @return new YadaAttachedFile to add to the list
	 */
	public YadaAttachedFile linkAdd() {
		YadaAttachedFile newYadaAttachedFile = link(null);
		return newYadaAttachedFile;
	}

	/**
	 * Link to an Entity by means of a YadaAttachedFile.
	 * @param someYadaAttachedFile an existing YadaAttachedFile or null to create a new one.
	 * @param clientFilename
	 * @return the existing or a new YadaAttachedFile to set on the entity
	 */
	public YadaAttachedFile link(YadaAttachedFile someYadaAttachedFile) {
		// this is null and new is null --> create instance
		// this is null and new is not null --> set
		// this is not null and new is null --> check consistency and do nothing
		// this is not null and new is not null --> check consistency and do nothing

		if (this.yadaAttachedFile!=null) {
			// Check for consistency and do nothing if all is good
			if (someYadaAttachedFile!=null) {
				if (!this.yadaAttachedFile.getId().equals(someYadaAttachedFile.getId())) {
					throw new YadaInvalidUsageException("yadaAttachedFile {} already attached to entity", this.yadaAttachedFile.getId());
				}
			}
			log.warn("Attaching an already attached file (ignored)");
			return this.yadaAttachedFile;
		}

		// this.yadaAttachedFile is null

		if (someYadaAttachedFile==null) {
			someYadaAttachedFile = new YadaAttachedFile();
		}
		// someYadaAttachedFile.setAttachedToId(attachedToId);
		someYadaAttachedFile.setRelativeFolderPath(targetRelativeFolder);
		someYadaAttachedFile.setClientFilename(clientFilename);
		YadaIntDimension dimension = yadaUtil.getImageDimension(imageToCrop.getAbsoluteFile());
		someYadaAttachedFile.setImageDimension(dimension);
		if (someYadaAttachedFile.getId()==null) {
			someYadaAttachedFile = yadaAttachedFileDao.save(someYadaAttachedFile);
		}
		this.yadaAttachedFile = someYadaAttachedFile;
		return someYadaAttachedFile;
	}

	/**
	 * Enables image cropping for desktop size
	 * @return
	 */
	public YadaCropImage cropDesktop() {
		this.cropDesktop = true;
		return this;
	}

	/**
	 * Enables image cropping for mobile size
	 * @return
	 */
	public YadaCropImage cropMobile() {
		this.cropMobile = true;
		return this;
	}

	/**
	 * Enables image cropping for pdf size
	 * @return
	 */
	public YadaCropImage cropPdf() {
		this.cropPdf= true;
		return this;
	}

	public YadaManagedFile getImageToCrop() {
		return imageToCrop;
	}

	@SuppressWarnings("unused")
	private void setImageToCrop(YadaManagedFile imageToCrop) {
		this.imageToCrop = imageToCrop;
	}

	public YadaAttachedFile getYadaAttachedFile() {
		return yadaAttachedFile;
	}

	public void setYadaAttachedFile(YadaAttachedFile yadaAttachedFile) {
		this.yadaAttachedFile = yadaAttachedFile;
	}

	public String getTargetRelativeFolder() {
		return targetRelativeFolder;
	}

	@SuppressWarnings("unused")
	private void setTargetRelativeFolder(String targetRelativeFolder) {
		this.targetRelativeFolder = targetRelativeFolder;
	}

	public String getTargetNamePrefix() {
		return targetNamePrefix;
	}

	@SuppressWarnings("unused")
	private void setTargetNamePrefix(String targetNamePrefix) {
		this.targetNamePrefix = targetNamePrefix;
	}

	public YadaIntDimension getTargetDesktopDimension() {
		return targetDesktopDimension;
	}

	@SuppressWarnings("unused")
	private void setTargetDesktopDimension(YadaIntDimension targetDesktopDimension) {
		this.targetDesktopDimension = targetDesktopDimension;
	}

	public YadaIntDimension getTargetMobileDimension() {
		return targetMobileDimension;
	}

	@SuppressWarnings("unused")
	private void setTargetMobileDimension(YadaIntDimension targetMobileDimension) {
		this.targetMobileDimension = targetMobileDimension;
	}

	public boolean isCropDesktop() {
		return cropDesktop;
	}

	@SuppressWarnings("unused") // Use cropDesktop() instead
	private void setCropDesktop(boolean cropDesktop) {
		this.cropDesktop = cropDesktop;
	}

	public boolean isCropMobile() {
		return cropMobile;
	}

	@SuppressWarnings("unused") // Use cropMobile() instead
	private void setCropMobile(boolean cropMobile) {
		this.cropMobile = cropMobile;
	}

	public String getTitleKey() {
		return titleKey;
	}

	/**
	 * Set the title to show when cropping this image
	 * @param titleKey a key in messages.properties
	 * @return
	 */
	public YadaCropImage titleKey(String titleKey) {
		this.titleKey = titleKey;
		return this;
	}

	@SuppressWarnings("unused") // Use titleKey() instead
	private void setTitleKey(String titleKey) {
		this.titleKey = titleKey;
	}

	public boolean isCropPdf() {
		return cropPdf;
	}

	@SuppressWarnings("unused") // Use cropMobile() instead
	private void setCropPdf(boolean cropPdf) {
		this.cropPdf = cropPdf;
	}

	public YadaIntDimension getTargetPdfDimension() {
		return targetPdfDimension;
	}

	@SuppressWarnings("unused")
	private void setTargetPdfDimension(YadaIntDimension targetPdfDimension) {
		this.targetPdfDimension = targetPdfDimension;
	}

}
