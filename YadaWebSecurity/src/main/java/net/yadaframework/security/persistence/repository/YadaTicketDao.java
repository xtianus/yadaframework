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

    @PersistenceContext private EntityManager em;
    
    /**
     * Send a reply to a ticket.
     * @param yadaTicket
     * @param messageText
     * @param replySender the user replying to the previous message, could be either the support staff or the original user
     * @param supportStaffReply true if this reply is an answer to the user who opened the ticket, false if it is the user answer to the support staff
     * @param closeTicket true if the replyer has closed the ticket on this reply
     * @return the new message added to the ticket
     */
    public YadaTicketMessage replyTicket(YadaTicket yadaTicket, String messageText, YadaUserProfile replySender, boolean supportStaffReply, boolean closeTicket) {
		YadaTicketMessage yadaTicketMessage = new YadaTicketMessage();	
		yadaTicket.getMessages().add(yadaTicketMessage);
		yadaTicketMessage.setYadaTicket(yadaTicket);
		yadaTicketMessage.setMessage(messageText);
		yadaTicketMessage.setTitle(yadaTicket.getTitle());
		yadaTicketMessage.setSender(replySender);
		if (supportStaffReply) {
			yadaTicketMessage.setRecipient(yadaTicket.getOwner());
		} else {
			yadaTicketMessage.setRecipient(yadaTicket.getAssigned()); // Could be null, but it's ok
		}
		yadaTicketMessage.setStackable(false); // Same-content messages will never be stacked
		yadaTicketMessage.setPriority(yadaTicket.getPriority()); // message priority is the same as ticket priority
		if (closeTicket) {
			yadaTicket.setStatus(YadaTicketStatus.CLOSED);
		} else {
			// When the owner replies without closing, the ticket becomes OPEN
			// When the staff replies an open ticket, the status becomes ANSWERED
			yadaTicket.setStatus(supportStaffReply && yadaTicket.isOpen() ? YadaTicketStatus.ANSWERED : YadaTicketStatus.OPEN);
		}
		return yadaTicketMessage;
    }

	/**
	 * Opens a new ticket
	 * @param type
	 * @param title
	 * @param messageText initial ticket message
	 * @param sender User opening the ticket
	 * @param severity
	 * @return the newly created ticket
	 */
    public YadaTicket addTicket(YadaTicketType type, String title, String messageText, YadaUserProfile sender, int severity) {
    	List<YadaTicketMessage> yadaTicketMessages = new ArrayList<>();
		YadaTicket yadaTicket = new YadaTicket();
		yadaTicket.setStatus(YadaTicketStatus.OPEN);
		yadaTicket.setPriority(severity);
		yadaTicket.setType(type);
		yadaTicket.setOwner(sender);
		yadaTicket.setTitle(title);

		YadaTicketMessage yadaTicketMessage = new YadaTicketMessage();	
		yadaTicketMessage.setTitle(title);
		yadaTicketMessage.setMessage(messageText);
		yadaTicketMessage.setSender(sender);
		// When a ticket is added, no recipient has been chosen yet for the message
		// yadaTicketMessage.setRecipient();
		yadaTicketMessage.setStackable(false); // Same-content messages will never be stacked
		yadaTicketMessage.setPriority(severity); // message priority is the same as ticket priority

		yadaTicket.setMessages(yadaTicketMessages);
		yadaTicketMessage.setYadaTicket(yadaTicket);
		yadaTicketMessages.add(yadaTicketMessage);
		
		em.persist(yadaTicket); // Cascade save
		return yadaTicket;
	}
	    

}
