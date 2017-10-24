package net.yadaframework.security.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaTicket;
import net.yadaframework.security.persistence.entity.YadaTicketMessage;

@Transactional(readOnly = true) 
public interface YadaTicketMessageRepository extends JpaRepository<YadaTicketMessage, Long> {
	
	/**
	 * Find all YadaTicketMessage by one yadaTicket. Ordinato da dateSent descendente.
	 * @param yadaTicket
	 * @return
	 */
	List<YadaTicketMessage> findByYadaTicketOrderByDateSentDesc(YadaTicket yadaTicket);
	
	List<YadaTicketMessage> findByYadaTicketOrderByDateSentAsc(YadaTicket yadaTicket);

}
