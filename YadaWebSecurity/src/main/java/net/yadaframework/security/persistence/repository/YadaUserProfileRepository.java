package net.yadaframework.security.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Transactional(readOnly = true) 
public interface YadaUserProfileRepository<T extends YadaUserProfile> extends JpaRepository<T, Long> {
	
	@Query(value="select r.roles from YadaUserProfile yup join YadaUserCredentials yuc on yup.userCredentials_id = yuc.id " + 
			"join YadaUserCredentials_roles r on yuc.id = r.YadaUserCredentials_id where yup.id=:userProfileId", nativeQuery=true)
	List<Integer> findRoleIds(@Param("userProfileId") Long userProfileId);
	
	/**
	 * Retrive the userprofile id given the username
	 * @param username
	 * @return
	 */
	@Query(value="select up.id from YadaUserProfile up join YadaUserCredentials uc ON uc.id = up.userCredentials_id where uc.username=:username limit 1", nativeQuery=true)
	Long findUserProfileIdByUsername(@Param("username") String username);
	
	List<T> findByUserCredentials(YadaUserCredentials userCredentials, Pageable pageable);
	
	List<T> findByUserCredentialsUsername(String username, Pageable pageable);

	/**
	 * Find by role
	 * @param role
	 * @return
	 */
	@Query("select up from #{#entityName} up join up.userCredentials uc where uc.enabled = true and :role member of uc.roles")
	List<T> findEnabledUsersWithRole(@Param("role") Integer role);
	
	/**
	 * Find by enabled flag
	 * @return
	 */
	@Query("select up from #{#entityName} up join up.userCredentials uc where uc.enabled = true ")
	List<T> findEnabledUsers();
	
	
}
