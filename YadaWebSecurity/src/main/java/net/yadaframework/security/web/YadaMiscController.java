package net.yadaframework.security.web;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.components.YadaFileManager;
import net.yadaframework.components.YadaNotify;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.entity.YadaAttachedFile.YadaAttachedFileType;
import net.yadaframework.persistence.entity.YadaManagedFile;
import net.yadaframework.persistence.repository.YadaAttachedFileDao;
import net.yadaframework.raw.YadaIntDimension;
import net.yadaframework.web.YadaCropImage;
import net.yadaframework.web.YadaCropQueue;

/**
 * Miscellaneous @RequestMapping methods
 */
@Controller
public class YadaMiscController {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaSession<?> yadaSession;
	@Autowired private YadaUtil yadaUtil;
	@Autowired private YadaNotify yadaNotify;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaAttachedFileDao yadaAttachedFileDao;
	@Autowired private YadaFileManager yadaFileManager;

	@RequestMapping(value="/yada/cropPerform", params={"!submit", "cancel"})
	// @RequestParam Map<String,String> allParams
	public String cropCancel(YadaCropDefinition yadaCropDefinition, Model model, RedirectAttributes redirectAttributes) {
		YadaCropQueue cropQueue = yadaSession.getCropQueue();
		if (cropQueue==null) {
			throw new YadaInternalException("No data in session when cancelling image crop");
		}
		// Skip the crop of all remaining images
		String destination = cropQueue.getDestinationRedirect(); // Done
		yadaSession.deleteCropQueue();
		return destination;
	}

	@RequestMapping("/yada/cropPerform")
	public String cropPerform(YadaCropDefinition yadaCropDefinition, Model model, RedirectAttributes redirectAttributes) {
		YadaCropQueue cropQueue = yadaSession.getCropQueue();
		if (cropQueue==null) {
			throw new YadaInternalException("No data in session when performing image crop");
		}
		YadaCropImage yadaCropImage = cropQueue.getAndRemoveCurrentImage();
		boolean failed = true;
		if (yadaCropImage!=null) {
			failed = false;
			try {
				YadaAttachedFile yadaAttachedFile = yadaCropImage.getYadaAttachedFile();
				if (yadaCropImage.isCropDesktop()) {
					// Desktop
					File createdFile = cropAndResizeImage(yadaCropImage, yadaCropDefinition.getDesktopCrop(), yadaCropImage.getTargetDesktopDimension(), YadaAttachedFileType.DESKTOP);
					if (!yadaCropImage.isCropMobile()) {
						// Default is like desktop when there is no mobile
						yadaAttachedFile.setFilename(createdFile.getName());
					}
				}
				if (yadaCropImage.isCropMobile()) {
					// Mobile
					File createdFile = cropAndResizeImage(yadaCropImage, yadaCropDefinition.getMobileCrop(), yadaCropImage.getTargetMobileDimension(), YadaAttachedFileType.MOBILE);
				}
				if (yadaCropImage.isCropPdf()) {
					// pdf
					File createdFile = cropAndResizeImage(yadaCropImage, yadaCropDefinition.getPdfCrop(), yadaCropImage.getTargetPdfDimension(), YadaAttachedFileType.PDF);
				}
				try {
					yadaAttachedFile = yadaAttachedFileDao.save(yadaAttachedFile);
				} catch (OptimisticLockException e) {
					throw new YadaInvalidUsageException("Concurrent modification on yadaAttachedFile. This happens if you set 'cascade=CascadeType.ALL' on the owning entity or if the yadaAttachedFile is merged after setting it on YadaCropImage", e);
				}
			} catch (IOException e) {
				// Failed to crop
				failed = true;
			}
			// Delete original file when temporary
			YadaManagedFile yadaManagedFile = yadaCropImage.getImageToCrop();
			if (yadaManagedFile.isExpired()) {
				yadaFileManager.delete(yadaManagedFile);
			}
		}
		//
		if (cropQueue.hasCropImages()) {
			if (failed) {
				yadaNotify.titleKey(model, "yada.crop.error.title").error().messageKey("yada.crop.error.message").add();
			}
			return cropQueue.getCropRedirect(); // Crop more images
		} else {
			if (failed) {
				yadaNotify.titleKey(redirectAttributes, "yada.crop.error.title").error().messageKey("yada.crop.error.message").add();
			}
			String destination = cropQueue.getDestinationRedirect(); // Done
			yadaSession.deleteCropQueue();
			return destination;
		}
	}

	private File cropAndResizeImage(YadaCropImage yadaCropImage, Map<String,String> cropDefinition, YadaIntDimension targetDimension, YadaAttachedFileType type) throws IOException {
		if (cropDefinition.isEmpty()) {
			throw new YadaInvalidUsageException("Empty crop definition");
		}
		YadaManagedFile sourceManagedFile = yadaCropImage.getImageToCrop();
		File imageToCropFile = sourceManagedFile.getAbsoluteFile();
		YadaAttachedFile yadaAttachedFile = yadaCropImage.getYadaAttachedFile();
		String sourceExtension = sourceManagedFile.getFileExtension();
		String targetExtension = config.getTargetImageExtension(); // "jpg"
		// Check if the source extension has to be preserved (e.g. for gif)
		if (config.isPreserveImageExtension(sourceExtension)) {
			targetExtension = sourceExtension;
		}
		// Always set a new target file to prevent cache issues
		File destinationFile = yadaAttachedFile.getAbsoluteFile(type);
		if (destinationFile!=null) {
			destinationFile.delete();
		}
		destinationFile = yadaAttachedFile.calcAndSetTargetFile(yadaCropImage.getTargetNamePrefix(), targetExtension, type, targetDimension);
		destinationFile.getParentFile().mkdirs(); // Ensure the target folder exists
		Map<String, String> params = new HashMap<>();
		params.put("FILENAMEIN", imageToCropFile.getAbsolutePath());
		params.put("FILENAMEOUT", destinationFile.getAbsolutePath());
		params.put("x", cropDefinition.get("x"));
		params.put("y", cropDefinition.get("y"));
		params.put("w", cropDefinition.get("w"));
		params.put("h", cropDefinition.get("h"));
		params.put("resizew", Integer.toString(targetDimension.getWidth()));
		params.put("resizeh", Integer.toString(targetDimension.getHeight()));
		int exitValue = yadaUtil.shellExec("config/shell/yadaCropAndResize", params, null);
		int newWidth = targetDimension.getWidth();
		int newHeight = targetDimension.getHeight();
		if (type == YadaAttachedFileType.DESKTOP) {
			yadaAttachedFile.setDesktopImageDimension(new YadaIntDimension(newWidth, newHeight));
		} else if (type == YadaAttachedFileType.MOBILE) {
			yadaAttachedFile.setMobileImageDimension(new YadaIntDimension(newWidth, newHeight));
		} else if (type == YadaAttachedFileType.PDF) {
			yadaAttachedFile.setPdfImageDimension(new YadaIntDimension(newWidth, newHeight));
		} else {
			yadaAttachedFile.setImageDimension(new YadaIntDimension(newWidth, newHeight));
		}
		return destinationFile;
	}


}
