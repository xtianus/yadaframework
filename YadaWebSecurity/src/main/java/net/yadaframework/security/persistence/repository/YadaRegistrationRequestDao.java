package net.yadaframework.security.persistence.repository;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;

@Repository
@Transactional(readOnly = true) 
public class YadaRegistrationRequestDao {
	
	@PersistenceContext EntityManager em;

	public <R extends YadaRegistrationRequest> List<R> findByIdAndTokenOrderByTimestampDesc(long id, long token, Class<R> type) {
		String sql = "from " + type.getSimpleName() + " where id=:id and token=:token order by timestamp desc";
		return em.createQuery(sql, type)
			.setParameter("id", id)
			.setParameter("token", token)
			.getResultList();
	}

	public <R extends YadaRegistrationRequest> List<R> findByEmailAndRegistrationType(String email, YadaRegistrationType registrationType, Class<R> type) {
		String sql = "from " + type.getSimpleName() + " where email=:email and registrationType=:registrationType";
		return em.createQuery(sql, type)
			.setParameter("email", email)
			.setParameter("registrationType", registrationType)
			.getResultList();
	}

	/**
	 * Trova tutti gli elementi con timestamp antecedente la data indicata
	 * @param upperLimit
	 * @return
	 */
	public List<YadaRegistrationRequest> findByTimestampBefore(Date upperLimit) {
		String sql = "from YadaRegistrationRequest where timestamp < :upperLimit";
		return em.createQuery(sql, YadaRegistrationRequest.class)
			.setParameter("upperLimit", upperLimit)
			.getResultList();
	}

	@Transactional(readOnly = false)
	public void delete(YadaRegistrationRequest registrationRequest) {
		registrationRequest = em.merge(registrationRequest);
		em.remove(registrationRequest);
	}

	@Transactional(readOnly = false)
	public YadaRegistrationRequest save(YadaRegistrationRequest entity) {
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
