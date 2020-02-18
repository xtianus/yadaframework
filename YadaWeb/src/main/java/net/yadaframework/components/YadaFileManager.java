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

	// Image to return when no image is available
	public final static String NOIMAGE_DATA="data:image/jpeg;base64,/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAA6AAD/4QMxaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjYtYzEzOCA3OS4xNTk4MjQsIDIwMTYvMDkvMTQtMDE6MDk6MDEgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDQyAyMDE3IChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkM0MzIwODI4RDk5ODExRTc5NkJEQUU4MkU3NzAwMUNEIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkM0MzIwODI5RDk5ODExRTc5NkJEQUU4MkU3NzAwMUNEIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6RDQyMjJGN0ZEOTBBMTFFNzk2QkRBRTgyRTc3MDAxQ0QiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6RDQyMjJGODBEOTBBMTFFNzk2QkRBRTgyRTc3MDAxQ0QiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz7/7gAhQWRvYmUAZMAAAAABAwAQAwMGCQAAJ/IAADkCAABIi//bAIQABwUFBQUFBwUFBwoGBQYKCwgHBwgLDQsLCwsLDREMDAwMDAwRDQ8QERAPDRQUFhYUFB0dHR0dICAgICAgICAgIAEHCAgNDA0ZEREZHBYSFhwgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAg/8IAEQgDwAPAAwERAAIRAQMRAf/EANQAAQADAQEBAQEAAAAAAAAAAAAFBgcEAwIBCAEBAAAAAAAAAAAAAAAAAAAAABAAAQQBAwQCAgICAgMAAAAAAwECBAUAEzQVECAzFBESYDJQITEiMCWQwNARAAECAgUICAQFBAIDAQAAAAECAwARECExcRIgQbHRIjJyM1GBkcHhQqITYaGSBGBSgiNzMFCyFGJDkMDw8RIBAAAAAAAAAAAAAAAAAAAA0BMBAAEBBgYCAwEBAAMBAAAAAREAEPAhMUFRIGGBkaGxccEwYNFQ4ZDA0PH/2gAMAwEAAhEDEQAAAP6RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA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AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA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";

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
	 * Returns the absolute path of the pdf file
	 * @param yadaAttachedFile the attachment
	 * @return the File or null
	 */
	public File getAbsolutePdfFile(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile!=null) {
			return getAbsoluteFile(yadaAttachedFile, yadaAttachedFile.getFilenamePdf());
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
		if (yadaAttachedFile.getFilenamePdf() != null) {
			getAbsoluteFile(yadaAttachedFile, yadaAttachedFile.getFilenamePdf()).delete();
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
			return NOIMAGE_DATA;
		}
		String imageName = yadaAttachedFile.getFilenameMobile();
		if (imageName==null) {
			return NOIMAGE_DATA;
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
			return NOIMAGE_DATA;
		}
		String imageName = yadaAttachedFile.getFilenameDesktop();
		if (imageName==null) {
			return getFileUrl(yadaAttachedFile);
		}
		return computeUrl(yadaAttachedFile, imageName);
	}

	/**
	 * Returns the (relative) url of the pdf image if any, or null.
	 * @param yadaAttachedFile
	 * @return
	 */
	public String getPdfImageUrl(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile==null) {
			return NOIMAGE_DATA;
		}
		String imageName = yadaAttachedFile.getFilenamePdf();
		if (imageName==null) {
			return NOIMAGE_DATA;
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
	 * @param currentAttachedFile an existing attachment, never null
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
	 * @param currentAttachedFile an existing attachment, never null
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
		String clientFilename = null;
		if (multipartFile!=null) {
			clientFilename = multipartFile.getOriginalFilename();
		}
		return attach(currentAttachedFile, managedFile, clientFilename, namePrefix, targetExtension, desktopWidth, mobileWidth);
	}

	/**
	 * Copies a managed file to the destination folder, creating a database association to assign to an Entity.
	 * The name of the file is in the format [basename]managedFileName_id.ext.
	 * Images are not resized.
	 * @param managedFile an uploaded file, can be an image or not
	 * @param multipartFile the original uploaded file, to get the client filename. If null, the client filename is not set.
	 * @param relativeFolderPath path of the target folder relative to the contents folder
	 * @param namePrefix prefix to attach before the original file name. Add a separator if you need one. Can be null.
	 * @return YadaAttachedFile if the file is uploaded, null if no file was sent by the user
	 * @throws IOException
	 * @see {@link #attach(File, String, String, String, Integer, Integer)}
	 */
	public YadaAttachedFile attachNew(File managedFile, MultipartFile multipartFile, String relativeFolderPath, String namePrefix) throws IOException {
		return attachNew(managedFile, multipartFile, relativeFolderPath, namePrefix, null, null, null);
	}

	/**
	 * Copies (and resizes) a managed file to the destination folder, creating a database association to assign to an Entity.
	 * The name of the file is in the format [basename]managedFileName_id.ext
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
	public YadaAttachedFile attachNew(File managedFile, MultipartFile multipartFile, String relativeFolderPath, String namePrefix, String targetExtension, Integer desktopWidth, Integer mobileWidth) throws IOException {
		String clientFilename = null;
		if (multipartFile!=null) {
			clientFilename = multipartFile.getOriginalFilename();
		}
		return attachNew(managedFile, clientFilename, relativeFolderPath, namePrefix, targetExtension, desktopWidth, mobileWidth);
	}

	/**
	 * Copies (and resizes) a managed file to the destination folder, creating a database association to assign to an Entity.
	 * The name of the file is in the format [basename]managedFileName_id.ext
	 * @param managedFile an uploaded file, can be an image or not. When null, nothing is done.
	 * @param clientFilename the original client filename. If null, the client filename is not changed.
	 * @param relativeFolderPath path of the target folder relative to the contents folder, starting with a slash /
	 * @param namePrefix prefix to attach before the original file name. Add a separator if you need one. Can be null.
	 * @param targetExtension optional, to convert image file formats
	 * @param desktopWidth optional width for desktop images - when null, the image is not resized
	 * @param mobileWidth optional width for mobile images - when null, the mobile file is the same as the desktop
	 * @return YadaAttachedFile if the file is uploaded, null if no file was sent by the user
	 * @throws IOException
	 * @see {@link #attach(File, String, String, String)}
	 */
	public YadaAttachedFile attachNew(File managedFile, String clientFilename, String relativeFolderPath, String namePrefix, String targetExtension, Integer desktopWidth, Integer mobileWidth) throws IOException {
		if (managedFile==null) {
			return null;
		}
		if (!relativeFolderPath.startsWith("/") && !relativeFolderPath.startsWith("\\")) {
			relativeFolderPath = "/" + relativeFolderPath;
			log.warn("The relativeFolderPath '{}' should have a leading slash (fixed)", relativeFolderPath);
		}
		YadaAttachedFile yadaAttachedFile = new YadaAttachedFile();
		// yadaAttachedFile.setAttachedToId(attachToId);
		yadaAttachedFile.setRelativeFolderPath(relativeFolderPath);
		// This save should not bee needed anymore because of @PostPersist in YadaAttachedFile
		yadaAttachedFile = yadaAttachedFileRepository.save(yadaAttachedFile); // Get the id
		File targetFolder = new File(config.getContentPath(), relativeFolderPath);
		targetFolder.mkdirs();
		return attach(yadaAttachedFile, managedFile, clientFilename, namePrefix, targetExtension, desktopWidth, mobileWidth);
	}

	/**
	 * Performs file copy and (for images) resize to different versions
	 * @param yadaAttachedFile object to fill with values
	 * @param managedFile an uploaded file, can be an image or not. When null, nothing is done.
	 * @param clientFilename the client filename. If null, the client filename is not changed.
	 * @param namePrefix prefix to attach before the original file name to make the target name. Add a separator (like a dash) if you need one. Can be null.
	 * @param targetExtension optional, to convert image file formats
	 * @param desktopWidth optional width for desktop images - when null, the image is not resized
	 * @param mobileWidth optional width for mobile images - when null, the mobile file is the same as the desktop
	 * @return
	 * @throws IOException
	 */
	private YadaAttachedFile attach(YadaAttachedFile yadaAttachedFile, File managedFile, String clientFilename, String namePrefix, String targetExtension, Integer desktopWidth, Integer mobileWidth) throws IOException {
		//
		yadaAttachedFile.setUploadTimestamp(new Date());
		if (clientFilename!=null) {
			yadaAttachedFile.setClientFilename(clientFilename);
		}
		String origExtension = yadaUtil.getFileExtension(yadaAttachedFile.getClientFilename());
		if (targetExtension==null) {
			targetExtension = origExtension;
		}
		YadaIntDimension dimension = yadaUtil.getImageDimension(managedFile);
		yadaAttachedFile.setImageDimension(dimension);
		boolean imageExtensionChanged = origExtension==null || targetExtension.compareToIgnoreCase(origExtension)!=0;
		boolean requiresTransofmation = imageExtensionChanged || desktopWidth!=null || mobileWidth!=null;
		boolean needToDeleteOriginal =  config.isFileManagerDeletingUploads();
		//
		// If the file does not need resizing, there is just one default filename like "product-mydoc_2631.pdf"
		if (!requiresTransofmation) {
			File targetFile = yadaAttachedFile.calcAndSetTargetFile(namePrefix, targetExtension, null, YadaAttachedFile.YadaAttachedFileType.DEFAULT);
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
			File targetFile = yadaAttachedFile.calcAndSetTargetFile(namePrefix, targetExtension, desktopWidth, YadaAttachedFile.YadaAttachedFileType.DESKTOP);
			resizeAndConvertImageAsNeeded(managedFile, targetFile, desktopWidth);
			yadaAttachedFile.setFilename(targetFile.getName());
			if (mobileWidth==null) {
				yadaAttachedFile.setFilenameMobile(null); // No mobile image
			} else {
				targetFile = yadaAttachedFile.calcAndSetTargetFile(namePrefix, targetExtension, mobileWidth, YadaAttachedFile.YadaAttachedFileType.MOBILE);
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
