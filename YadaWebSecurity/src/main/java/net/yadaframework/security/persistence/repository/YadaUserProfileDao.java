package net.yadaframework.security.persistence.repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.web.YadaPageRequest;

@Repository
@Transactional(readOnly = true)
public class YadaUserProfileDao<T extends YadaUserProfile> {

	@PersistenceContext EntityManager em;

	@Autowired YadaUtil yadaUtil;

	public List<Integer> findRoleIds(Long userProfileId) {
		String sql = "select r.roles from YadaUserProfile yup join YadaUserCredentials yuc on yup.userCredentials_id = yuc.id " +
			"join YadaUserCredentials_roles r on yuc.id = r.YadaUserCredentials_id where yup.id=:userProfileId";
		return em.createNativeQuery(sql)
				.setParameter("userProfileId", userProfileId)
				.getResultList();
	}

	/**
	 * Retrieve the userprofile id given the username (email)
	 * @param username
	 * @return
	 */
	public Long findUserProfileIdByUsername(String username) {
		String sql = "select up.id from YadaUserProfile up join YadaUserCredentials uc ON uc.id = up.userCredentials_id where uc.username=:username";
		try {
			return ((BigInteger)em.createNativeQuery(sql)
				.setParameter("username", username)
				.setMaxResults(1)
				.getSingleResult()).longValue();
		} catch (NonUniqueResultException | NoResultException e) {
			return null; // Nothing found
		}
	}

	public List<T> findByUserCredentials(YadaUserCredentials userCredentials, YadaPageRequest pageable) {
		String sql = "from YadaUserProfile where userCredentials = :userCredentials";
		boolean isPage = pageable!=null && pageable.isValid();
		if (isPage) {
			sql += " " + YadaSql.getOrderBy(pageable);
		}
		Class<T> returnTypeClass = (Class<T>) yadaUtil.findGenericClass(this); // Returns the class that extends YadaUserProfile e.g. UserProfile.class
		TypedQuery<T> query = em.createQuery(sql, returnTypeClass)
			.setParameter("userCredentials", userCredentials);
		if (isPage) {
			query.setFirstResult(pageable.getFirstResult()).setMaxResults(pageable.getSize());
		}
		return query.getResultList();
	}


	public List<T> findByUserCredentialsUsername(String username, YadaPageRequest pageable) {
		String sql = "from YadaUserProfile where userCredentials.username = :username";
		boolean isPage = pageable!=null && pageable.isValid();
		if (isPage) {
			sql += " " + YadaSql.getOrderBy(pageable);
		}
		Class<T> returnTypeClass = (Class<T>) yadaUtil.findGenericClass(this); // Returns the class that extends YadaUserProfile e.g. UserProfile.class
		TypedQuery<T> query = em.createQuery(sql, returnTypeClass)
			.setParameter("username", username);
		if (isPage) {
			query.setFirstResult(pageable.getFirstResult()).setMaxResults(pageable.getSize());
		}
		return query.getResultList();
	}

	/**
	 * Find by role
	 * @param role
	 * @return
	 */
	public List<T> findEnabledUsersWithRole(Integer role) {
		String sql = "select up from YadaUserProfile up join up.userCredentials uc where uc.enabled = true and :role member of uc.roles";
		Class<T> returnTypeClass = (Class<T>) yadaUtil.findGenericClass(this); // Returns the class that extends YadaUserProfile e.g. UserProfile.class
		TypedQuery<T> query = em.createQuery(sql, returnTypeClass)
			.setParameter("role", role);
		return query.getResultList();
	}


	/**
	 * Find by enabled flag
	 * @return
	 */
	public List<T> findEnabledUsers() {
		String sql = "select up from YadaUserProfile up join up.userCredentials uc where uc.enabled = true";
		Class<T> returnTypeClass = (Class<T>) yadaUtil.findGenericClass(this); // Returns the class that extends YadaUserProfile e.g. UserProfile.class
		return em.createQuery(sql, returnTypeClass).getResultList();
	}


	/**
	 * Find the profile for the given user credentials id
	 * @param userProfileId
	 * @return
	 */
	public T findByUserCredentialsId(Long userCredentialsId) {
		String sql = "select up from YadaUserProfile up join up.userCredentials yuc where userCredentials_id = :userCredentialsId";
		List<YadaUserProfile> resultList = em.createQuery(sql, YadaUserProfile.class)
			.setMaxResults(1)
			.setParameter("userCredentialsId", userCredentialsId)
			.getResultList();
		return normaliseSingleResult(resultList);
	}


	public T findUserProfileByUsername(String username) {
		String sql = "select up from YadaUserProfile up join up.userCredentials yuc where username=:username";
		List<YadaUserProfile> resultList = em.createQuery(sql, YadaUserProfile.class)
			.setMaxResults(1)
			.setParameter("username", username)
			.getResultList();
		return normaliseSingleResult(resultList);
	}

    /**
     * For backwards compatibility, returns null when no result is found
     * @param resultList
     * @return
     */
    private T normaliseSingleResult(List<YadaUserProfile> resultList) {
		// Need to keep the contract of the Spring Data Repository, so we return null when no value found.
		if (resultList.isEmpty()) {
			return null;
		} else {
			return (T) resultList.get(0);
		}
    }

	@Transactional(readOnly = false)
	public T save(T entity) {
		if (entity==null) {
			return null;
		}
		if (entity.getId()==null) {
			em.persist(entity);
			return entity;
		}
		return em.merge(entity);
	}

	// Signature kept for legacy Spring Data Repository compatibility
	public Optional<T> findById(Long entityId) {
		// Class<T> returnTypeClass = (Class<T>) yadaUtil.findGenericClass(this); // Returns the class that extends YadaUserProfile e.g. UserProfile.class
		YadaUserProfile result = em.find(YadaUserProfile.class, entityId);
		return (Optional<T>) Optional.ofNullable(result);
	}

}
