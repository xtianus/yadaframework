package net.yadaframework.commerce.persistence.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.commerce.persistence.entity.YadaTransaction;

@Repository
@Transactional(readOnly = true)
public class YadaTransactionDao {

    @PersistenceContext private EntityManager em;

    @Transactional(readOnly = false)
    public YadaTransaction save(YadaTransaction entity) {
    	if (entity.getId()==null) {
    		em.persist(entity);
    	} else {
    		entity = em.merge(entity);
    	}
    	return entity;
    }

}
