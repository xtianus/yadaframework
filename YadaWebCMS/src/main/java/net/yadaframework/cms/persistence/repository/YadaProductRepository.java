package net.yadaframework.cms.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.cms.persistence.entity.YadaProduct;

@Transactional(readOnly = true) 
public interface YadaProductRepository<T extends YadaProduct> extends JpaRepository<T, Long> {

	/**
	 * Remove the association of a gallery image from a product
	 * @param productId
	 * @param yadaAttachedFileId
	 */
	@Transactional(readOnly = false) 
	@Modifying
	@Query(value="delete from YadaProduct_galleryImages where YadaProduct_id = :yadaProductId and galleryImages_id = :yadaAttachedFileId", nativeQuery=true)
	void removeGalleryImage(@Param("yadaProductId") Long productId, @Param("yadaAttachedFileId") Long yadaAttachedFileId);
	
	/**
	 * Adds a new gallery image attachment to a product
	 * @param productId
	 * @param yadaAttachedFileId
	 */
	@Transactional(readOnly = false) 
	@Modifying
	@Query(value="insert into YadaProduct_galleryImages (YadaProduct_id, galleryImages_id) values (:yadaProductId, :yadaAttachedFileId)", nativeQuery=true)
	void addGalleryImage(@Param("yadaProductId") Long productId, @Param("yadaAttachedFileId") Long yadaAttachedFileId);
	
	/**
	 * Update a new thumbnail image attachment to a product
	 * @param productId
	 * @param yadaAttachedFileId
	 */
	@Transactional(readOnly = false) 
	@Modifying
	@Query(value="UPDATE YadaProduct SET image_id=:yadaAttachedFileId where id=:yadaProductId", nativeQuery=true)
	void setThumbnailImage(@Param("yadaProductId") Long productId, @Param("yadaAttachedFileId") Long yadaAttachedFileId);
}
