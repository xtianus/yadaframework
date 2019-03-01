package net.yadaframework.cms.persistence.repository;


import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Miscellaneous CMS-related methods 
 */
@Repository
@Transactional(readOnly = true) 
public class YadaWebCmsDao {
	// private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext EntityManager em;

    /**
     * Swaps the attributes of two entities. For example for swapping positions.
     * @param entityClass the class of the @Entity
     * @param attributeName the attribute to swap, e.g. "pos"
     * @param oneId the id of one entity
     * @param anotherId the id of another entity
     */
    @Transactional(readOnly = false) 
    public void swapAttributes(Class<?> entityClass, String attributeName, Long oneId, Long anotherId) {
    	String className = entityClass.getSimpleName();
    	String query = "update "+className+" theEntity1 inner join "+className+" theEntity2 on theEntity1.id != theEntity2.id"
    		// Switching the two values
    		+ " set theEntity1."+attributeName+" = theEntity2."+attributeName+", theEntity2."+attributeName+" = theEntity1."+attributeName
    		//
    		+ " where theEntity1.id in (:oneId,:anotherId) and theEntity2.id in (:oneId,:anotherId)";
    	em.createNativeQuery(query).setParameter("oneId", oneId).setParameter("anotherId", anotherId).executeUpdate();
    }
    
}