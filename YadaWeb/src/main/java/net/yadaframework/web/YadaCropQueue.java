package net.yadaframework.web;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.yadaframework.components.YadaFileManager;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.entity.YadaManagedFile;
import net.yadaframework.persistence.repository.YadaAttachedFileDao;
import net.yadaframework.persistence.repository.YadaFileManagerDao;
import net.yadaframework.raw.YadaIntDimension;

/**
 * Kept in HttpSession, it stores all info needed to crop images.
 *
 */
public class YadaCropQueue {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	// Autowiring must be performed by yadaUtil.autowireAndInitialize()
	@Autowired private YadaUtil yadaUtil;
	@Autowired private YadaFileManagerDao yadaFileManagerDao;
	@Autowired private YadaAttachedFileDao yadaAttachedFileDao;
	@Autowired private YadaFileManager yadaFileManager;
	// private @Autowired YadaConfiguration config;


	private String cropPerformAction = "/yada/cropPerform"; // Where to go to execute the crop after the area has been chosen

	private Queue<YadaCropImage> cropImages; // Images to crop
	private String cropRedirect; // Where to go to show the crop page, e.g. "redirect:/en/cropPage"
	private String destinationRedirect; // Where to go after all the crop has been done, e.g. "redirect:/en/dashboard/finishProfile?id=13"

	public final static String SESSION_KEY = "net.yadaframework.web.YadaCropQueue";

	/**
	 * Package-accessible constructor used by YadaSession
	 * @param cropRedirect where to go to perform the crop, e.g. "/some/controller/cropPage"
	 * @param destinationRedirect where to go after all the crop has been done, e.g. "/some/controller/afterCrop"
	 */
	public YadaCropQueue(String cropRedirect, String destinationRedirect) {
		this.destinationRedirect = destinationRedirect;
		this.cropRedirect = cropRedirect;
		this.cropImages = new LinkedList<>();
	}

	/**
	 * Returns the next image that has to be cropped
	 * @return
	 */
	public YadaCropImage getCurrentImage() {
		return cropImages.peek();
	}

	/**
	 * Returns the current image to be cropped, removing it from the list
	 * @return
	 */
	public // Internal use only
	YadaCropImage getAndRemoveCurrentImage() {
		return cropImages.poll();
	}

//	/**
//	 * Returns the current image to be cropped
//	 * @return
//	 */
//	// package visible
//	YadaCropImage getCurrentImage() {
//		return cropImages.peek();
//	}

	/**
	 * Returns the current YadaCropQueue
	 * @return
	 */
	@SuppressWarnings("unused")
	private Queue<YadaCropImage> getCropImages() {
		return this.cropImages;
	}

	/**
	 * Delete any images in the queue, aborting the upload of any remaining pictures.
	 */
	public void delete() {
		for (YadaCropImage yadaCropImage : cropImages) {
			yadaFileManagerDao.delete(yadaCropImage.getImageToCrop());
			// Se lo YadaAttachedFile non è stato ancora usato, lo cancello, altrimenti
			// lo lascio perché contiene le immagini precedenti
			YadaAttachedFile yadaAttachedFile = yadaCropImage.getYadaAttachedFile();
			if (yadaAttachedFile.getId()!=null && yadaAttachedFile.getFilename()==null && yadaAttachedFile.getFilenameDesktop()==null && yadaAttachedFile.getFilenameMobile()==null) {
				yadaAttachedFileDao.delete(yadaAttachedFile.getId());
				//	Non serve sganciarli dall'entity purché tale entity non venga usata prima di un reload, cosa improbabile a meno che non ci sia una cache.
			}
		}
		cropImages.clear();
	}

	/**
	 * Add an image to be cropped
	 * @param imageToCrop image to crop
	 * @param targetDimensions desktop and mobile target dimensions, use null when a crop is not needed for some dimension
	 * @param targetRelativeFolder Folder where to place cropped images, relative to the contents folder
	 * @param targetNamePrefix Prefix to use for the target file, can be empty or null
	 * @throws IOException 
	 */
	public YadaCropImage addCropImage(YadaManagedFile imageToCrop, YadaIntDimension[] targetDimensions, String targetRelativeFolder, String targetNamePrefix) {
		if (this.cropImages==null) {
			throw new YadaInvalidUsageException("Please call YadaSession.addCropQueue() before adding images to the YadaCropQueue instance");
		}
		if (targetDimensions == null || targetDimensions.length<3) {
			throw new YadaInvalidUsageException("Please call YadaCropQueue.addCropImage() with a targetDimensions array of three elements that can be null");
		}
		YadaCropImage yadaCropImage = new YadaCropImage(yadaFileManager, imageToCrop, targetDimensions, targetRelativeFolder, targetNamePrefix);
		yadaCropImage = (YadaCropImage) yadaUtil.autowireAndInitialize(yadaCropImage);
		this.cropImages.add(yadaCropImage);
		return yadaCropImage;
	}

	/**
	 * Check if there are any images to be cropped
	 * @return
	 */
	public boolean hasCropImages() {
		return this.cropImages!=null && !this.cropImages.isEmpty();
	}

	/**
	 * Count the images left to be cropped
	 * @return
	 */
	public int getCount() {
		return cropImages!=null?cropImages.size() : 0;
	}

	/**
	 * Returns the url to open when the crop is terminated on all images
	 * @return
	 */
	public String getDestinationRedirect() {
		return this.destinationRedirect;
	}

	@SuppressWarnings("unused")
	private void setDestinationRedirect(String destinationRedirect) {
		this.destinationRedirect = destinationRedirect;
	}

	public String getCropPerformAction() {
		return cropPerformAction;
	}

	@SuppressWarnings("unused")
	private void setCropPerformAction(String cropPerformAction) {
		this.cropPerformAction = cropPerformAction;
	}

	public String getCropRedirect() {
		return cropRedirect;
	}

	@SuppressWarnings("unused")
	private void setCropRedirect(String cropRedirect) {
		this.cropRedirect = cropRedirect;
	}

}
