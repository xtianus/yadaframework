package net.yadaframework.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaAttachedFile;

/**
 *
 */
@Repository
@Transactional(readOnly = true)
public class YadaAttachedFileDao {

    @PersistenceContext
	private EntityManager em;

    /**
     * Delete a YadaAttachedFile and connected data, but not the file on disk. If there is a relationship between an Entity and the YadaAttachedFile,
     * deletion fails because of the foreign key. The relationship should be removed before calling this method.
     * @param yadaAttachedFileId
     */
    @Transactional(readOnly = false)
    public void delete(long yadaAttachedFileId) {
    	YadaSql.instance().deleteFrom("delete from YadaAttachedFile_title")
    		.where("YadaAttachedFile_id = :id")
    		.setParameter("id", yadaAttachedFileId)
    		.nativeQuery(em).executeUpdate();
    	YadaSql.instance().deleteFrom("delete from YadaAttachedFile_description")
	    	.where("YadaAttachedFile_id = :id")
	    	.setParameter("id", yadaAttachedFileId)
	    	.nativeQuery(em).executeUpdate();
    	// If the YadaAttachedFile has been attached to an entity that has been saved, there's no way of removing that relationship
    	// because we don't know which entity so we just ignore the deletion and keep the row
    	YadaSql.instance().deleteFrom("delete ignore from YadaAttachedFile")
	    	.where("id = :id")
	    	.setParameter("id", yadaAttachedFileId)
	    	.nativeQuery(em).executeUpdate();
    }

    /**
     * Deletes a YadaAttachedFile when not null
     * @param yadaAttachedFile
     */
    @Transactional(readOnly = false)
    public void delete(YadaAttachedFile yadaAttachedFile) {
    	if (yadaAttachedFile!=null && yadaAttachedFile.getId()!=null) {
    		delete(yadaAttachedFile.getId());
    	}
    }

	/**
	 * Swaps the sortOrder of two YadaAttachedFile entities
	 * @param oneId
	 * @param anotherId
	 */
	@Transactional(readOnly = false)
	public void swapSortOrder(long oneId, long anotherId) {
		em.createNativeQuery("update YadaAttachedFile yaf inner join YadaAttachedFile yaf2 on yaf.id != yaf2.id "
			+ "set yaf.sortOrder = yaf2.sortOrder, yaf2.sortOrder = yaf.sortOrder "
			+ "where yaf.id in (:oneId,:anotherId) and yaf2.id in (:oneId,:anotherId);")
		.setParameter("oneId", oneId)
		.setParameter("anotherId", anotherId)
		.executeUpdate();
	}

	@Transactional(readOnly = false)
	public YadaAttachedFile save(YadaAttachedFile yadaAttachedFile) {
		if (yadaAttachedFile==null) {
			return null;
		}
		if (yadaAttachedFile.getId()==null) {
			em.persist(yadaAttachedFile);
			return yadaAttachedFile;
		}
		return em.merge(yadaAttachedFile);
	}

	/**
	 * Adds a yadaAttachedFile to a list that could be lazy
	 * @param yadaAttachedFile
	 * @param list
	 */
	@Transactional(readOnly = false)
	public void addTo(Object entity, List<YadaAttachedFile> list, YadaAttachedFile yadaAttachedFile) {
		yadaAttachedFile = em.merge(yadaAttachedFile);
		entity = em.merge(entity);
		list.add(yadaAttachedFile);
	}

	/**
	 * Find entity from the id
	 * @param yadaAttachedFileId
	 * @return the found entity instance or null if the entity doesnot exist
	 */
	public YadaAttachedFile find(Long yadaAttachedFileId) {
		return em.find(YadaAttachedFile.class, yadaAttachedFileId);
	}

	// Kept for compatibility with Spring Data Repository
	@Deprecated // it is a Spring Data api
	public Optional<YadaAttachedFile> findById(Long yadaAttachedFileId) {
		YadaAttachedFile result = em.find(YadaAttachedFile.class, yadaAttachedFileId);
		return  Optional.ofNullable(result);
	}

}