package net.yadaframework.persistence.repository;


import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.web.YadaPageRequest;

/**
 */
@Repository
@Transactional(readOnly = true) 
public class YadaJobDao {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext private EntityManager em;
    
    /**
     * Set the state of a job in the database. If the job does not exist, nothing happens.
     * @param yadaJobId
     * @param yadaJobState
     */
    @Transactional(readOnly = false)
    public void setState(Long yadaJobId, YadaJobState yadaJobState) {
    	String sql = "update YadaJob yj set yj.jobStateObject_id = :yadaJobStateId where yj.id = :jobId";
    	em.createNativeQuery(sql)
    		.setParameter("yadaJobStateId", yadaJobState.toYadaPersistentEnum().getId())
    		.setParameter("jobId", yadaJobId)
    		.executeUpdate();
    	
		//    	String sql = "update YadaJob yj set yj.jobStateObject_id = ("
		//			+ "SELECT id FROM YadaPersistentEnum ype where ype.enumClassName=:enumClass and "
		//			+ "ype.enumOrdinal=:enumOrdinal limit 1) "
		//			+ "where yj.id = :jobId";
		//    	em.createNamedQuery(sql)
		//    		.setParameter("enumClass", yadaJobState.getClass().getName())
		//    		.setParameter("enumOrdinal", yadaJobState.ordinal())
		//    		.setParameter("jobId", yadaJobId)
		//    		.executeUpdate();
    }
    
    /**
    * Deletes a job
    * @param long1
    */
   @Transactional(readOnly = false)
   public void delete(Long yadaJobId) {
   	// Natively delete job associations (quicker than JPA)
      	YadaSql.instance()
      		.selectFrom("delete from YadaJob_BeActive")
   		.where("where YadaJob_id = :id or jobsMustBeActive_id = :id")
   		.setParameter("id", yadaJobId)
   		.nativeQuery(em)
   		.executeUpdate();
      	YadaSql.instance()
	       	.selectFrom("delete from YadaJob_BeCompleted")
	       	.where("where YadaJob_id = :id or jobsMustComplete_id = :id")
	       	.setParameter("id", yadaJobId)
	       	.nativeQuery(em)
	       	.executeUpdate();
      	YadaSql.instance()
	       	.selectFrom("delete from YadaJob_BeInactive")
	       	.where("where YadaJob_id = :id or jobsMustBeInactive_id = :id")
	       	.setParameter("id", yadaJobId)
	       	.nativeQuery(em)
	       	.executeUpdate();
      	// JPA-delete so that any joined application subclasses (which are obviously unknown when writing this) are deleted too
      	// Better to reload the entity because the argument might come from another entitymanager (?!)
      	YadaJob deletable = em.find(YadaJob.class, yadaJobId);
      	em.remove(deletable);
   }
    
//    /**
//     * If the job is RUNNING, set the state to ACTIVE
//     * @param yadaJob
//     */
//    @Transactional(readOnly = false)
//    public void setActiveWhenRunning(YadaJob yadaJob) {
//    	// Wrong:
//    	YadaSql.instance().updateSet("update YadaJob yg join yg. set yg.jobStateObject = :active")
//		.where("where yg = :job and yg.jobStateObject = :running")
//		.setParameter("active", YadaJobState.ACTIVE.toYadaPersistentEnum())
//		.setParameter("job", yadaJob)
//		.setParameter("running", YadaJobState.RUNNING.toYadaPersistentEnum())
//		.query(em).executeUpdate();
//    }
    
    /**
     * Return the list of jobs that can be recovered after a crash.
     * @return
     */
    @SuppressWarnings("unchecked")
	public List<YadaJob> getRecoverableJobs() {
    	return YadaSql.instance().selectFrom("select yg from YadaJob yg left join yg.jobStateObject")
    		.where("where yg.jobGroupPaused = false and yg.jobRecoverable = true and yg.jobStateObject = :running")
    		.setParameter("running", YadaJobState.RUNNING.toYadaPersistentEnum())
    		.query(em).getResultList();
    }
    
	/**
	 * Disable all RUNNING jobs that are not recoverable and not group-paused. Called at server startup.
	 * @param stateObject
	 */
    @Transactional(readOnly = false)
	public void setUnrecoverableJobState() {
		YadaSql.instance().updateSet("update YadaJob yg set yg.jobStateObject = :disabled")
		.where("where yg.jobGroupPaused = false and yg.jobRecoverable = false and yg.jobStateObject = :running")
		.setParameter("disabled", YadaJobState.DISABLED.toYadaPersistentEnum())
		.setParameter("running", YadaJobState.RUNNING.toYadaPersistentEnum())
		.query(em).executeUpdate();
	}

//  /**
//  * TODO Change the state of all jobs in the group
//  * @param jobGroup
//  * @param fromState
//  * @param toState
//  */
//	public void changeAllStates(String jobGroup, YadaJobState fromState, YadaJobState toState) {
//		YadaSql.instance().set("set "); //////////////// TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//		
//		
//	}
	
	/**
	 * Get the current job state from the database
	 * @param yadaJobId
	 * @return
	 */
	public YadaPersistentEnum<YadaJobState> getJobState(Long yadaJobId) {
		String sql = "select job.jobStateObject from YadaJob job where id=:yadaJobId";
		try {
			YadaPersistentEnum<YadaJobState> result = (YadaPersistentEnum<YadaJobState>) em.createQuery(sql)
				.setMaxResults(1)
				.setParameter("yadaJobId", yadaJobId)
				.getSingleResult();
			return result;
		} catch (NonUniqueResultException | NoResultException e) {
			log.debug("No state found for jobId = {}", yadaJobId);
			return null;
		}
	}

	/**
	 * Invoked by the scheduler classes when starting a job. Only an active job can become running
	 * @param yadaJobId the id of the job
	 * @param fromId the id of the state enum that the job must have in order to be changed
	 * @param toId the id of the state enum to assign
	 * 
	 */
	@Transactional(readOnly = false)
	public void stateChangeFromTo(Long yadaJobId, YadaPersistentEnum<YadaJobState> fromStateEnum, YadaPersistentEnum<YadaJobState> toStateEnum) {
		String sql = "update YadaJob set jobStateObject=:toStateEnum where id=:yadaJobId and jobStateObject=:fromStateEnum";
		em.createQuery(sql)
			.setParameter("yadaJobId", yadaJobId)
			.setParameter("fromStateEnum", fromStateEnum)
			.setParameter("toStateEnum", toStateEnum)
			.executeUpdate();
	}
	
	/**
	 * Invoked by the scheduler classes when starting a job. Only an active job can become running
	 * @param yadaJob the job
	 * @param fromState the state enum that the job must have in order to be changed
	 * @param toState the state enum to assign
	 * 
	 */
	@Transactional(readOnly = false)
	public void stateChangeFromTo(YadaJob yadaJob, YadaJobState fromStateObject, YadaJobState toStateObject) {
		stateChangeFromTo(yadaJob.getId(), fromStateObject.toYadaPersistentEnum(), toStateObject.toYadaPersistentEnum());
	}
	
	/**
	 * Returns the start time of a job
	 * @param yadaJobId
	 * @return might be null when not running or not existing
	 */
	public Date getJobStartTime(Long yadaJobId) {
		String sql = "select job.jobStartTime from YadaJob job where id=:yadaJobId";
		try {
			Date result = em.createQuery(sql, Date.class)
				.setMaxResults(1)
				.setParameter("yadaJobId", yadaJobId)
				.getSingleResult();
			return result;
		} catch (NonUniqueResultException | NoResultException e) {
			log.debug("No date found for jobId = {}", yadaJobId);
			return null;
		}
	}
	
	/**
	 * Check if the job group has been paused. It is considered paused if at least one job in the group has the jobGroupPaused set
	 * @param jobGroup
	 * @return 1 if the group is paused, null otherwise.
	 */
	public Integer isJobGroupPaused(String jobGroup) {
		String sql = "select 1 from YadaJob e where e.jobGroup = :jobGroup and e.jobGroupPaused = true limit 1";
		try {
			return (Integer) em.createNativeQuery(sql).setParameter("jobGroup", jobGroup).getSingleResult();
		} catch (NonUniqueResultException | NoResultException e) {
			// The job is either paused or not found
			return null;
		}

	}

	/**
	 * Set the pause flag on all jobs of a jobGroup
	 * @param jobGroup
	 * @param paused
	 */
	@Transactional(readOnly = false)
	public void setJobGroupPaused(String jobGroup, boolean paused) {
		String sql = "update YadaJob yj set yj.jobGroupPaused = :paused where yj.jobGroup = :jobGroup";
		em.createNativeQuery(sql)
			.setParameter("jobGroup", jobGroup)
			.setParameter("paused", paused)
			.executeUpdate();
	}

	/**
	 * Returns all jobs for the given group that are in the given state
	 * @param jobGroup the job group
	 * @param stateObject the job state
	 * @param pageable can be null for all results
	 * @return
	 */
	public List<YadaJob> findByJobGroupAndState(String jobGroup, YadaPersistentEnum<YadaJobState> stateObject, YadaPageRequest pageable) {
		String sql = "select e from YadaJob e join e.jobStateObject where e.jobGroup=:jobGroup and e.jobStateObject = :stateObject";
		TypedQuery<YadaJob> query = em.createQuery(sql, YadaJob.class)
			.setParameter("jobGroup", jobGroup)
			.setParameter("stateObject", stateObject);
		if (pageable!=null && pageable.isValid()) {
			query.setFirstResult(pageable.getFirstResult()).setMaxResults(pageable.getSize());
		}
		return query.getResultList();
	}

	/**
	 * Returns the running job for the given group if any
	 * @param jobGroup
	 * @return the running job or null
	 */
	public YadaJob findRunning(String jobGroup) {
		List<YadaJob> result = findByJobGroupAndState(jobGroup, YadaJobState.RUNNING.toYadaPersistentEnum(), YadaPageRequest.of(0, 1));
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
	public long countByJobGroupAndStates(String jobGroup, Collection<YadaPersistentEnum<YadaJobState>> stateObjects) {
		String sql = "select count(*) from YadaJob e join e.jobStateObject where e.jobGroup=:jobGroup and e.jobStateObject in :stateObjects";
		try {
			return (long) em.createQuery(sql)
				.setParameter("jobGroup", jobGroup)
				.setParameter("stateObjects", stateObjects)
				.getSingleResult();
		} catch (NonUniqueResultException | NoResultException e) {
			log.error("Count of jobs failed", e);
			return 0;
		}
	}
	
	/**
	 * Returns all jobs for the given group, that are in one of the given states
	 * @param jobGroup
	 * @param stateObjects a collection of job states
	 * @return
	 */
	public List<YadaJob> findByJobGroupAndStates(String jobGroup, Collection<YadaPersistentEnum<YadaJobState>> stateObjects) {
		String sql = "select e from YadaJob e join e.jobStateObject where e.jobGroup=:jobGroup and e.jobStateObject in :stateObjects order by e.jobStateObject.enumOrdinal desc, e.jobScheduledTime asc";
		return em.createQuery(sql, YadaJob.class)
			.setParameter("jobGroup", jobGroup)
			.setParameter("stateObjects", stateObjects)
			.getResultList();
	}

	/**
	 * 
	 * @param jobId
	 * @param startTime
	 */
	@Transactional(readOnly = false)
	public void setStartTime(long jobId, Date startTime) {
		String sql = "update YadaJob set jobStartTime=:startTime where id=:jobId";
		em.createQuery(sql)
			.setParameter("jobId", jobId)
			.setParameter("startTime", startTime)
			.executeUpdate();
	}
	
	@Transactional(readOnly = false)
	public YadaJob save(YadaJob entity) {
		if (entity==null) {
			return null;
		}
		if (entity.getId()==null) {
			em.persist(entity);
			return entity;
		}
		return em.merge(entity);
	}
	
	// Kept for compatibility with Spring Data Repository
	public Optional<YadaJob> findById(Long entityId) {
		YadaJob result = em.find(YadaJob.class, entityId);
		return  Optional.ofNullable(result);
	}	
	
}
