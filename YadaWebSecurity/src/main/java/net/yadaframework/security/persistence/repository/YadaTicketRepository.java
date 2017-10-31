package net.yadaframework.security.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.persistence.entity.YadaTicketStatus;
import net.yadaframework.security.persistence.entity.YadaTicket;

@Transactional(readOnly = true) 
public interface YadaTicketRepository extends JpaRepository<YadaTicket, Long> {

	
	/**
	 * Find the credentials for the given user profile id
	 * @param 
	 * @return
	 */
	//@Query("select e from #{#entityName} e  where e.creationDate <= (NOW() - INTERVAL 7 DAY)  AND e.status = :status")
	//List<YadaTicket> findOldAnsweredYadaTicket(@Param("status") YadaPersistentEnum<YadaTicketStatus> status);
	
	@Query(value="select * from YadaTicket  where creationDate <= (NOW() - INTERVAL 7 DAY) AND status_id = '10'", nativeQuery = true)
	List<YadaTicket> findOldAnsweredYadaTicketNative();
}
