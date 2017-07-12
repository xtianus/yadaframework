package net.yadaframework.persistence.repository;


import java.sql.Timestamp;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;

/**
 */
@Repository
@Transactional(readOnly = true) 
public class YadaJobDao {

    @Autowired private YadaConfiguration config;

    @PersistenceContext private EntityManager em;
    
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

    /**
     * Find job candidates: they must start before the end of the period, have no unsatisfied dependencies with other jobs, be ACTIVE and not groupPaused.
     * They are sorted first by priority then by start time.
     * @return
     */
    public List<? extends YadaJob> findJobsToRun() {
		long now = System.currentTimeMillis();
		long jobSchedulerPeriodMillis = config.getYadaJobSchedulerPeriod();
		Timestamp periodEnd = new Timestamp(now + jobSchedulerPeriodMillis);
		
		//		select yg from YadaJob yg
		//		left join yg.jobsMustBeActive jmba
		//		left join yg.jobsMustBeInactive jmbi
		//		left join yg.jobsMustComplete jmc
		//		where yg.jobStateObject.id = 1
		//		and yg.jobScheduledTime < :nextPeriod
		//		and (jmba is null or jmba.jobStateObject.id = 1)
		//		and (jmbi is null or jmbi.jobStateObject.id in (4, 5))
		//		and (jmc is null or jmc.jobStateObject.id = 4)
		//		order by yg.jobPriority desc, yg.jobScheduledTime asc
		
		@SuppressWarnings("unchecked")
		List<? extends YadaJob> jobsToRun = YadaSql.instance().selectFrom("select yg from YadaJob yg")
				.join("left join yg.jobsMustBeActive jmba")
				.join("left join yg.jobsMustBeInactive jmbi")
				.join("left join yg.jobsMustComplete jmc")
				// group must not be paused
				.where("yg.jobGroupPaused = false").and() 	
				// job must be ACTIVE
				.where("yg.jobStateObject.id = :jobStateActive").and() 	
				// must start before the next period
				.where("yg.jobScheduledTime < :nextPeriod").and() 						
				// must obey the job precedence if any
				.where("(jmba is null or jmba.jobStateObject.id = :jobStateActive)").and()
				.where("jmbi is null or jmbi.jobStateObject.id in :jobStateInactive").and()
				.where("jmc is null or jmc.jobStateObject.id = :jobStateCompleted")
				// high priority first, ordered by start time
				.orderBy("order by yg.jobPriority desc, yg.jobScheduledTime asc")
				.setParameter("nextPeriod", periodEnd)
				.setParameter("jobStateActive", YadaJobState.ACTIVE.toId())
				.setParameter("jobStateInactive", new Long[] {YadaJobState.COMPLETED.toId(), YadaJobState.DISABLED.toId()})
				.setParameter("jobStateCompleted", YadaJobState.COMPLETED.toId())
				.query(em).getResultList();
		return jobsToRun;
    }

    /**
     * Change the state of all jobs in the group
     * @param jobGroup
     * @param fromState
     * @param toState
     */
	public void changeAllStates(String jobGroup, YadaJobState fromState, YadaJobState toState) {
		YadaSql.instance().set("set "); //////////////// TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		
		
	}
	
}
