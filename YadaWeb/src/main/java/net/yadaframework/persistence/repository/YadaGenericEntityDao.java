package net.yadaframework.persistence.repository;


import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Non attualmente usato, serve a memorizzare su db un Entity qualunque
 */
@Repository
@Transactional(readOnly = true) 
public class YadaGenericEntityDao {

    @PersistenceContext EntityManager em;
    
    public Object getEntity(long id, Class type) {
    	return em.find(type, id);
    }
	
}
