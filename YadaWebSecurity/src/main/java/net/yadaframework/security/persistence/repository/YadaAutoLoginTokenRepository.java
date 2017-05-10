package net.yadaframework.security.persistence.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;

@Transactional(readOnly = true) 
public interface YadaAutoLoginTokenRepository extends JpaRepository<YadaAutoLoginToken, Long> {
	
	List<YadaAutoLoginToken> findByIdAndTokenOrderByTimestampDesc(long id, long token);
	
	@Modifying
    @Transactional(readOnly = false)
	@Query("delete from #{#entityName} e where e.expiration is not null and e.expiration < NOW()")
	void deleteExpired();

}
