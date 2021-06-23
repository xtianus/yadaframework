package net.yadaframework.cms.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import net.yadaframework.cms.persistence.repository.YadaProductDao;

@Controller
@RequestMapping("/yadacms/product")
public class YadaCmsProductController {
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired private YadaProductDao yadaProductDao;


	@RequestMapping("/delete/galleryImage")
	public String deleteGalleryImage(Long productId, Long imageId) {
		// Remove the association between product and image
		yadaProductDao.removeGalleryImage(productId, imageId);
		// Delete the image
		// TODO devo usare il filemanager per cancellare il File
		// Forse è meglio se è il file manager stesso a cancellalre da db, in modo che ci sia un metodo unico generico per fare la cosa.
		return "todo";
		
	}

}
