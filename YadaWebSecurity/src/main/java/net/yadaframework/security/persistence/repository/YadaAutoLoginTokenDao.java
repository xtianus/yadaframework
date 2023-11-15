package net.yadaframework.security.persistence.repository;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;

@Repository
@Transactional(readOnly = true) 
public class YadaAutoLoginTokenDao {
	
	@PersistenceContext
	EntityManager em;
	
	/**
	 * Returns the list of objects associated with the YadaUserCredentials
	 * @param YadaUserCredentials
	 * @return
	 */
	public List<YadaAutoLoginToken> findByYadaUserCredentials(YadaUserCredentials yadaUserCredentials) {
		String sql = "from YadaAutoLoginToken e where e.yadaUserCredentials=:yadaUserCredentials and (e.expiration is null or e.expiration > NOW())";
		return em.createQuery(sql, YadaAutoLoginToken.class)
			.setParameter("yadaUserCredentials", yadaUserCredentials)
			.getResultList();
	}

	/**
	 * Returns the objects that match both id and token (should be no more than one I guess)
	 * @param id
	 * @param token
	 * @return
	 */
	public List<YadaAutoLoginToken> findByIdAndTokenOrderByTimestampDesc(long id, long token) {
		String sql = "from YadaAutoLoginToken e where e.id=:id and e.token=:token order by timestamp desc";
		return em.createQuery(sql, YadaAutoLoginToken.class)
			.setParameter("id", id)
			.setParameter("token", token)
			.getResultList();
	}
	
	/**
	 * Delete expired elements
	 */
    @Transactional(readOnly = false)
	public void deleteExpired() {
		String sql = "delete from YadaAutoLoginToken e where e.expiration is not null and e.expiration < NOW()";
		em.createQuery(sql).executeUpdate();
	}
    
	@Transactional(readOnly = false)
	public YadaAutoLoginToken save(YadaAutoLoginToken entity) {
		if (entity==null) {
			return null;
		}
		if (entity.getId()==null) {
			em.persist(entity);
			return entity;
		}
		return em.merge(entity);
	}

}
