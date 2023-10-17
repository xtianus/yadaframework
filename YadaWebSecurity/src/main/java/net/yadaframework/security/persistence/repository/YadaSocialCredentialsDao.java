package net.yadaframework.security.persistence.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaSocialCredentials;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;

@Repository
@Transactional(readOnly = true) 
public class YadaSocialCredentialsDao {
	
	@PersistenceContext
	EntityManager em;

	public List<YadaSocialCredentials> findBySocialIdAndType(String socialId, int type) {
		String sql = "from YadaSocialCredentials where socialId=:socialId and type=:type";
		return em.createQuery(sql, YadaSocialCredentials.class)
			.setParameter("socialId", socialId)
			.setParameter("type", type)
			.getResultList();
	}

	public List<YadaSocialCredentials> findByYadaUserCredentialsAndType(YadaUserCredentials yadaUserCredentials, int type) {
		String sql = "from YadaSocialCredentials where yadaUserCredentials=:yadaUserCredentials and type=:type";
		return em.createQuery(sql, YadaSocialCredentials.class)
				.setParameter("yadaUserCredentials", yadaUserCredentials)
				.setParameter("type", type)
				.getResultList();	
	}

    @Transactional(readOnly = false)
	public void deleteByYadaUserCredentialsAndType(YadaUserCredentials userCredentials, int facebookType) {
		String sql = "delete from YadaSocialCredentials e where e.yadaUserCredentials = :userCredentials and e.type = :facebookType";
		em.createQuery(sql)
			.setParameter("userCredentials", userCredentials)
			.setParameter("facebookType", facebookType)
			.executeUpdate();
	}

	@Transactional(readOnly = false)
	public YadaSocialCredentials save(YadaSocialCredentials entity) {
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
