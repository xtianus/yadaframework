package net.yadaframework.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaSocialCredentials;
import net.yadaframework.persistence.entity.YadaUserCredentials;

@Transactional(readOnly = true) 
public interface YadaSocialCredentialsRepository extends JpaRepository<YadaSocialCredentials, Long> {

	List<YadaSocialCredentials> findBySocialIdAndType(String socialId, int type); 

	List<YadaSocialCredentials> findByYadaUserCredentialsAndType(YadaUserCredentials yadaUserCredentials, int type);

	@Modifying
    @Transactional(readOnly = false)
	@Query("delete from #{#entityName} e where e.yadaUserCredentials = :userCredentials and e.type = :facebookType")
	void deleteByYadaUserCredentialsAndType(@Param("userCredentials") YadaUserCredentials userCredentials, @Param("facebookType") int facebookType);
	
	// List<YadaSocialCredentials> findByEmail(String email);



}
