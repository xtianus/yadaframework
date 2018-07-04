package net.yadaframework.cms.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.repository.YadaAttachedFileRepository;

/**
 * The File Manager handles uploaded files. They are kept in a specific folder where they can be 
 * chosen to be attached to entities. 
 *
 */
@Service
public class YadaFileManager {
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired private YadaAttachedFileRepository yadaAttachedFileRepository;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaUtil yadaUtil;
	
	protected String COUNTER_SEPARATOR="_";
	
	// TODO distinguere tra mobile portrait e mobile landscape
	// TODO le dimensioni mobile/desktop devono essere configurabili
	// TODO mantenere l'immagine caricata nella versione originale
	
	/**
	 * Returns the absolute path of the default file (no mobile/desktop variant)
	 * @param yadaAttachedFile the attachment
	 * @return
	 */
	public File getAbsoluteFile(YadaAttachedFile yadaAttachedFile) {
		File targetFolder = new File(config.getContentPath(), yadaAttachedFile.getRelativeFolderPath());
		return new File(targetFolder, yadaAttachedFile.getFilename());
	}

	/**
	 * Returns the absolute path of a file
	 * @param yadaAttachedFile the attachment
	 * @param filename the relative file name, can be yadaAttachedFile.getFilename(), yadaAttachedFile.getFilenameDesktop(), yadaAttachedFile.getFilenameMobile()
	 * @return
	 */
	public File getAbsoluteFile(YadaAttachedFile yadaAttachedFile, String filename) {
		File targetFolder = new File(config.getContentPath(), yadaAttachedFile.getRelativeFolderPath());
		return new File(targetFolder, filename);
	}
	
	/**
	 * Deletes from the filesystem all files related to the attachment
	 * @param yadaAttachedFileId the attachment id
	 * @see #deleteFileAttachment(YadaAttachedFile)
	 */
	public void deleteFileAttachment(Long yadaAttachedFileId) {
		deleteFileAttachment(yadaAttachedFileRepository.findOne(yadaAttachedFileId));
	}
	
	/**
	 * Deletes from the filesystem all files related to the attachment
	 * @param yadaAttachedFile the attachment
	 * @see #deleteFileAttachment(Long)
	 */
	public void deleteFileAttachment(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile.getFilename() != null) {
			getAbsoluteFile(yadaAttachedFile, yadaAttachedFile.getFilename()).delete();
		}
		if (yadaAttachedFile.getFilenameDesktop() != null) {
			getAbsoluteFile(yadaAttachedFile, yadaAttachedFile.getFilenameDesktop()).delete();
		}
		if (yadaAttachedFile.getFilenameMobile() != null) {
			getAbsoluteFile(yadaAttachedFile, yadaAttachedFile.getFilenameMobile()).delete();
		}
	}
	
	/**
	 * Deletes from the filesystem all files related to the attachments
	 * @param yadaAttachedFiles the attachments
	 */
	public void deleteFileAttachment(List<YadaAttachedFile> yadaAttachedFiles) {
		for (YadaAttachedFile yadaAttachedFile : yadaAttachedFiles) {
			deleteFileAttachment(yadaAttachedFile);
		}
	}

	/**
	 * Returns the (relative) url of the mobile image if any, otherwise fallback to the desktop image
	 * @param yadaAttachedFile
	 * @return
	 */
	public String getMobileImageUrl(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile==null) {
			return null;
		}
		String imageName = yadaAttachedFile.getFilenameMobile();
		if (imageName==null) {
			return getDesktopImageUrl(yadaAttachedFile);
		}
		StringBuilder result = new StringBuilder(config.getContentUrl());
		result.append(yadaAttachedFile.getRelativeFolderPath())
			.append("/")
			.append(imageName);
		return result.toString();
	}
	
	/**
	 * Returns the (relative) url of the desktop image. If not defined, falls back to the plain file.
	 * @param yadaAttachedFile
	 * @return
	 */
	public String getDesktopImageUrl(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile==null) {
			return null;
		}
		String imageName = yadaAttachedFile.getFilenameDesktop();
		if (imageName==null) {
			imageName = yadaAttachedFile.getFilename();
		}
		StringBuilder result = new StringBuilder(config.getContentUrl());
		result.append(yadaAttachedFile.getRelativeFolderPath())
		.append("/")
		.append(imageName);
		return result.toString();
	}
	
	/**
	 * Copies a received file to the upload folder
	 * @param multipartFile file coming from the http request
	 * @return the uploaded file with a unique name
	 * @throws IOException
	 */
	public File uploadFile(MultipartFile multipartFile) throws IOException {
		String originalFilename = multipartFile.getOriginalFilename();
		String targetName = YadaUtil.reduceToSafeFilename(originalFilename);
		String[] filenameParts = YadaUtil.splitFileNameAndExtension(targetName);
		File targetFolder = config.getUploadFolder();
		targetFolder.mkdirs();
		File targetFile = YadaUtil.findAvailableName(targetFolder, filenameParts[0], filenameParts[1], COUNTER_SEPARATOR);
		try (InputStream inputStream = multipartFile.getInputStream(); OutputStream outputStream = new FileOutputStream(targetFile)) {
			IOUtils.copy(inputStream, outputStream);
		} catch (IOException e) {
			throw e;
		}
		log.debug("File {} uploaded", targetFile.getAbsolutePath());
		return targetFile;
	}
	
	/**
	 * Copies a managed file to the destination folder, creating a database association to assign to an Entity.
	 * The name of the file is in the format [basename]managedFileName_id.ext.
	 * Images are not resized.
	 * @param attachToId the id of the entity to which the file should be attached
	 * @param managedFile an uploaded file, can be an image or not
	 * @param relativeFolderPath path of the target folder relative to the contents folder
	 * @param baseName prefix to attach on the file name. Add a separator if you need one. Can be null.
	 * @return
	 * @throws IOException 
	 * @see {@link #attach(File, String, String, String, Integer, Integer)}
	 */
	public YadaAttachedFile attach(Long attachToId, File managedFile, String relativeFolderPath, String baseName) throws IOException {
		return attach(attachToId, managedFile, relativeFolderPath, baseName, null, null, null);
	}
	
	/**
	 * Copies (and resizes) a managed file to the destination folder, creating a database association to assign to an Entity.
	 * The name of the file is in the format [basename]managedFileName_id.ext
	 * @param attachToId the id of the entity to which the file should be attached
	 * @param managedFile an uploaded file, can be an image or not
	 * @param relativeFolderPath path of the target folder relative to the contents folder
	 * @param baseName prefix to attach on the file name. Add a separator if you need one. Can be null.
	 * @param targetExtension optional, to convert image file formats
	 * @param desktopWidth optional width for desktop images - when null, the image is not resized
	 * @param mobileWidth optional width for mobile images - when null, the mobile file is the same as the desktop
	 * @return
	 * @throws IOException
	 * @see {@link #attach(File, String, String, String)} 
	 */
	public YadaAttachedFile attach(Long attachToId, File managedFile, String relativeFolderPath, String baseName, String targetExtension, Integer desktopWidth, Integer mobileWidth) throws IOException {
		File targetFolder = new File(config.getContentPath(), relativeFolderPath);
		targetFolder.mkdirs();
		String[] filenameParts = YadaUtil.splitFileNameAndExtension(managedFile.getName());
		String filenameNoCounter = filenameParts[0];
		String origExtension = filenameParts[1]; // jpg or pdf
//		String filenameNoCounter = YadaUtil.stripCounterFromFilename(managedFile.getName(), COUNTER_SEPARATOR);
		//
		// Now prefix doesn't have any counter at the end
		YadaAttachedFile yadaAttachedFile = new YadaAttachedFile();
		yadaAttachedFile.setAttachedToId(attachToId);
		yadaAttachedFile.setClientFilename(managedFile.getName());
		yadaAttachedFile = yadaAttachedFileRepository.save(yadaAttachedFile); // Get the id
		yadaAttachedFile.setSortOrder(yadaAttachedFile.getId()); // Set the sort order to the id, so that this will be the last image in a list
		String targetFilenamePrefix = baseName.trim() + filenameNoCounter + COUNTER_SEPARATOR + yadaAttachedFile.getId(); // product_2631
		targetFilenamePrefix = YadaUtil.reduceToSafeFilename(targetFilenamePrefix, true);
		boolean imageExtensionChanged = targetExtension!=null && targetExtension.compareToIgnoreCase(origExtension)!=0;
		boolean requiresTransofmation = imageExtensionChanged || desktopWidth!=null || mobileWidth!=null; 
		if (targetExtension==null) {
			targetExtension = origExtension;
		}
		if (!requiresTransofmation) {
			// No transformation: copy bytes
			File targetFile = new File(targetFolder, targetFilenamePrefix + "." + targetExtension);
			try (InputStream inputStream = new FileInputStream(managedFile); OutputStream outputStream = new FileOutputStream(targetFile)) {
				IOUtils.copy(inputStream, outputStream);
			} catch (IOException e) {
				throw e;
			}
			yadaAttachedFile.setFilename(targetFile.getName());
		} else {
			// Transformation: copy with imagemagick
			File desktopFile = resizeIfNeeded(managedFile, targetFolder, targetFilenamePrefix, targetExtension, desktopWidth);
			yadaAttachedFile.setFilenameDesktop(desktopFile.getName());
			if (mobileWidth==null) {
				yadaAttachedFile.setFilenameMobile(null); // No mobile image
				yadaAttachedFile.setFilename(desktopFile.getName());
			} else {
				File mobileFile = resizeIfNeeded(managedFile, targetFolder, targetFilenamePrefix, targetExtension, mobileWidth);
				yadaAttachedFile.setFilenameMobile(mobileFile.getName());
				yadaAttachedFile.setFilename(null);
			}
		}
		if (config.isFileManagerDeletingUploads()) {
			log.debug("Deleting file {}", managedFile.getAbsolutePath());
			managedFile.delete();
		}
		yadaAttachedFile.setRelativeFolderPath(relativeFolderPath);
		return yadaAttachedFileRepository.save(yadaAttachedFile);
	}
	
	private File resizeIfNeeded(File managedFile, File targetFolder, String targetFilenamePrefix, String targetExtension, Integer targetWidth) {
		if (targetWidth==null) {
			// Convert only
			File targetFile = new File(targetFolder, targetFilenamePrefix + "." + targetExtension);
			Map<String,String> params = new HashMap<String,String>();
			params.put("FILENAMEIN", managedFile.getAbsolutePath());
			params.put("FILENAMEOUT", targetFile.getAbsolutePath());
			boolean convert = yadaUtil.exec("config/shell/convert", params);
			if (!convert) {
				log.error("Image not copied when making attachment: {}", targetFile);
			}			
			return targetFile;
		} else {
			// Resize
			File targetFile = new File(targetFolder, targetFilenamePrefix + COUNTER_SEPARATOR + targetWidth + "." + targetExtension);
			Map<String,String> params = new HashMap<String,String>();
			params.put("FILENAMEIN", managedFile.getAbsolutePath());
			params.put("FILENAMEOUT", targetFile.getAbsolutePath());
			params.put("W", Integer.toString(targetWidth));
			params.put("H", ""); // the height must be empty to keep the original proportions and resize based on width
			boolean resized = yadaUtil.exec("config/shell/resize", params);
			if (!resized) {
				log.error("Image not resized when making attachment: {}", targetFile);
			}
			return targetFile;
		}
	}
	

}
