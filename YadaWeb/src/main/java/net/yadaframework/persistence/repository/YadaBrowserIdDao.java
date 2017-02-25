package net.yadaframework.persistence.repository;


import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaBrowserId;

/**
 * 
 */
@Repository
@Transactional(readOnly = true) 
public class YadaBrowserIdDao {

    @PersistenceContext EntityManager em;
    
    @Autowired YadaBrowserIdRepository yadaBrowserIdRepository;

    /**
     * Find or create the instance with the given UUID
     * @param uuid
     * @return
     */
    @Transactional(readOnly = false) 
    public YadaBrowserId findOrCreate(UUID uuid) {
    	YadaBrowserId yadaBrowserId = yadaBrowserIdRepository.findByMostSigBitsAndLeastSigBits(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    	if (yadaBrowserId==null) {
    		yadaBrowserId = new YadaBrowserId();
    		yadaBrowserId.setUUID(uuid);
    		yadaBrowserId = yadaBrowserIdRepository.save(yadaBrowserId);
    	}
    	return yadaBrowserId;
    }
	
}
