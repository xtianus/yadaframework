package net.yadaframework.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaAttachedFile;

@Transactional(readOnly = true) 
public interface YadaAttachedFileRepository extends JpaRepository<YadaAttachedFile, Long> {


}