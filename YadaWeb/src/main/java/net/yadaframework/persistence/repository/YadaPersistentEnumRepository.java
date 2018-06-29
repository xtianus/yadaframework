package net.yadaframework.persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaBrowserId;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

@Transactional(readOnly = true) 
public interface YadaPersistentEnumRepository extends JpaRepository<YadaPersistentEnum, Long> {
	

}
