package net.yadaframework.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaClause;

@Transactional(readOnly = true) 
public interface YadaClauseRepository extends JpaRepository<YadaClause, Long> {

	List<YadaClause> findByName(String name);

	/**
	 * Ritorno la clausola con nome 'trattamentoDati' avente versione più alta
	 */
	// Devo usare una native perchè jpql non ha LIMIT :-(
	@Query(value="select * from YadaClause yc where yc.name='trattamentoDati' order by yc.clauseVersion desc limit 1", nativeQuery = true)
	YadaClause getTrattamentoDati();

	/**
	 * Ritorno la clausola con nome 'pubblicazioneSito' avente versione più alta
	 */
	// Devo usare una native perchè jpql non ha LIMIT :-(
	@Query(value="select * from YadaClause yc where yc.name='pubblicazioneSito' order by yc.clauseVersion desc limit 1", nativeQuery = true)
	YadaClause getPubblicazioneSito();
	
	@Query(value="select * from YadaClause yc where yc.name='pubblicazioneRaccolta' order by yc.clauseVersion desc limit 1", nativeQuery = true)
	YadaClause getPubblicazioneRaccolta(); 

}
