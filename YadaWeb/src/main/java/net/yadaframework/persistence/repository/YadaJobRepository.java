package net.yadaframework.persistence.repository;

import java.util.Date;
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
	 * Get the current job state from the database
	 * @param yadaJobId
	 * @return
	 */
	@Query("select job.jobStateObject from YadaJob job where id=:id")
	YadaPersistentEnum<YadaJobState> getJobState(@Param("id") Long yadaJobId);
	
//	/**
//	 * Invoked by the scheduler classes to set the job state.
//	 * @param yadaJobId
//	 * @param jobStateId
//	 */
//	@Modifying
//	@Transactional(readOnly = false)
//	@Query("update YadaJob set jobStateObject=:stateObject where id=:id")
//	void setState(@Param("id") Long yadaJobId, @Param("stateObject") YadaPersistentEnum<YadaJobState> stateObject);
	
//	/**
//	 * Toggle between two states. No change is done if the state is another.
//	 * @param yadaJobId
//	 * @param aStateEnum
//	 * @param bStateEnum
//	 */
//	@Modifying
//	@Transactional(readOnly = false)
//	@Query("update YadaJob y set y.jobStateObject = CASE y.jobStateObject "
//		+ "WHEN :aStateEnum THEN :bStateEnum WHEN :bStateEnum THEN :aStateEnum END where y.id=:id")
//	void stateToggleBetween(@Param("id") Long yadaJobId, @Param("aStateEnum") YadaPersistentEnum<YadaJobState> aStateEnum, @Param("bStateEnum") YadaPersistentEnum<YadaJobState> bStateEnum);

	/**
	 * Invoked by the scheduler classes when starting a job. Only an active job can become running
	 * @param yadaJobId the id of the job
	 * @param fromId the id of the state enum that the job must have in order to be changed
	 * @param toId the id of the state enum to assign
	 * 
	 */
	@Modifying
	@Transactional(readOnly = false)
	@Query("update YadaJob set jobStateObject=:toStateEnum where id=:id and jobStateObject=:fromStateEnum")
	void stateChangeFromTo(@Param("id") Long yadaJobId, @Param("fromStateEnum") YadaPersistentEnum<YadaJobState> fromStateEnum, @Param("toStateEnum") YadaPersistentEnum<YadaJobState> toStateEnum);
	
	/**
	 * Invoked by the scheduler classes when starting a job. Only an active job can become running
	 * @param yadaJob the job
	 * @param fromState the state enum that the job must have in order to be changed
	 * @param toState the state enum to assign
	 * 
	 */
	@Modifying
	@Transactional(readOnly = false)
	default void stateChangeFromTo(YadaJob yadaJob, YadaJobState fromStateObject, YadaJobState toStateObject) {
		stateChangeFromTo(yadaJob.getId(), fromStateObject.toYadaPersistentEnum(), toStateObject.toYadaPersistentEnum());
	}
	
	/**
	 * Returns the start time of a job
	 * @param yadaJobId
	 * @return might be null when not running or not existing
	 */
	@Query("select job.jobStartTime from YadaJob job where id=:id")
	Date getJobStartTime(@Param("id") Long yadaJobId);
	
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
	@Query(value="update YadaJob yj set yj.jobGroupPaused = :paused where yj.jobGroup = :jobGroup", nativeQuery=true)
	void setJobGroupPaused(@Param("jobGroup") String jobGroup, @Param("paused") boolean paused);

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
		List<YadaJob> result = findByJobGroupAndState(jobGroup, YadaJobState.RUNNING.toYadaPersistentEnum(), PageRequest.of(0, 1));
		if (result.size()==1) {
			return result.get(0);
		}
		return null;
	}
	
	/**
	 * Returns the number of jobs for the given group, that are in one of the given states
	 * @param jobGroup
	 * @param stateObjects a collection of job states
	 * @return
	 */
	@Query("select count(*) from #{#entityName} e join e.jobStateObject where e.jobGroup=:jobGroup and e.jobStateObject in :stateObjects")
	int countByJobGroupAndStates(@Param("jobGroup") String jobGroup, @Param("stateObjects") Collection<YadaPersistentEnum<YadaJobState>> stateObjects);
	
	/**
	 * Returns all jobs for the given group, that are in one of the given states
	 * @param jobGroup
	 * @param stateObjects a collection of job states
	 * @return
	 */
	@Query("select e from #{#entityName} e join e.jobStateObject where e.jobGroup=:jobGroup and e.jobStateObject in :stateObjects order by e.jobStateObject.enumOrdinal desc, e.jobScheduledTime asc")
	List<YadaJob> findByJobGroupAndStates(@Param("jobGroup") String jobGroup, @Param("stateObjects") Collection<YadaPersistentEnum<YadaJobState>> stateObjects);

	/**
	 * 
	 * @param jobId
	 * @param startTime
	 */
	@Modifying
	@Transactional(readOnly = false)
	@Query("update YadaJob set jobStartTime=:startTime where id=:id")
	void setStartTime(@Param("id") long jobId, @Param("startTime") Date startTime);

}
