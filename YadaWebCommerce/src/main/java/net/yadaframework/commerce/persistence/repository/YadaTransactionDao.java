package net.yadaframework.commerce.persistence.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.commerce.persistence.entity.YadaTransaction;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.web.YadaPageRequest;
import net.yadaframework.web.YadaPageRows;

@Repository
@Transactional(readOnly = true)
public class YadaTransactionDao {

    @PersistenceContext private EntityManager em;

    /**
     * Find a page of transactions (payments received or sent) for the specified user
     * @param userProfileId
     * @param yadaPageRequest
     * @return
     */
	public YadaPageRows<YadaTransaction> find(Long userProfileId, YadaPageRequest yadaPageRequest) {
		List<YadaTransaction> found = YadaSql.instance().selectFrom("from YadaTransaction")
            .where("payer.id = :userProfileId").or()
            .where("payee.id = :userProfileId").or()
            .orderBy(yadaPageRequest)
            .setParameter("userProfileId", userProfileId)
            .query(em, YadaTransaction.class)
            .setFirstResult(yadaPageRequest.getFirstResult())
            .setMaxResults(yadaPageRequest.getMaxResults())
            .getResultList();
        return new YadaPageRows<YadaTransaction>(found, yadaPageRequest);
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
