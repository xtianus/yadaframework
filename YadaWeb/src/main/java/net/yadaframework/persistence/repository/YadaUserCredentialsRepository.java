package net.yadaframework.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaUserCredentials;

@Transactional(readOnly = true) 
public interface YadaUserCredentialsRepository extends JpaRepository<YadaUserCredentials, Long> {

//	List<YadaUserCredentials> findByYadaSocialCredentialsList(YadaSocialCredentials yadaSocialCredentials); 


	/**
	 * Cerca il primo utente con lo username indicato
	 * @param username
	 * @return
	 */
	YadaUserCredentials findFirstByUsername(String username);

	/**
	 * Cerca (eventualmente il primo) utente con lo username indicato
	 * @param username
	 * @param pageable il pageable, oppure yadaWebUtil.FIND_ONE per avere solo il primo
	 * @return
	 */
	List<YadaUserCredentials> findByUsername(String username, Pageable pageable); 

	List<YadaUserCredentials> findByUsername(String username);

	@Modifying
	@Transactional
	@Query("update #{#entityName} e set e.lastSuccessfulLogin = NOW() where e.username = :username")
	void updateLoginTimestamp(@Param("username") String username); 

	@Modifying
	@Transactional
	@Query("update #{#entityName} e set e.failedAttempts = e.failedAttempts + 1, e.lastFailedAttempt = NOW() where e.username = :username")
	void incrementFailedAttempts(@Param("username") String username); 

	@Modifying
	@Transactional
	@Query("update #{#entityName} e set e.failedAttempts = 0, e.lastFailedAttempt = null where e.username = :username")
	void resetFailedAttempts(@Param("username") String username);

}
