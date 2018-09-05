package net.yadaframework.security.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
	List<YadaTicketMessage> findByYadaTicketOrderByCreatedDesc(YadaTicket yadaTicket);
	
	//List<YadaTicketMessage>findByYadaTicketOrderByCreatedAsc(YadaTicket yadaTicket);
	
	
	/**
	 * Find all YadaTicketMessage and Attachments by one yadaTicket. Order by modified ASC.
	 * @param yadaTicket
	 * @return
	 */
	@Query("select m from YadaTicket t join t.messages m join fetch m.attachment a where t= :yadaTicket order by m.modified asc ")
	List<YadaTicketMessage> findMessagesAndAttachmentByYadaTicketOrderByModifiedAsc(@Param("yadaTicket") YadaTicket yadaTicket);

}
