package net.yadaframework.security.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.web.YadaPageRequest;

@Repository
@Transactional(readOnly = true)
public class YadaUserProfileDao<T extends YadaUserProfile> {

	@PersistenceContext
	EntityManager em;

	@Autowired YadaUtil yadaUtil;
	@Autowired YadaConfiguration config;

	@Transactional(readOnly = false)
	public void updateTimezone(String username, TimeZone timezone) {
		if (username==null || timezone==null) {
			return;
		}
		String sql = "update YadaUserProfile yup join YadaUserCredentials yuc on yup.userCredentials_id=yuc.id "
			+ "set yup.timezone=:timezone where yuc.username=:username and yup.timezoneSetByUser=false";
		em.createNativeQuery(sql)
			.setParameter("username", username)
			.setParameter("timezone", timezone.getID())
			.executeUpdate();
	}

	/**
	 * Find all user profiles that have the given role key
	 * @param roleKey the role key from config, like "ADMIN" or "USER"
	 */
	public List<T> findByRoleKey(String roleKey) {
		Integer roleId = config.getRoleId(roleKey);
		String sql = "select yup from YadaUserProfile yup join yup.userCredentials uc where :roleId member of uc.roles";
		return em.createQuery(sql)
			.setParameter("roleId", roleId)
			.getResultList();
	}

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
			return (Long) em.createNativeQuery(sql)
					.setParameter("username", username)
					.setMaxResults(1)
					.getSingleResult();
		} catch (NonUniqueResultException | NoResultException e) {
			return null; // Nothing found
		}
	}

	public List<T> findByUserCredentials(YadaUserCredentials userCredentials, YadaPageRequest pageable) {
		String sql = "from YadaUserProfile where userCredentials = :userCredentials";
		boolean isPage = pageable!=null && pageable.isValid();
		if (isPage) {
			sql += " " + YadaSql.getOrderByNative(pageable);
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
			sql += " " + YadaSql.getOrderByNative(pageable);
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

	/**
	 * Find a user profile
	 * @param userProfileId
	 * @return YadaUserProfile (subclass) instance or null
	 */
	public T find(long userProfileId) {
		T userProfile = (T) em.find(YadaUserProfile.class, userProfileId);
		return userProfile;
	}

	// Signature kept for legacy Spring Data Repository compatibility
	public Optional<T> findById(Long entityId) {
		// Class<T> returnTypeClass = (Class<T>) yadaUtil.findGenericClass(this); // Returns the class that extends YadaUserProfile e.g. UserProfile.class
		YadaUserProfile result = em.find(YadaUserProfile.class, entityId);
		return (Optional<T>) Optional.ofNullable(result);
	}

}
