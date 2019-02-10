package net.yadaframework.security.persistence.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;

@Transactional(readOnly = true) 
public interface YadaAutoLoginTokenRepository extends JpaRepository<YadaAutoLoginToken, Long> {
	
	/**
	 * Returns the list of objects associated with the YadaUserCredentials
	 * @param YadaUserCredentials
	 * @return
	 */
	@Query("from #{#entityName} e where e.yadaUserCredentials=:yadaUserCredentials and (e.expiration is null or e.expiration > NOW())")
	List<YadaAutoLoginToken> findByYadaUserCredentials(YadaUserCredentials yadaUserCredentials);

	/**
	 * Returns the objects that match both id and token (should be no more than one I guess)
	 * @param id
	 * @param token
	 * @return
	 */
	List<YadaAutoLoginToken> findByIdAndTokenOrderByTimestampDesc(long id, long token);
	
	/**
	 * Delete expired elements
	 */
	@Modifying
    @Transactional(readOnly = false)
	@Query("delete from #{#entityName} e where e.expiration is not null and e.expiration < NOW()")
	void deleteExpired();

}
