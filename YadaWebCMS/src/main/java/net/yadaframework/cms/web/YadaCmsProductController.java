package net.yadaframework.cms.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import net.yadaframework.cms.persistence.entity.YadaProduct;
import net.yadaframework.cms.persistence.repository.YadaProductRepository;

@Controller
@RequestMapping("/yadacms/product")
public class YadaCmsProductController {
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired private YadaProductRepository<YadaProduct> yadaProductRepository;


	@RequestMapping("/delete/galleryImage")
	public String deleteGalleryImage(Long productId, Long imageId) {
		// Remove the association between product and image
		yadaProductRepository.removeGalleryImage(productId, imageId);
		// Delete the image
		// TODO devo usare il filemanager per cancellare il File
		// Forse è meglio se è il file manager stesso a cancellalre da db, in modo che ci sia un metodo unico generico per fare la cosa.
		return "todo";
		
	}

}
