package net.yadaframework.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.entity.YadaManagedFile;
import net.yadaframework.persistence.repository.YadaAttachedFileRepository;
import net.yadaframework.persistence.repository.YadaFileManagerDao;
import net.yadaframework.raw.YadaIntDimension;

/**
 * The File Manager handles uploaded files. They are kept in a specific folder where they can be
 * chosen to be attached to entities.
 *
 */
// Not in YadaWebCMS because used by YadaSession and YadaUtil
@Service
public class YadaFileManager {
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired private YadaAttachedFileRepository yadaAttachedFileRepository;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaUtil yadaUtil;
	@Autowired private YadaFileManagerDao yadaFileManagerDao;

	protected String COUNTER_SEPARATOR="_";

	// TODO distinguere tra mobile portrait e mobile landscape
	// TODO le dimensioni mobile/desktop devono essere configurabili
	// TODO mantenere l'immagine caricata nella versione originale

	/**
	 * Remove a managed file from disk and database
	 * @param managedFile
	 * @return true when deleted from disk, false when not deleted from disk (could have been deleted from db though)
	 */
	public boolean delete(YadaManagedFile managedFile) {
		return yadaFileManagerDao.delete(managedFile);
	}

	/**
	 * Returns the absolute path of a managed file
	 * @param yadaAttachedFile the attachment
	 * @param filename the relative file name, can be yadaAttachedFile.getFilename(), yadaAttachedFile.getFilenameDesktop(), yadaAttachedFile.getFilenameMobile()
	 * @return the File or null
	 */
	public File getAbsoluteFile(YadaManagedFile managedFile) {
		if (managedFile==null) {
			return null;
		}
		return managedFile.getAbsoluteFile();
	}

//	/**
//	 * Deletes a file from disk and database
//	 * @param managedFile the file to delete
//	 */
//	public void delete(YadaManagedFile managedFile) {
//		yadaFileManagerDao.delete(managedFile);
//	}

	/**
	 * Makes a copy of just the filesystem files. New names are generated from the old ones by appending an incremental number.
	 * The source YadaAttachedFile is updated with the new names. The old files are not deleted.
	 * Use case: you clone an instance of YadaAttachedFile with YadaUtil.copyEntity() then you need to copy its files too.
	 * @param yadaAttachedFile a copy of another YadaAttachedFile
	 * @return the saved YadaAttachedFile
	 * @throws IOException
	 */
	public YadaAttachedFile duplicateFiles(YadaAttachedFile yadaAttachedFile) throws IOException {
		if (yadaAttachedFile==null) {
			return null;
		}
		File newFile = null;
		File sourceFile = getAbsoluteMobileFile(yadaAttachedFile);
		if (sourceFile!=null) {
			newFile = YadaUtil.findAvailableName(sourceFile, null);
			try (InputStream inputStream = new FileInputStream(sourceFile); OutputStream outputStream = new FileOutputStream(newFile)) {
				IOUtils.copy(inputStream, outputStream);
			}
			yadaAttachedFile.setFilenameMobile(newFile.getName());
		}
		sourceFile = getAbsoluteDesktopFile(yadaAttachedFile);
		if (sourceFile!=null) {
			newFile = YadaUtil.findAvailableName(sourceFile, null);
			try (InputStream inputStream = new FileInputStream(sourceFile); OutputStream outputStream = new FileOutputStream(newFile)) {
				IOUtils.copy(inputStream, outputStream);
			}
			yadaAttachedFile.setFilenameDesktop(newFile.getName());
		}
		sourceFile = getAbsoluteFile(yadaAttachedFile);
		if (sourceFile!=null) {
			newFile = YadaUtil.findAvailableName(sourceFile, null);
			try (InputStream inputStream = new FileInputStream(sourceFile); OutputStream outputStream = new FileOutputStream(newFile)) {
				IOUtils.copy(inputStream, outputStream);
			}
			yadaAttachedFile.setFilename(newFile.getName());
		}
		return yadaAttachedFileRepository.save(yadaAttachedFile);
	}

	/**
	 * Returns the absolute path of the mobile file
	 * @param yadaAttachedFile the attachment
	 * @return the File or null
	 */
	public File getAbsoluteMobileFile(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile!=null) {
			return getAbsoluteFile(yadaAttachedFile, yadaAttachedFile.getFilenameMobile());
		}
		return null;
	}

	/**
	 * Returns the absolute path of the desktop file
	 * @param yadaAttachedFile the attachment
	 * @return the File or null
	 */
	public File getAbsoluteDesktopFile(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile!=null) {
			return getAbsoluteFile(yadaAttachedFile, yadaAttachedFile.getFilenameDesktop());
		}
		return null;
	}

	/**
	 * Returns the absolute path of the default file (no mobile/desktop variant)
	 * @param yadaAttachedFile the attachment
	 * @return the File or null
	 */
	public File getAbsoluteFile(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile!=null) {
			return getAbsoluteFile(yadaAttachedFile, yadaAttachedFile.getFilename());
		}
		return null;
	}

	/**
	 * Returns the absolute path of a file
	 * @param yadaAttachedFile the attachment
	 * @param filename the relative file name, can be yadaAttachedFile.getFilename(), yadaAttachedFile.getFilenameDesktop(), yadaAttachedFile.getFilenameMobile()
	 * @return the File or null
	 */
	public File getAbsoluteFile(YadaAttachedFile yadaAttachedFile, String filename) {
		if (filename==null || yadaAttachedFile==null) {
			return null;
		}
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
		if (yadaAttachedFile==null) {
			return;
		}
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
	 * Returns the (relative) url of the mobile image if any, or null
	 * @param yadaAttachedFile
	 * @return
	 */
	public String getMobileImageUrl(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile==null) {
			return null;
		}
		String imageName = yadaAttachedFile.getFilenameMobile();
		if (imageName==null) {
			return null;
		}
		return computeUrl(yadaAttachedFile, imageName);
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
			return getFileUrl(yadaAttachedFile);
		}
		return computeUrl(yadaAttachedFile, imageName);
	}

	/**
	 * Returns the (relative) url of the file, or null.
	 * @param yadaAttachedFile
	 * @return
	 */
	public String getFileUrl(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile==null) {
			return null;
		}
		String imageName = yadaAttachedFile.getFilename();
		if (imageName==null) {
			return null;
		}
		return computeUrl(yadaAttachedFile, imageName);
	}

	private String computeUrl(YadaAttachedFile yadaAttachedFile, String imageName) {
		StringBuilder result = new StringBuilder(config.getContentUrl());
		result.append(yadaAttachedFile.getRelativeFolderPath())
		.append("/")
		.append(imageName);
		return result.toString();
	}

	/**
	 * Uploads a file into the uploads folder.
	 * @param multipartFile
	 * @return
	 * @throws IOException
	 */
	private File uploadFileInternal(MultipartFile multipartFile) throws IOException {
		String originalFilename = multipartFile.getOriginalFilename();
		String targetName = YadaUtil.reduceToSafeFilename(originalFilename);
		String[] filenameParts = YadaUtil.splitFileNameAndExtension(targetName);
		File targetFolder = config.getUploadsFolder();
		// Useless: doesn't throw an exception when it fails: targetFolder.mkdirs();
		File targetFile = YadaUtil.findAvailableName(targetFolder, filenameParts[0], filenameParts[1], COUNTER_SEPARATOR);
		multipartFile.transferTo(targetFile);
		//		try (InputStream inputStream = multipartFile.getInputStream(); OutputStream outputStream = new FileOutputStream(targetFile)) {
		//			IOUtils.copy(inputStream, outputStream);
		//		} catch (IOException e) {
		//			throw e;
		//		}
		log.debug("File {} uploaded", targetFile.getAbsolutePath());
		return targetFile;
	}

	/**
	 * Copies a received file to the upload folder. The returned File is the only pointer to the uploaded file.
	 * @param multipartFile file coming from the http request
	 * @return the uploaded file with a unique name, or null if the user did not send any file
	 * @throws IOException
	 */
	public File uploadFile(MultipartFile multipartFile) throws IOException {
		if (multipartFile==null || multipartFile.getSize()==0) {
			log.debug("No file sent for upload");
			return null;
		}
		File targetFile = uploadFileInternal(multipartFile);
		return targetFile;
	}

	/**
	 * Copies a received file to the upload folder. A pointer to the file is stored in the database as
	 * @param multipartFile file coming from the http request
	 * @return the uploaded file with a unique name, or null if the user did not send any file
	 * @throws IOException
	 */
	public YadaManagedFile manageFile(MultipartFile multipartFile) throws IOException {
		return manageFile(multipartFile, null);
	}

	/**
	 * Copies a received file to the upload folder. A pointer to the file is stored in the database as
	 * @param multipartFile file coming from the http request
	 * @param description a user description for the file
	 * @return the uploaded file with a unique name, or null if the user did not send any file
	 * @throws IOException
	 */
	public YadaManagedFile manageFile(MultipartFile multipartFile, String description) throws IOException {
		if (multipartFile==null || multipartFile.getSize()==0) {
			log.debug("No file sent for upload");
			return null;
		}
		File targetFile = uploadFileInternal(multipartFile);
		YadaManagedFile yadaManagedFile = yadaFileManagerDao.createManagedFile(multipartFile, targetFile, description);
		return yadaManagedFile;
	}

	/**
	 * Replace the file associated with the current attachment
	 * @param currentAttachedFile an existing attachment
	 * @param managedFile the new file to set
	 * @param multipartFile the original uploaded file, to get the client filename. If null, the client filename is not changed.
	 * @return
	 * @throws IOException
	 */
	public YadaAttachedFile attachReplace(YadaAttachedFile currentAttachedFile, File managedFile, MultipartFile multipartFile, String namePrefix) throws IOException {
		return attachReplace(currentAttachedFile, managedFile, multipartFile, namePrefix, null, null, null);
	}

	/**
	 * Replace the file associated with the current attachment, only if a file was actually attached
	 * @param currentAttachedFile an existing attachment
	 * @param managedFile the new file to set
	 * @param multipartFile the original uploaded file, to get the client filename. If null, the client filename is not changed.
	 * @param targetExtension optional, to convert image file formats
	 * @param desktopWidth optional width for desktop images - when null, the image is not resized
	 * @param mobileWidth optional width for mobile images - when null, the mobile file is the same as the desktop
	 * @return YadaAttachedFile if the file is uploaded, null if no file was sent by the user
	 * @throws IOException
	 */
	public YadaAttachedFile attachReplace(YadaAttachedFile currentAttachedFile, File managedFile, MultipartFile multipartFile, String namePrefix, String targetExtension, Integer desktopWidth, Integer mobileWidth) throws IOException {
		if (managedFile==null) {
			return null;
		}
		deleteFileAttachment(currentAttachedFile); // Delete any previous attached files
		return attach(currentAttachedFile, managedFile, multipartFile, namePrefix, targetExtension, desktopWidth, mobileWidth);
	}

	/**
	 * Copies a managed file to the destination folder, creating a database association to assign to an Entity.
	 * The name of the file is in the format [basename]managedFileName_id.ext.
	 * Images are not resized.
	 * @param attachToId the id of the entity to which the file should be attached
	 * @param managedFile an uploaded file, can be an image or not
	 * @param multipartFile the original uploaded file, to get the client filename. If null, the client filename is not set.
	 * @param relativeFolderPath path of the target folder relative to the contents folder
	 * @param namePrefix prefix to attach before the original file name. Add a separator if you need one. Can be null.
	 * @return YadaAttachedFile if the file is uploaded, null if no file was sent by the user
	 * @throws IOException
	 * @see {@link #attach(File, String, String, String, Integer, Integer)}
	 */
	public YadaAttachedFile attachNew(Long attachToId, File managedFile, MultipartFile multipartFile, String relativeFolderPath, String namePrefix) throws IOException {
		return attachNew(attachToId, managedFile, multipartFile, relativeFolderPath, namePrefix, null, null, null);
	}

	/**
	 * Copies (and resizes) a managed file to the destination folder, creating a database association to assign to an Entity.
	 * The name of the file is in the format [basename]managedFileName_id.ext
	 * @param attachToId the id of the entity to which the file should be attached
	 * @param managedFile an uploaded file, can be an image or not. When null, nothing is done.
	 * @param multipartFile the original uploaded file, to get the client filename. If null, the client filename is not changed.
	 * @param relativeFolderPath path of the target folder relative to the contents folder, starting with a slash /
	 * @param namePrefix prefix to attach before the original file name. Add a separator if you need one. Can be null.
	 * @param targetExtension optional, to convert image file formats
	 * @param desktopWidth optional width for desktop images - when null, the image is not resized
	 * @param mobileWidth optional width for mobile images - when null, the mobile file is the same as the desktop
	 * @return YadaAttachedFile if the file is uploaded, null if no file was sent by the user
	 * @throws IOException
	 * @see {@link #attach(File, String, String, String)}
	 */
	public YadaAttachedFile attachNew(Long attachToId, File managedFile, MultipartFile multipartFile, String relativeFolderPath, String namePrefix, String targetExtension, Integer desktopWidth, Integer mobileWidth) throws IOException {
		if (managedFile==null) {
			return null;
		}
		if (!relativeFolderPath.startsWith("/") && !relativeFolderPath.startsWith("\\")) {
			relativeFolderPath = "/" + relativeFolderPath;
			log.warn("The relativeFolderPath '{}' should have a leading slash (fixed)", relativeFolderPath);
		}
		YadaAttachedFile yadaAttachedFile = new YadaAttachedFile(attachToId);
		// yadaAttachedFile.setAttachedToId(attachToId);
		yadaAttachedFile.setRelativeFolderPath(relativeFolderPath);
		// This save should not bee needed anymore because of @PostPersist in YadaAttachedFile
		yadaAttachedFile = yadaAttachedFileRepository.save(yadaAttachedFile); // Get the id
		File targetFolder = new File(config.getContentPath(), relativeFolderPath);
		targetFolder.mkdirs();
		return attach(yadaAttachedFile, managedFile, multipartFile, namePrefix, targetExtension, desktopWidth, mobileWidth);
	}

	/**
	 * Performs file copy and (for images) resize to different versions
	 * @param yadaAttachedFile object to fill with values
	 * @param managedFile an uploaded file, can be an image or not. When null, nothing is done.
	 * @param multipartFile the original uploaded file, to get the client filename. If null, the client filename is not changed.
	 * @param namePrefix prefix to attach before the original file name to make the target name. Add a separator (like a dash) if you need one. Can be null.
	 * @param targetExtension optional, to convert image file formats
	 * @param desktopWidth optional width for desktop images - when null, the image is not resized
	 * @param mobileWidth optional width for mobile images - when null, the mobile file is the same as the desktop
	 * @return
	 * @throws IOException
	 */
	private YadaAttachedFile attach(YadaAttachedFile yadaAttachedFile, File managedFile, MultipartFile multipartFile, String namePrefix, String targetExtension, Integer desktopWidth, Integer mobileWidth) throws IOException {
		//
		yadaAttachedFile.setUploadTimestamp(new Date());
		if (multipartFile!=null) {
			yadaAttachedFile.setClientFilename(multipartFile.getOriginalFilename());
		}
		String origExtension = yadaUtil.getFileExtension(yadaAttachedFile.getClientFilename());
		if (targetExtension==null) {
			targetExtension = origExtension;
		}
		YadaIntDimension dimension = yadaUtil.getImageDimension(managedFile);
		yadaAttachedFile.setImageDimension(dimension);
		boolean imageExtensionChanged = targetExtension.compareToIgnoreCase(origExtension)!=0;
		boolean requiresTransofmation = imageExtensionChanged || desktopWidth!=null || mobileWidth!=null;
		boolean needToDeleteOriginal =  config.isFileManagerDeletingUploads();
		//
		// If the file does not need resizing, there is just one default filename like "product-mydoc_2631.pdf"
		if (!requiresTransofmation) {
			File targetFile = yadaAttachedFile.calcAndSetTargetFile(namePrefix, targetExtension, null, YadaAttachedFile.YadaAttachedFileType.DEFAULT, config);
			// File targetFile = new File(targetFolder, targetFilenamePrefix + "." + targetExtension);
			if (needToDeleteOriginal) {
				// Just move the old file to the new destination
				Files.move(managedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} else {
				// Copy bytes
				try (InputStream inputStream = new FileInputStream(managedFile); OutputStream outputStream = new FileOutputStream(targetFile)) {
					IOUtils.copy(inputStream, outputStream);
				} catch (IOException e) {
					throw e;
				}
			}
		} else {
			// Transformation: copy with imagemagick
			// If desktopWidth is null, the image original size does not change.
			// The file name is like "product-mydoc_2631_640.jpg"
			File targetFile = yadaAttachedFile.calcAndSetTargetFile(namePrefix, targetExtension, desktopWidth, YadaAttachedFile.YadaAttachedFileType.DESKTOP, config);
			resizeAndConvertImageAsNeeded(managedFile, targetFile, desktopWidth);
			yadaAttachedFile.setFilename(targetFile.getName());
			if (mobileWidth==null) {
				yadaAttachedFile.setFilenameMobile(null); // No mobile image
			} else {
				targetFile = yadaAttachedFile.calcAndSetTargetFile(namePrefix, targetExtension, mobileWidth, YadaAttachedFile.YadaAttachedFileType.MOBILE, config);
				resizeAndConvertImageAsNeeded(managedFile, targetFile, mobileWidth);
			}
			if (needToDeleteOriginal) {
				log.debug("Deleting original file {}", managedFile.getAbsolutePath());
				managedFile.delete();
			}
		}
		return yadaAttachedFileRepository.save(yadaAttachedFile);
	}

	/**
	 * Perform image format conversion and/or resize, when needed
	 * @param sourceFile
	 * @param targetFile
	 * @param targetWidth resize width, can be null for no resize
	 */
	private void resizeAndConvertImageAsNeeded(File sourceFile, File targetFile, Integer targetWidth) {
		if (targetWidth==null) {
			// Convert only
			Map<String,String> params = new HashMap<>();
			params.put("FILENAMEIN", sourceFile.getAbsolutePath());
			params.put("FILENAMEOUT", targetFile.getAbsolutePath());
			boolean convert = yadaUtil.exec("config/shell/convert", params);
			if (!convert) {
				log.error("Image not copied when making attachment: {}", targetFile);
			}
		} else {
			// Resize
			Map<String,String> params = new HashMap<>();
			params.put("FILENAMEIN", sourceFile.getAbsolutePath());
			params.put("FILENAMEOUT", targetFile.getAbsolutePath());
			params.put("W", Integer.toString(targetWidth));
			params.put("H", ""); // the height must be empty to keep the original proportions and resize based on width
			boolean resized = yadaUtil.exec("config/shell/resize", params);
			if (!resized) {
				log.error("Image not resized when making attachment: {}", targetFile);
			}
		}
	}


}
