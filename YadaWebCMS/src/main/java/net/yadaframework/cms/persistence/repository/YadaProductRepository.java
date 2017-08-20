package net.yadaframework.cms.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.cms.persistence.entity.YadaProduct;

@Transactional(readOnly = true) 
public interface YadaProductRepository<T extends YadaProduct> extends JpaRepository<T, Long> {
	
}
