package net.yadaframework.security.persistence.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Repository
@Transactional(readOnly = true) 
public class YadaUserProfileDao<T extends YadaUserProfile> {
	
	@PersistenceContext EntityManager em;
	
	// DA FINIRE
	
//	public List<Integer> findRoleIds(Long userProfileId) {
//		String sql = "select r.roles from YadaUserProfile yup join YadaUserCredentials yuc on yup.userCredentials_id = yuc.id " + 
//			"join YadaUserCredentials_roles r on yuc.id = r.YadaUserCredentials_id where yup.id=:userProfileId";
//		return em.createNativeQuery(sql, Integer.class)
//				.setParameter("userProfileId", userProfileId)
//				.getResultList();
//	}
//
//	
//	/**
//	 * Retrieve the userprofile id given the username
//	 * @param username
//	 * @return
//	 */
//	public Long findUserProfileIdByUsername(String username) {
//		String sql = "select up.id from YadaUserProfile up join YadaUserCredentials uc ON uc.id = up.userCredentials_id where uc.username=:username limit 1";
//		try {
//			return (Long) em.createNativeQuery(sql, Long.class)
//				.setParameter("username", username)
//				.setMaxResults(1)
//				.getSingleResult();
//		} catch (NonUniqueResultException | NoResultException e) {
//			return null; // Nothing found
//		}
//	}
//
//	
//	public List<T> findByUserCredentials(YadaUserCredentials userCredentials, YadaPageRequest pageable) {
//		String sql = "from YadaUserProfile where userCredentials = :userCredentials";
//			TypedQuery<T> query = em.createQuery(sql)
//				.setParameter("userCredentials", userCredentials);
//			if (pageable!=null && pageable.isValid()) {
//				query.setFirstResult(pageable.getFirstResult()).setMaxResults(pageable.getSize());
//			}
//			// TODO sistemare anche il sort usando il pageable!!!
//			return query.getResultList();
//		}
//	}
//
//	
//	public List<T> findByUserCredentialsUsername(String username, Pageable pageable) {
//		String sql = "from YadaUserProfile where userCredentials.username = :username";
//	}
//
//	/**
//	 * Find by role
//	 * @param role
//	 * @return
//	 */
//	@Query()
//	public List<T> findEnabledUsersWithRole(@Param("role") Integer role) {
//		String sql = "select up from YadaUserProfile up join up.userCredentials uc where uc.enabled = true and :role member of uc.roles";
//	}
//
//	
//	/**
//	 * Find by enabled flag
//	 * @return
//	 */
//	@Query()
//	public List<T> findEnabledUsers() {
//		String sql = "select up from YadaUserProfile up join up.userCredentials uc where uc.enabled = true ";
//	}
//
//	
//	/**
//	 * Find the profile for the given user credentials id
//	 * @param userProfileId
//	 * @return
//	 */
//	@Query()
//	public YadaUserProfile findByUserCredentialsId(@Param("userCredentialsId") Long userProfileId) {
//		String sql = "select up from YadaUserProfile up join up.userCredentials yuc where userCredentials_id = :userCredentialsId";
//	}
//
//	
//	@Query(value=)
//	public YadaUserProfile findUserProfileByUsername(@Param("username") String username) {
//		String sql = "select up from YadaUserProfile up join up.userCredentials yuc where username=:username";
//	}
//
//	
	
}
