package net.yadaframework.cms.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.cms.persistence.entity.YadaArticle;

@Transactional(readOnly = true) 
public interface YadaArticleRepository<T extends YadaArticle> extends JpaRepository<T, Long> {
	
}
