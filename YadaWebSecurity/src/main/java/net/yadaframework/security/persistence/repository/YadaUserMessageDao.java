package net.yadaframework.security.persistence.repository;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.security.persistence.entity.YadaUserMessage;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.web.YadaPageRequest;
import net.yadaframework.web.YadaPageRows;

@Repository
@Transactional(readOnly = true)
public class YadaUserMessageDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired private YadaUtil yadaUtil;

    @PersistenceContext
	EntityManager em;

	/**
	 * Returns the most recent date of the message stack - which is the initial date if the message is not stackable
	 * @return
	 */
    public Date getLastDate(YadaUserMessage message) {
    	message = em.merge(message);
    	return message.getLastDate();
    }

	@Transactional(readOnly = false)
	public void markAsRead(List<YadaUserMessage> messages) {
		String jpql = "update YadaUserMessage m set m.readByRecipient = true where m in :messages";
		em.createQuery(jpql).setParameter("messages", messages).executeUpdate();
		// Don't do this or "unread" messages won't show as such
		// for (YadaUserMessage yadaUserMessage : messages) {
		// 		yadaUserMessage.setReadByRecipient(true);
		// }
	}

	public YadaUserMessage find(Long id) {
		return em.find(YadaUserMessage.class, id);
	}

	/**
	 * Find a page of notifications
	 * @param currentUserProfileId
	 * @param yadaPageRequest
	 * @param types required types, can be omitted
	 * @return
	 */
	public YadaPageRows<YadaUserMessage> find(Long recipientUserProfileId, YadaPageRequest yadaPageRequest, YadaPersistentEnum<?> ... types) {
		boolean hasTypes = types!=null && types.length>0;
		List<YadaUserMessage> found = YadaSql.instance().selectFrom("select yum from YadaUserMessage yum")
			.join("join yum.recipient recipient")
			.join(hasTypes, "join yum.type t")
			.where(hasTypes, "t in :types").and()
			.where("recipient.id = :userProfileId").and()
			.orderBy(yadaPageRequest)
			.setParameter("types", types)
			.setParameter("userProfileId", recipientUserProfileId)
			.query(em, YadaUserMessage.class)
			.setFirstResult(yadaPageRequest.getFirstResult())
			.setMaxResults(yadaPageRequest.getMaxResults())
			.getResultList();
		return new YadaPageRows<YadaUserMessage>(found, yadaPageRequest);
	}

    /**
     * Returns true if there exists at least one unread message for the user
     * @param userProfile
     * @return
     */
    public boolean hasUnreadMessage(YadaUserProfile userProfile) {
    	String sql = "select case when exists ( "
			+ "select 1 from YadaUserMessage s where s.recipient_id=:userProfileId and s.readByRecipient=false limit 1 "
			+ ") then 1 else 0 end";
    	Query nativeQuery = em.createNativeQuery(sql);
    	nativeQuery.setParameter("userProfileId", userProfile.getId());
		Long singleResult = (Long) nativeQuery.getSingleResult();
    	return singleResult.intValue()==1;
    }

    /**
     * Delete all messages that do not involve users other than the one specified (no other users as sender o recipient)
     * @param userProfile the receiver/sender of the message
     */
    @Transactional(readOnly = false)
    public void deleteBelongingTo(YadaUserProfile userProfile) {
    	// Messages that have the user as sender or recipient, and nobody else involved, or
    	// where the user is both sender and recipient
    	List <YadaUserMessage> toDelete = YadaSql.instance().selectFrom("select yum from YadaUserMessage yum")
    	    .where("(sender=:userProfile and recipient=null)").or()
    	    .where("(sender=null and recipient=:userProfile)").or()
    	    .where("(sender=:userProfile and recipient=:userProfile)")
	    	.setParameter("userProfile", userProfile)
	    	.query(em, YadaUserMessage.class).getResultList();
    	// Need to fetch them all then delete them, in order to cascade deletes to "created" and "attachment"
    	for (YadaUserMessage yadaUserMessage : toDelete) {
			em.remove(yadaUserMessage);
		}
    }

    /**
     * Save a message. If the message is stackable, only increment the counter of an existing message with identical
     * content and same recipient and same sender and same data, if not older than one day.
     * @param m the new message that will be persisted if not already in the database. On return, in any case this will hold
     *          the result of the database operation (either a new instance or an existing element with incremented counter)
     * @return true if the message has been created, false if the counter incremented
     */
    @Transactional(readOnly = false)
    public boolean createOrIncrement(YadaUserMessage<?> m) {
    	log.debug("YadaUserMessage to {} from {}: [{}] \"{}\" {} (data={})",
    		m.getReceiverName()!=null?m.getReceiverName():"-",
    		m.getSender()!=null?m.getSenderName():"-",
    		m.getPriority(), m.getTitle(), m.getMessage(), m.getData());
    	if (m.getId()!=null) {
    		throw new YadaInvalidUsageException("Message already exists with id=" + m.getId());
    	}
    	/* Recipient can be null. For example in a ticket.
    	if (m.getRecipient()==null) {
    		throw new YadaInvalidUsageException("Message with no recipient - title=" + m.getTitle());
    	}*/
    	if (!m.isStackable()) {
    		em.persist(m);
    		return true; // Created new
    	}
    	// Need to update an existing row counter if it exists, or insert a new one.
    	// Not using "insert ... on duplicate key update" because of the time window
    	// TODO the time window could be a (configured) parameter
    	Date oldestStackTime = yadaUtil.daysAgo(1); // Time window: one day ago
    	m.computeHash(); // Needed
    	List<YadaUserMessage> existingList = YadaSql.instance().selectFrom("select m from YadaUserMessage m") //YadaTicketMessage?
    		.where("m.contentHash=:contentHash").and()
    		.where("m.modified > :oldestStackTime").and()
    		.where("m.recipient = :recipient").and()
    		.where(m.getSender()!=null, "m.sender = :sender").and()
    		.where(m.getData()!=null, "m.data = :data").and()
    		.orderBy("m.modified desc")
    		.setParameter("contentHash", m.getContentHash())
    		.setParameter("oldestStackTime", oldestStackTime)
    		.setParameter("recipient", m.getRecipient())
    		.setParameter("sender", m.getSender())
    		.setParameter("data", m.getData())
    		.query(em, YadaUserMessage.class).setMaxResults(1).getResultList();

    	if (existingList.isEmpty()) {
    		em.persist(m);
    		return true; // Created new
    	} else {
    		m = existingList.get(0);
    		m.incrementStack();
    		m.setReadByRecipient(false);
    		m.setModified(new Date());
    		return false;
    	}
    }

	List<YadaUserMessage> findOldYadaUserMessages() {
		String sql = "select * from YadaUserMessage  where modified <= (NOW() - INTERVAL 30 DAY)";
		return em.createNativeQuery(sql, YadaUserMessage.class)
				.getResultList();
	}



}
