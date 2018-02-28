package net.yadaframework.cms.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.yadaframework.cms.persistence.entity.YadaAttachedFile;
import net.yadaframework.cms.persistence.repository.YadaAttachedFileRepository;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;

/**
 * The File Manager handles uploaded files. They are kept in a specific folder where they can be chosen to be attached to entities. 
 *
 */
@Service
public class YadaFileManager {
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired private YadaAttachedFileRepository yadaAttachedFileRepository;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaUtil yadaUtil;
	
	protected String COUNTER_SEPARATOR="_";
	
	public void deleteFileAttachment(Long yadaAttachedFileId) {
		// TODO !!!!!!!!!!!!!!!!!!!!!!!!!! qui devo cancellare il file da filesystem
		
		
	}

	/**
	 * Returns the (relative) url of the mobile image if any, otherwise fallback to the desktop image
	 * @param yadaAttachedFile
	 * @return
	 */
	public String getMobileImageUrl(YadaAttachedFile yadaAttachedFile) {
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
	 * Returns the (relative) url of the desktop image
	 * @param yadaAttachedFile
	 * @return
	 */
	public String getDesktopImageUrl(YadaAttachedFile yadaAttachedFile) {
		String imageName = yadaAttachedFile.getFilenameDesktop();
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
	 * Copies (and resizes) a managed file to the destination folder, creating a database association to assign to an Entity
	 * @param managedFile
	 * @param relativeFolderPath path of the target folder relative to the contents folder
	 * @param baseName
	 * @param targetExtension optional, to convert image file formats
	 * @param desktopWidth optional width for desktop images - when null, the image is not resized
	 * @param mobileWidth optional width for mobile images - when null, the mobile file is the same as the desktop
	 * @return
	 * @throws IOException 
	 */
	public YadaAttachedFile attach(File managedFile, String relativeFolderPath, String baseName, String targetExtension, Integer desktopWidth, Integer mobileWidth) throws IOException {
		File targetFolder = new File(config.getContentPath(), relativeFolderPath);
		targetFolder.mkdirs();
		String[] filenameParts = YadaUtil.splitFileNameAndExtension(managedFile.getName());
		String origExtension = filenameParts[1]; // jpg or pdf
		String filenameNoCounter = YadaUtil.stripCounterFromFilename(managedFile.getName(), COUNTER_SEPARATOR);
		//
		// Now prefix doesn't have any counter at the end
		YadaAttachedFile yadaAttachedFile = new YadaAttachedFile();
		yadaAttachedFile = yadaAttachedFileRepository.save(yadaAttachedFile); // Get the id
		String targetFilenamePrefix = filenameNoCounter + COUNTER_SEPARATOR + yadaAttachedFile.getId(); // product_2631
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
