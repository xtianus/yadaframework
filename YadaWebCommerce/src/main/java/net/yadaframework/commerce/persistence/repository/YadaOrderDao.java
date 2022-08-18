package net.yadaframework.commerce.persistence.repository;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.commerce.persistence.entity.YadaOrder;
import net.yadaframework.commerce.persistence.entity.YadaOrderItem;
import net.yadaframework.commerce.persistence.entity.YadaOrderStatus;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Repository
@Transactional(readOnly = true)
public class YadaOrderDao {

    @PersistenceContext private EntityManager em;

    /**
     * Find a previously saved YadaOrderItem. Can be many if the user repeated the request.
     * @param owner
     * @param articleCode
     * @return
     */
    public List<YadaOrderItem> find(YadaUserProfile owner, String articleCode) {
    	return YadaSql.instance().selectFrom("select yoi from YadaOrderItem yoi")
    		.join("join yoi.order yo")
    		.where("yo.owner = :owner").and()
    		.where("yoi.articleCode = :articleCode")
    		.orderBy("yoi.modified desc")
    		.setParameter("owner", owner)
    		.setParameter("articleCode", articleCode)
    		.query(em, YadaOrderItem.class)
    		.getResultList();
    }

    /**
     * Delete unpaid orders after 20 days
     */
    @Transactional(readOnly = false)
    public void cleanup() {
    	cleanup(20); // 20 days expiration
    }

    /**
     * Delete unpaid orders after some time
     */
    @Transactional(readOnly = false)
    public void cleanup(int daysPassed) {
    	Date thePast = YadaUtil.addDays(new Date(), -daysPassed);
    	String sql = "select yo from YadaOrder yo "
    			+ "where yo.modified < :someDate and "
    			+ "yo.orderStatus = :someStatus";
    	List<YadaOrder> resultList = em.createQuery(sql, YadaOrder.class)
    			.setParameter("someDate", thePast)
    			.setParameter("someStatus", YadaOrderStatus.UNPAID.toYadaPersistentEnum())
    			.getResultList();
    	for (YadaOrder yadaOrder : resultList) {
    		em.remove(yadaOrder);
    	}
    }

    @Transactional(readOnly = false)
    public YadaOrderItem save(YadaOrderItem entity) {
    	if (entity.getId()==null) {
    		em.persist(entity);
    	} else {
    		entity = em.merge(entity);
    	}
    	return entity;
    }

    @Transactional(readOnly = false)
    public YadaOrder save(YadaOrder yadaOrder) {
    	if (yadaOrder.getId()==null) {
    		em.persist(yadaOrder);
    	} else {
    		yadaOrder = em.merge(yadaOrder);
    	}
    	return yadaOrder;
    }


}
