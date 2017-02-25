package net.yadaframework.persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaBrowserId;

@Transactional(readOnly = true) 
public interface YadaBrowserIdRepository extends JpaRepository<YadaBrowserId, Long> {
	
	/**
	 * Find the instance with the given value
	 * @param mostSigBits
	 * @param leastSigBits
	 * @return
	 */
	YadaBrowserId findByMostSigBitsAndLeastSigBits(long mostSigBits, long leastSigBits);

}
