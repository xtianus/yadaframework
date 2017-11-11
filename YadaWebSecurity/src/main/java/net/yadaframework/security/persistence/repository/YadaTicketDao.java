package net.yadaframework.security.persistence.repository;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaTicket;
import net.yadaframework.security.persistence.entity.YadaTicketMessage;
import net.yadaframework.security.persistence.entity.YadaTicketStatus;
import net.yadaframework.security.persistence.entity.YadaTicketType;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Repository
@Transactional(readOnly = true) 
public class YadaTicketDao {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext EntityManager em;

	/**
	 * Opens a new ticket
	 * @param type
	 * @param title
	 * @param messageText initial ticket message
	 * @param sender User opening the ticket
	 * @param severity
	 */
    public void addTicket(YadaTicketType type, String title, String messageText, YadaUserProfile sender, int severity) {
    	List<YadaTicketMessage> yadaTicketMessages = new ArrayList<>();
		YadaTicket yadaTicket = new YadaTicket();
		yadaTicket.setStatus(YadaTicketStatus.OPEN);
		yadaTicket.setPriority(severity);
		yadaTicket.setType(type);
		yadaTicket.setOwner(sender);

		YadaTicketMessage yadaTicketMessage = new YadaTicketMessage();	
		yadaTicketMessage.setTitle(title);
		yadaTicketMessage.setMessage(messageText);
		yadaTicketMessage.setSender(sender);
		// No need to set the message recipient because this information is in the YadaTicket object
		// yadaTicketMessage.setRecipient();
		yadaTicketMessage.setStackable(false); // Same-content messages will never be stacked
		yadaTicketMessage.setPriority(severity); // message priority is the same as ticket priority

		yadaTicket.setMessages(yadaTicketMessages);
		yadaTicketMessage.setYadaTicket(yadaTicket);
		yadaTicketMessages.add(yadaTicketMessage);
		
		em.persist(yadaTicket); // Cascade save
	}
	    

}
