package net.yadaframework.core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class YadaJobScheduler implements Runnable {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaConfiguration config;
	
	@Override
	public void run() {
		log.debug("RUN");
		startJobs();
	}
	
	// TODO allo startup del server bisogna cercare i job in stato RUNNING
	// (insieme a quelli che stanno per partire) avevnti il flag jobRecoverable=true, mentre quelli che
	// l'hanno false vengono messi a disabled.
	// Quando un job è recovered, viene chiamato un suo metodo recover() che si occupa di capire se deve completare il run precedente
	// e decide quando schedularsi di nuovo.
	
	/** 
	 * Run the jobs that are scheduled to run in the next X seconds.
	 */
	private void startJobs() {
		// Find all YadaJob that are ACTIVE,
		// where the jobScheduledTime is before (jobScheduledTime + getJobSchedulerPeriod)
		// sorted by jobPriority desc and jobScheduledTime asc 
		long jobSchedulerPeriodMillis = config.getJobSchedulerPeriod();
		
//		Trova tutti i job che non sono vincolati a delle condizioni su altri job
//		select * from YadaJob yg 
//		left join YadaJob mba ON mba.id = yg.jobMustBeActive_id
//		left join YadaJob mc ON mc.id = yg.jobMustComplete_id
//		left join YadaJob mnba ON mc.id = yg.jobMustNotBeActive_id
//		where 
//		   (yg.jobMustBeActive_id is null or mba.persistentJobState_id=1)
//		and (yg.jobMustComplete_id is null or mc.persistentJobState_id=4)
//		and (yg.jobMustNotBeActive_id is null or (mnba.persistentJobState_id!=1 and mnba.persistentJobState_id!=2))
//		;
		
	}
}
