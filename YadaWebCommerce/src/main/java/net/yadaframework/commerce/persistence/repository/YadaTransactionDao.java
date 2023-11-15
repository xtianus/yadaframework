package net.yadaframework.commerce.persistence.repository;

import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.commerce.persistence.entity.YadaOrder;
import net.yadaframework.commerce.persistence.entity.YadaTransaction;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.web.YadaPageRequest;
import net.yadaframework.web.YadaPageRows;

@Repository
@Transactional(readOnly = true)
public class YadaTransactionDao {

    @PersistenceContext private EntityManager em;

    /**
     * Returns the sum of all transaction amounts for the given user or for all users.
     * @param accountOwner when null, all transactions from all users are counted. The result should be zero in a
     * double ledger system.
     * @return
     */
    public YadaMoney sumAmount(@Nullable YadaUserProfile accountOwner) {
    	Long sum = (Long) YadaSql.instance().selectFrom("select SUM(amount) from YadaTransaction")
    		.where(accountOwner!=null, "where accountOwner=:accountOwner")
    		.setParameter("accountOwner", accountOwner)
    		.query(em).getSingleResult();
    	YadaMoney result = YadaMoney.fromDatabaseColumn(sum);
    	return result;
    	}

    /**
     * Given an existing transaction, create the twin opposite transaction in order to implement a double ledger
     * @param payment
     * @return a new specular transaction with the amount of opposite sign and parties exchanged
     */
    @Transactional(readOnly = false)
    public YadaTransaction createInverse(YadaTransaction payment) {
    	payment = em.merge(payment);
    	payment.setInverse(false); // Initial transaction
		YadaTransaction yadaTransaction = new YadaTransaction();
		yadaTransaction.setAmount(payment.getAmount().getNegated());
		yadaTransaction.setCurrencyCode(payment.getCurrencyCode());
		yadaTransaction.setAccountOwner(payment.getOtherParty());
		yadaTransaction.setOtherParty(payment.getAccountOwner());
		yadaTransaction.setTransactionId(payment.getTransactionId());
		yadaTransaction.setTimestamp(payment.getTimestamp());
		yadaTransaction.setStatus(payment.getStatus());
		yadaTransaction.setDescription(payment.getDescription());
		yadaTransaction.setPayerId1(payment.getPayerId1());
		yadaTransaction.setPayerId2(payment.getPayerId2());
		yadaTransaction.setOrder(payment.getOrder());
		yadaTransaction.setData(payment.getData());
		yadaTransaction.setPaymentSystem(payment.getPaymentSystem());
		yadaTransaction.setInverse(true); // twin transaction
		em.persist(yadaTransaction);
		return yadaTransaction;
    }

//    /**
//     * Create a new transaction
//     * @param movement the amount sent (if negative) or received (if positive)
//     * @param currency currency symbol like "EUR" or "€"
//     * @param sender user that sends money (payer). It is the account owner if the movement is negative.
//     * @param receiver user that receives money (payee). It is the account owner if the movement is positive.
//     * @param externalTransactionId transaction id received from the payment system
//     * @param externalTransactionTime transaction time received from the payment system
//     * @param externalStatus status of the transaction received from the payment system
//     * @param description
//     * @param externalPayerEmail email that identifies the payer on the payment system
//     * @param yadaOrder order for which the transaction has been made
//     * @return
//     */
//    @Transactional(readOnly = false)
//	public YadaTransaction makeTwinTransactions(YadaMoney movement, String currency, YadaUserProfile sender, YadaUserProfile receiver,
//			String externalTransactionId, Date externalTransactionTime, String externalStatus, String description, String externalPayerEmail,
//			YadaOrder yadaOrder) {
//		YadaTransaction yadaTransaction = new YadaTransaction();
//		yadaTransaction.setAmount(movement);
//		yadaTransaction.setCurrencyCode(currency);
//		yadaTransaction.setAccountOwner(sender);
//		yadaTransaction.setOtherParty(receiver);
//		yadaTransaction.setTransactionId(externalTransactionId);
//		yadaTransaction.setTimestamp(externalTransactionTime);
//		yadaTransaction.setStatus(externalStatus);
//		yadaTransaction.setDescription(description);
//		yadaTransaction.setData(externalPayerEmail);
//		yadaTransaction.setOrder(yadaOrder);
//		em.persist(yadaTransaction);
//		return yadaTransaction;
//	}


    /**
     * Delete a suspended transaction relative to an order.
     * @param order
     */
    @Transactional(readOnly = false)
	public int deleteSuspended(YadaOrder yadaOrder) {
		String sql = "delete from YadaTransaction where order = :yadaOrder and suspended is true";
		return em.createQuery(sql).setParameter("yadaOrder", yadaOrder).executeUpdate();
	}

    /**
     * Find all direct transactions (not the inverse) related to an order, and not suspended
     * i.e. only the originating transactions of the double ledger, that may be two in case of a refund
     * @param yadaOrder
     * @return
     */
    public List<YadaTransaction> find(YadaOrder yadaOrder) {
    	List<YadaTransaction> found = YadaSql.instance().selectFrom("from YadaTransaction")
			.where("order = :yadaOrder").and()
			.where("inverse != true").and()
			.where("suspended != true").and()
            .setParameter("yadaOrder", yadaOrder)
            .query(em, YadaTransaction.class)
            .getResultList();
		return found;
    }

    /**
     * Find a page of transactions (payments received or sent) for the specified user
     * @param userProfileId
     * @param yadaPageRequest
     * @return
     */
	public YadaPageRows<YadaTransaction> find(Long userProfileId, YadaPageRequest yadaPageRequest) {
		// Trova sia i pagamenti che i rimborsi. Nel primo caso trova le transazioni dirette, nel secondo trova quelle inverse.
		List<YadaTransaction> found = YadaSql.instance().selectFrom("from YadaTransaction")
            .where("accountOwner.id = :userProfileId")
            .orderBy(yadaPageRequest)
            .setParameter("userProfileId", userProfileId)
            .query(em, YadaTransaction.class)
            .setFirstResult(yadaPageRequest.getFirstResult())
            .setMaxResults(yadaPageRequest.getMaxResults())
            .getResultList();
        return new YadaPageRows<YadaTransaction>(found, yadaPageRequest);
	}

    public YadaTransaction find(Long id) {
    	if (id==null) {
    		return null;
    	} else {
    		return em.find(YadaTransaction.class, id);
    	}
    }

    @Transactional(readOnly = false)
    public YadaTransaction save(YadaTransaction entity) {
    	if (entity.getId()==null) {
    		em.persist(entity);
    	} else {
    		entity = em.merge(entity);
    	}
    	return entity;
    }

}
