package net.yadaframework.persistence.repository;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaJobState;

/**
 * Takes care of running and managing YadaJob instances.
 * It is itself scheduled by a TaskScheduler configured in YadaAppConfig and run every config/yada/jobSchedulerPeriodMillis milliseconds
 * @see YadaConfiguration#getJobSchedulerPeriod()
 */
@Service
@Repository
public class YadaJobScheduler implements Runnable {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaConfiguration config;
    @PersistenceContext EntityManager em;
    
	@Autowired private TaskScheduler taskScheduler;
	
	@PostConstruct
	public void init() throws Exception {
		log.debug("init called");
		long period = config.getJobSchedulerPeriod();
		if (period>0) {
			taskScheduler.scheduleAtFixedRate(this, new Date(), period);
		}
		// TODO allo startup del server bisogna cercare i job in stato RUNNING
		// (insieme a quelli che stanno per partire) avevnti il flag jobRecoverable=true, mentre quelli che
		// l'hanno false vengono messi a disabled.
		// Quando un job è recovered, viene chiamato un suo metodo recover() che si occupa di capire se deve completare il run precedente
		// e decide quando schedularsi di nuovo.
	}
	
	@Override
	public void run() {
		log.debug("RUN");
		startJobs();
	}
	
	/** 
	 * Run the jobs that are scheduled to run in the next X seconds.
	 */
	@Transactional(readOnly = true) 
	private void startJobs() {
		long now = System.currentTimeMillis();
		long jobSchedulerPeriodMillis = config.getJobSchedulerPeriod();
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

		List<?> jobsToRun = YadaSql.instance().selectFrom("select yg from YadaJob yg")
			.join("left join yg.jobsMustBeActive jmba")
			.join("left join yg.jobsMustBeInactive jmbi")
			.join("left join yg.jobsMustComplete jmc")
			// must be ACTIVE
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
			log.debug("Found {} jobs to run", jobsToRun.size());
		
	}
}
