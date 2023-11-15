package net.yadaframework.security.persistence.repository;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.security.persistence.entity.YadaTicket;
import net.yadaframework.security.persistence.entity.YadaTicketMessage;
import net.yadaframework.security.persistence.entity.YadaTicketStatus;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Repository
@Transactional(readOnly = true) 
public class YadaTicketDao {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
	private EntityManager em;
    
    @Autowired private YadaWebUtil yadaWebUtil;
    @Autowired private YadaConfiguration config;
    
    /**
     * Send a reply to a ticket.
     * @param yadaTicket
     * @param messageText
     * @param replySender the user replying to the previous message, could be either the support staff or the original user
     * @param supportStaffReply true if this reply is an answer to the user who opened the ticket, false if it is the user answer to the support staff
     * @param closeTicket true if the replyer has closed the ticket on this reply
     * @return the new message added to the ticket
     */
    @Transactional(readOnly = false) 
    public YadaTicketMessage replyTicket(Long yadaTicketId, String messageText, YadaUserProfile replySender, boolean supportStaffReply, boolean closeTicket) {
    	YadaTicket yadaTicket = em.find(YadaTicket.class, yadaTicketId); 
    	// Altrimenti si prende "could not initialize proxy - no Session" alla getMessages()
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
	 * @param attachment 
	 * @return the newly created ticket
	 */
    @Transactional(readOnly = false) 
    public YadaTicket addTicket(YadaLocalEnum<?> type, String title, String messageText, YadaUserProfile sender, int severity, MultipartFile attachmentFile) throws IOException {
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
		
		// Attachment
		if (attachmentFile!=null && !attachmentFile.isEmpty()) {
			File attachmentFolder = new File(config.getContentPath(), "tmp");
			String nameFile = sender.getId() + "_" + System.currentTimeMillis() +  "_" + attachmentFile.getOriginalFilename();
			File uploadedFile = new File(attachmentFolder, nameFile);
			yadaWebUtil.saveAttachment(attachmentFile, uploadedFile);
			
			YadaAttachedFile yadaAttachedFile = new YadaAttachedFile();
			// yadaAttachedFile.setAttachedToId(attachToId);
			yadaAttachedFile.setClientFilename(attachmentFile.getOriginalFilename());
			yadaAttachedFile.setFilenameDesktop(nameFile);

			yadaTicketMessage.addAttachment(yadaAttachedFile);
		}
		
		em.persist(yadaTicket); // Cascade save
		return yadaTicket;
	}

	public List<YadaTicket> findOldAnsweredYadaTicketNative() {
		String sql = "select * from YadaTicket  where creationDate <= (NOW() - INTERVAL 7 DAY) AND status_id = '10'";
		return em.createNativeQuery(sql, YadaTicket.class).getResultList();
	}

	
	public long countAllYadaTicketOpenNative() {
		String sql = "SELECT count(*) from YadaTicket where status_id = '13'";
		return (Long)em.createNativeQuery(sql, Long.class).getSingleResult();
	}



}
