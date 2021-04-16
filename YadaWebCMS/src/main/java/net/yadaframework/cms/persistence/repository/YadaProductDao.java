package net.yadaframework.cms.persistence.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true) 
public class YadaProductDao {
	
    @PersistenceContext EntityManager em;

	/**
	 * Remove the association of a gallery image from a product
	 * @param productId
	 * @param yadaAttachedFileId
	 */
	@Transactional(readOnly = false) 
	public void removeGalleryImage(Long productId, Long yadaAttachedFileId) {
		String sql = "delete from YadaProduct_galleryImages where YadaProduct_id = :yadaProductId and galleryImages_id = :yadaAttachedFileId";
		em.createNativeQuery(sql)
			.setParameter("yadaProductId", productId)
			.setParameter("yadaAttachedFileId", yadaAttachedFileId)
			.executeUpdate();
	}
	
	/**
	 * Adds a new gallery image attachment to a product
	 * @param productId
	 * @param yadaAttachedFileId
	 */
	@Transactional(readOnly = false) 
	public void addGalleryImage(Long productId, Long yadaAttachedFileId) {
		String sql = "insert into YadaProduct_galleryImages (YadaProduct_id, galleryImages_id) values (:yadaProductId, :yadaAttachedFileId)";
		em.createNativeQuery(sql)
			.setParameter("yadaProductId", productId)
			.setParameter("yadaAttachedFileId", yadaAttachedFileId)
			.executeUpdate();
	}
	
	/**
	 * Update a new thumbnail image attachment to a product
	 * @param productId
	 * @param yadaAttachedFileId
	 */
	@Transactional(readOnly = false) 
	public void setThumbnailImage(Long productId, Long yadaAttachedFileId) {
		String sql = "UPDATE YadaProduct SET image_id=:yadaAttachedFileId where id=:yadaProductId";
		em.createNativeQuery(sql)
			.setParameter("yadaProductId", productId)
			.setParameter("yadaAttachedFileId", yadaAttachedFileId)
			.executeUpdate();
	}
}
