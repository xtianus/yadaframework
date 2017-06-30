package net.yadaframework.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaJob;

@Transactional(readOnly = true) 
public interface YadaJobRepository extends JpaRepository<YadaJob, Long> {


}
