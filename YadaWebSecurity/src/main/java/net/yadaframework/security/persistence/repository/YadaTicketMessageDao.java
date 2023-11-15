package net.yadaframework.security.persistence.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.security.persistence.entity.YadaTicket;
import net.yadaframework.security.persistence.entity.YadaTicketMessage;

@Repository
@Transactional(readOnly = true) 
public class YadaTicketMessageDao {
	
	@PersistenceContext
	EntityManager em;
	
	/**
	 * Find all YadaTicketMessage by one yadaTicket. Ordinato da dateSent descendente.
	 * @param yadaTicket
	 * @return
	 */
	public List<YadaTicketMessage> findByYadaTicketOrderByCreatedDesc(YadaTicket yadaTicket) {
		String sql = "from YadaTicketMessage where yadaTicket=:yadaTicket order by created desc";
		return em.createQuery(sql, YadaTicketMessage.class)
			.setParameter("yadaTicket", yadaTicket)
			.getResultList();
}
	
	/**
	 * Find all YadaTicketMessage and Attachments by one yadaTicket. Order by modified ASC.
	 * @param yadaTicket
	 * @return
	 */
	public List<YadaTicketMessage> findMessagesAndAttachmentByYadaTicketOrderByModifiedDesc(YadaTicket yadaTicket) {
		String sql = "select m from YadaTicket t join t.messages m left join fetch m.attachment a where t= :yadaTicket order by m.modified desc ";
		return em.createQuery(sql, YadaTicketMessage.class)
			.setParameter("yadaTicket", yadaTicket)
			.getResultList();
	}

}
