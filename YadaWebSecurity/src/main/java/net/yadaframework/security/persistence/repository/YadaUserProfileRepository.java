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
public interface YadaUserProfileRepository extends JpaRepository<YadaUserProfile, Long> {
	
	List<YadaUserProfile> findByUserCredentials(YadaUserCredentials userCredentials, Pageable pageable);
	
	List<YadaUserProfile> findByUserCredentialsUsername(String username, Pageable pageable);

	/**
	 * Find by role
	 * @param role
	 * @return
	 */
	@Query("select up from #{#entityName} up join up.userCredentials uc where uc.enabled = true and :role member of uc.roles")
	List<YadaUserProfile> findEnabledUsersWithRole(@Param("role") Integer role);
	
	/**
	 * Find by enabled flag
	 * @return
	 */
	@Query("select up from #{#entityName} up join up.userCredentials uc where uc.enabled = true ")
	List<YadaUserProfile> findEnabledUsers();
	
	
}
