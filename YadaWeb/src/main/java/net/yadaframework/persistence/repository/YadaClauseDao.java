package net.yadaframework.persistence.repository;


import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaClause;

/**
 * 
 */
@Repository
@Transactional(readOnly = true) 
public class YadaClauseDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
	EntityManager em;
    
	public List<YadaClause> findByName(String name) {
		String sql = "select yc from YadaClause yc where yc.name = :name";
		List<YadaClause> resultList = em.createQuery(sql, YadaClause.class)
			.setParameter("name", name)
			.getResultList();
		return resultList;
	}

	/**
	 * Ritorno la clausola con nome 'trattamentoDati' avente versione più alta
	 */
	// TODO: this must be moved to the application because it's not generic
	public YadaClause getTrattamentoDati() {
		String sql = "select * from YadaClause yc where yc.name='trattamentoDati' order by yc.clauseVersion desc limit 1";
		List<YadaClause> resultList = em.createNativeQuery(sql, YadaClause.class).getResultList();
		return normaliseSingleResult(resultList);
	}

	/**
	 * Ritorno la clausola con nome 'pubblicazioneSito' avente versione più alta
	 */
	// TODO: this must be moved to the application because it's not generic
	public YadaClause getPubblicazioneSito() {
		String sql = "select * from YadaClause yc where yc.name='pubblicazioneSito' order by yc.clauseVersion desc limit 1";
		List<YadaClause> resultList = em.createNativeQuery(sql, YadaClause.class).getResultList();
		return normaliseSingleResult(resultList);
	}
	
	// TODO: this must be moved to the application because it's not generic
	public YadaClause getPubblicazioneRaccolta() {
		String sql = "select * from YadaClause yc where yc.name='pubblicazioneRaccolta' order by yc.clauseVersion desc limit 1";
		List<YadaClause> resultList = em.createNativeQuery(sql, YadaClause.class).getResultList();
		return normaliseSingleResult(resultList);
	}

    /**
     * For backwards compatibility, returns null when no result is found
     * @param resultList
     * @return
     */
    private YadaClause normaliseSingleResult(List<YadaClause> resultList) {
		// Need to keep the contract of the Spring Data Repository, so we return null when no value found.
		if (resultList.isEmpty()) {
			return null;
		} else {
			return resultList.get(0);
		}
    }
    
    public long count() {
    	return em.createQuery("select count(*) from YadaClause", Long.class).getSingleResult().longValue();
    }

	@Transactional(readOnly = false)
	public YadaClause save(YadaClause entity) {
		if (entity==null) {
			return null;
		}
		if (entity.getId()==null) {
			em.persist(entity);
			return entity;
		}
		return em.merge(entity);
	}

	@Transactional(readOnly = false)
	public void saveAll(List<YadaClause> yadaClauses) {
		for (YadaClause yadaClause : yadaClauses) {
			save(yadaClause);
		}
	}
    

	
}
