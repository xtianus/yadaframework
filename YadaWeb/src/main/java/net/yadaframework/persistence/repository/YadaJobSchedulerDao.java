package net.yadaframework.persistence.repository;


import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;

/**
 * This class is used internally by the YadaJobScheduler and should never be used directly.
 */
//Note: I'm using Long instead of YadaJob to avoid "Row was updated or deleted by another transaction" errors
@Repository
@Transactional(readOnly = true) 
public class YadaJobSchedulerDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext private EntityManager em;
    
    @Autowired private YadaConfiguration config;
    @Autowired private YadaJobDao yadaJobDao;
    @Autowired private YadaUtil yadaUtil;
    
//    /**
//     * Do not use directly.
//     */
//	//    * Sets the state of a job directly in the database
//	//    * @param yadaJobId
//	//    * @param state
//    public void internalSetState(Long yadaJobId, YadaJobState state) {
////    	YadaSql.instance().updateSet("update YadaJob y set y.jobStateObject_id = :stateId")
//    	YadaSql.instance().updateSet("update YadaJob y set y.jobStateObjec = :state")
//		.where("where y.id = :yadaJobId")
//		.setParameter("state", state.toYadaPersistentEnum())
//		.setParameter("yadaJobId", yadaJobId)
//		.query(em).executeUpdate();
//    }

    /**
     * Do not use directly.
     */
    // Called by the YadaJobScheduler when a job fails
    @Transactional(readOnly = false)
	public void internalJobFailed(YadaJob yadaJob, Throwable thrown) {
		log.debug("Job id {} ended onFailure", yadaJob.getId(), thrown);
		try {
			yadaJob = em.merge(yadaJob); // Saves any changes to db when exiting the method
			yadaJob.incrementErrorStreak();
			// If not scheduled in the future, run again in 1 minute
			Date inOneMinute = new Date(System.currentTimeMillis()+60000); 
			if (yadaJob.getJobScheduledTime().before(inOneMinute)) {
				yadaJob.setJobScheduledTime(inOneMinute);
			}
			yadaJob.setJobStartTime(null); // The time at which the job was started - null if the job is not in the RUNNING state.
			if (yadaJob.isRunning()) {
				// A failed job will still be run next time, unless someone disables it
				yadaJob.activate(); // Ready for the next run
			}
		} catch (Exception e) {
			log.error("internalJobFailed failed to update Job id {} with db version {}", yadaJob.getId(), yadaJob.getVersion(), e);
		}
	}
	
    /**
     * Do not use directly.
     */
    // Called by the YadaJobScheduler when a job completes
    @Transactional(readOnly = false)
    public void internalJobSuccessful(YadaJob yadaJob) {
    	log.debug("Job id {} ended successfully", yadaJob.getId());
		try {
	    	yadaJob = em.merge(yadaJob); // Saves any changes to db when exiting the method
			yadaJob.setJobLastSuccessfulRun(new Date());
			yadaJob.setErrorStreakCount(0);
			yadaJob.setJobStartTime(null); // The time at which the job was started - null if the job is not in the RUNNING state.
			if (yadaJob.isRunning()) {
				yadaJob.activate(); // Ready for the next run
			}
		} catch (Exception e) {
			log.error("internalJobSuccessful failed to update Job id {} with db version {}", yadaJob.getId(), yadaJob.getVersion(), e);
		}
	}   
    
    /**
     * Do not use directly.
     */
	// Find job candidates: they must start before the end of the period, have no unsatisfied dependencies with other jobs, be ACTIVE and not groupPaused.
	// They are sorted first by priority then by start time.
    public List<? extends YadaJob> internalFindJobsToRun() {
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



}
