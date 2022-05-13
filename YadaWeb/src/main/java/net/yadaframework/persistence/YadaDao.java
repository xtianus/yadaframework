package net.yadaframework.persistence;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidValueException;

/**
 * Generic database methods
 *
 */
@Repository
@Transactional(readOnly = true) 
public class YadaDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext EntityManager em;
    
    /**
     * Save any Entity, also new or detached ones.
     * @param entity
     * @return the saved Entity
     */
    @Transactional(readOnly = false)
	public Object save(Object entity) {
    	Long id = (Long) getEntityAttributeValue(entity, "id");
		if (id==null) {
			em.persist(entity);
			return entity;
		} else {
			return em.merge(entity);
		}
	}
   
	/**
	 * Get the value of some field on any entity, within a transaction.
	 * @param entity
	 * @param attributeName
	 * @return
	 */
    public Object getEntityAttributeValue(Object entity, String attributeName) {
    	entity = em.merge(entity);
    	try {
			return PropertyUtils.getSimpleProperty(entity, attributeName);
		} catch (Exception e) {
			throw new YadaInternalException("Can't get field {} of class {}", attributeName, entity.getClass(), e);
		}
    }
    
    /**
     * Find any object given its class and id
     * @param <T>
     * @param someClass
     * @param id
     * @return
     */
    public <T> T find (Class<T> someClass, Long id) {
    	return em.find(someClass, id);
    }
	
    /**
     * Find any object given its class name and id
     * @param className
     * @param id
     * @return
     */
    // TODO not tested
    public Object find(String className, Long id) {
		try {
			Class<?> theClass = Class.forName(className);
			return em.find(theClass, id);
		} catch (ClassNotFoundException e) {
			throw new YadaInvalidValueException("Class {} not found", className, e);
		}
    }

}
