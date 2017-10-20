package net.yadaframework.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

@Transactional(readOnly = true) 
public interface YadaJobRepository extends JpaRepository<YadaJob, Long> {
	
	/**
	 * Check if the job group has been paused. It is considered paused if at least one job in the group has the jobGroupPaused set
	 * @param jobGroup
	 * @return 1 if the group is paused, null otherwise.
	 */
	@Query(value="select 1 from YadaJob e where e.jobGroup = :jobGroup and e.jobGroupPaused = true limit 1", nativeQuery=true)
	Integer isJobGroupPaused(@Param("jobGroup") String jobGroup);

	/**
	 * Set the pause flag on all jobs of a jobGroup
	 * @param jobGroup
	 * @param paused
	 */
	@Modifying
	@Transactional(readOnly = false)
	@Query("update #{#entityName} e set e.jobGroupPaused = :paused where e.jobGroup = :jobGroup")
	void setJobGroupPaused(@Param("jobGroup") String jobGroup, @Param("paused") boolean paused);

	List<YadaJob> findByJobsMustBeActiveContains(YadaJob jadaJob);

	List<YadaJob> findByJobsMustBeInactiveContains(YadaJob jadaJob);
	
	List<YadaJob> findByJobsMustCompleteContains(YadaJob jadaJob);

	/**
	 * Returns all jobs for the given group that are in the given state
	 * @param jobGroup the job group
	 * @param stateObject the job state
	 * @param pageable can be null
	 * @return
	 */
	@Query("select e from #{#entityName} e join e.jobStateObject where e.jobGroup=:jobGroup and e.jobStateObject = :stateObject")
	List<YadaJob> findByJobGroupAndState(@Param("jobGroup") String jobGroup, @Param("stateObject") YadaPersistentEnum<YadaJobState> stateObject, Pageable pageable);

	/**
	 * Returns the running job for the given group if any
	 * @param jobGroup
	 * @return the running job or null
	 */
	default YadaJob findRunning(@Param("jobGroup") String jobGroup) {
		List<YadaJob> result = findByJobGroupAndState(jobGroup, YadaJobState.RUNNING.toYadaPersistentEnum(), new PageRequest(0,1));
		if (result.size()==1) {
			return result.get(0);
		}
		return null;
	}
	
	/**
	 * Returns all jobs for the given group, that are in one of the given states
	 * @param jobGroup
	 * @param stateObjects a collection of job states
	 * @return
	 */
	@Query("select e from #{#entityName} e join e.jobStateObject where e.jobGroup=:jobGroup and e.jobStateObject in :stateObjects order by e.jobStateObject.enumOrdinal desc, e.jobScheduledTime asc")
	List<YadaJob> findByJobGroupAndStates(@Param("jobGroup") String jobGroup, @Param("stateObjects") Collection<YadaPersistentEnum<YadaJobState>> stateObjects);

}
