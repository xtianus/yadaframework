package net.yadaframework.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaAttachedFile;

@Transactional(readOnly = true) 
public interface YadaAttachedFileRepository extends JpaRepository<YadaAttachedFile, Long> {

	/**
	 * Swaps the sortOrder of two YadaAttachedFile entities
	 * @param oneId
	 * @param anotherId
	 */
	@Query(value="update YadaAttachedFile yaf inner join YadaAttachedFile yaf2 on yaf.id != yaf2.id "
		+ "set yaf.sortOrder = yaf2.sortOrder, yaf2.sortOrder = yaf.sortOrder "
		+ "where yaf.id in (:oneId,:anotherId) and yaf2.id in (:oneId,:anotherId);", nativeQuery=true)
	@Transactional(readOnly = false) 
	@Modifying
	void swapSortOrder(@Param("oneId") long oneId, @Param("anotherId") long anotherId);
	
}
