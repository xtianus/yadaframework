package net.yadaframework.security.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaTicket;
import net.yadaframework.security.persistence.entity.YadaTicketMessage;
import net.yadaframework.security.persistence.entity.YadaUserMessage;

@Transactional(readOnly = true) 
public interface YadaUserMessageRepository extends JpaRepository<YadaUserMessage, Long> {
	
	
	@Query(value="select * from YadaUserMessage  where modified <= (NOW() - INTERVAL 30 DAY)", nativeQuery = true)
	List<YadaUserMessage> findOldYadaUserMessages();
}
