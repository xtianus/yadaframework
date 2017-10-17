package net.yadaframework.security.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.entity.YadaTicket;

@Transactional(readOnly = true) 
public interface YadaTicketRepository extends JpaRepository<YadaTicket, Long> {

}
