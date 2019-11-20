package net.yadaframework.persistence.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaAttachedFile;

/**
 *
 */
@Repository
@Transactional(readOnly = true)
public class YadaAttachedFileDao {

    @PersistenceContext private EntityManager em;

    /**
     * Delete a YadaAttachedFile using fast native queries
     * @param yadaAttachedFileId
     */
    @Transactional(readOnly = false)
    public void delete(long yadaAttachedFileId) {
    	YadaSql.instance().deleteFrom("delete from YadaAttachedFile_title y")
    		.where("y.YadaAttachedFile_id = :id")
    		.setParameter("id", yadaAttachedFileId)
    		.nativeQuery(em).executeUpdate();
    	YadaSql.instance().deleteFrom("delete from YadaAttachedFile_description y")
	    	.where("y.YadaAttachedFile_id = :id")
	    	.setParameter("id", yadaAttachedFileId)
	    	.nativeQuery(em).executeUpdate();
    	YadaSql.instance().deleteFrom("delete from YadaAttachedFile y")
	    	.where("y.id = :id")
	    	.setParameter("id", yadaAttachedFileId)
	    	.nativeQuery(em).executeUpdate();
    }

    /**
     * Deletes a YadaAttachedFile after merging it (slower)
     * @param yadaAttachedFile
     */
    @Transactional(readOnly = false)
    public void delete(YadaAttachedFile yadaAttachedFile) {
    	em.merge(yadaAttachedFile);
    	em.remove(yadaAttachedFile);
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

}