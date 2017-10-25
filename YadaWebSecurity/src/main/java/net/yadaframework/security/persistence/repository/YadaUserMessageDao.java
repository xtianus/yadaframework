package net.yadaframework.security.persistence.repository;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.persistence.entity.YadaUserMessage;

@Repository
@Transactional(readOnly = true) 
public class YadaUserMessageDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired private YadaUtil yadaUtil;
	
    @PersistenceContext EntityManager em;
    
    /**
     * Save a message. If the message is stackable, only increment the counter of an existing message with identical
     * content and same recipient and same sender and same data, if not older than one day
     * @param m
     */
    @Modifying
    @Transactional(readOnly = false) 
    public void createOrIncrement(YadaUserMessage<?> m) {
    	log.debug("YadaUserMessage to {} from {}: [{}] '{}' - {} ({})", 
    		m.getReceiverName(), m.getSenderUser()!=null?m.getSenderName():"-", 
    		m.getPriority(), m.getTitle(), m.getMessage(), m.getData());
    	if (m.getId()!=null) {
    		throw new YadaInvalidUsageException("Message already exists with id=" + m.getId());
    	}
    	if (m.getRecipient()==null) {
    		throw new YadaInvalidUsageException("Message with no recipient - title=" + m.getTitle());
    	}
    	if (!m.isStackable()) {
    		em.persist(m);
    		return;
    	}
    	// Need to update an existing row counter if it exists, or insert a new one.
    	// Not using "insert ... on duplicate key update" because of the time window
    	// TODO the time window could be a (configured) parameter
    	Date oldestStackTime = yadaUtil.daysAgo(1); // Time window: one day ago
    	m.computeHash(); // Needed
    	List<YadaUserMessage> existingList = YadaSql.instance().selectFrom("select m from YadaTicketMessage m")
    		.where("m.contentHash=:contentHash").and()
    		.where("m.modified > :oldestStackTime").and()
    		.where("m.recipient = :recipient").and()
    		.where(m.getSenderUser()!=null, "m.senderUser = :senderUser").and()
    		.where(m.getData()!=null, "m.data = :data").and()
    		.setParameter("contentHash", m.getContentHash())
    		.setParameter("oldestStackTime", oldestStackTime)
    		.setParameter("recipient", m.getRecipient())
    		.setParameter("senderUser", m.getSenderUser())
    		.setParameter("data", m.getData())
    		.query(em, YadaUserMessage.class).setMaxResults(1).getResultList();
    	
    	if (existingList.isEmpty()) {
    		em.persist(m);
    	} else {
    		m = existingList.get(1);
    		m.setStackSize(m.getStackSize()+1);
    		m.setRead(false);
    	}
    }
}
