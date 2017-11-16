package net.yadaframework.components;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.persistence.repository.YadaJobDao;
import net.yadaframework.persistence.repository.YadaJobRepository;

/**
 * Job handling
 *
 */
@Service
public class YadaJobManager {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired private YadaJobScheduler yadaJobScheduler;

	@Autowired private YadaJobRepository yadaJobRepository;
    @Autowired private YadaJobDao yadaJobDao;
    @Autowired private YadaConfiguration config;
	@Autowired private TaskScheduler yadaJobSchedulerTaskScheduler; // Used only to schedule the YadaJobScheduler

	@PostConstruct
	public void init() throws Exception {
		// It is itself scheduled by a TaskScheduler configured in YadaAppConfig and run every config/yada/jobSchedulerPeriodMillis milliseconds
		log.debug("init called");
		long period = config.getYadaJobSchedulerPeriod();
		if (period>0) {
			// Using Guava to create a ListenableFuture: https://github.com/google/guava/wiki/ListenableFutureExplained
			ExecutorService executorService = Executors.newFixedThreadPool(config.getYadaJobSchedulerThreadPoolSize());
			// Disable any job that is already RUNNING but jobRecoverable is false
			yadaJobDao.setUnrecoverableJobState();
			// Recover any job that is still in the RUNNING state and its group is not paused
			List<YadaJob> recoverableJobs = yadaJobDao.getRecoverableJobs();
			for (YadaJob yadaJob : recoverableJobs) {
				yadaJob.setRecovered(true);
				yadaJobScheduler.runJob(yadaJob.getId());
			}
			// Scheduling the YadaJobScheduler according to the configured period
			yadaJobSchedulerTaskScheduler.scheduleAtFixedRate(yadaJobScheduler, new Date(System.currentTimeMillis() + 10000), period);
		} else {
			log.info("YadaJobScheduler not started");
		}
	}

	/**
	 * Activate the job so that it becomes available for the scheduler to start it. 
	 * The scheduled time is left unchanged unless null, when it is set to NOW (start ASAP).
	 * This method does nothing to a job that is already scheduled and in the ACTIVE state.
	 * @param yadaJob the job to start
	 * @return true if the job has been activated, false if it doesn't exist
	 */
	public boolean startJob(YadaJob yadaJob) {
		// If the job has been deleted, return false
		if (yadaJobRepository.findOne(yadaJob.getId())==null) {
			log.debug("Job {} not found in DB when activating", yadaJob);
			return false;
		}
		if (yadaJob.getJobScheduledTime()==null) {
			yadaJob.setJobScheduledTime(new Date());
		}
		yadaJob.activate();
		yadaJobRepository.save(yadaJob);
		return true;
	}
	
	/**
	 * Check if the job group has been paused
	 * @param jobGroup
	 * @return
	 */
	public boolean isJobGroupPaused(String jobGroup) {
		return yadaJobRepository.isJobGroupPaused(jobGroup)!=null;
	}
	
	/**
	 * Pause all jobs in the given group
	 * @param jobGroup
	 * @param interrupt true to interrupt a job that is in execution, false to let it complete before pausing
	 */
	public void pauseJobGroup(String jobGroup, boolean interrupt) {
		if (interrupt) {
			// There should be just one job in execution, but we look for many anyway
			List<YadaJob> running = yadaJobRepository.findByJobGroupAndState(jobGroup, YadaJobState.RUNNING.toYadaPersistentEnum(), null);
			for (YadaJob yadaJob : running) {
				yadaJobScheduler.interruptJob(yadaJob.getId());
			}
		}
		yadaJobRepository.setJobGroupPaused(jobGroup, true);
	}
	
	/**
	 * Resume jobs of the given group
	 * @param jobGroup
	 */
	public void resumeJobGroup(String jobGroup) {
		yadaJobRepository.setJobGroupPaused(jobGroup, false);
	}

	/**
	 * Removes the job from the database.
	 * @param yadaJob
	 */
	public void deleteJob(YadaJob yadaJob) {
		interruptAndPauseJob(yadaJob.getId());
		yadaJobDao.delete(yadaJob);
	}
	
	/**
	 * Returns all jobs for the given group that are in the ACTIVE/RUNNING state, ordered by state and scheduled time
	 * @param jobGroup
	 * @return
	 */
	public List<YadaJob> getAllActiveOrRunningJobs(String jobGroup) {
		List<YadaPersistentEnum<YadaJobState>> states = new ArrayList<>();
		states.add(YadaJobState.ACTIVE.toYadaPersistentEnum());
		states.add(YadaJobState.RUNNING.toYadaPersistentEnum());
		return yadaJobRepository.findByJobGroupAndStates(jobGroup, states);
	}
	
	/**
	 * Interrupt the job and make it PAUSED
	 * @param yadaJob
	 */
	public void interruptAndPauseJob(Long yadaJobId) {
		// FIRST set the state, then interrupt, otherwise the state might end up being ACTIVE because of concurrency
		yadaJobRepository.internalSetState(yadaJobId, YadaJobState.PAUSED.toYadaPersistentEnum());
		yadaJobScheduler.interruptJob(yadaJobId);
	}

	public void changeJobPriority(YadaJob yadaJob, int priority) {
		yadaJob.setJobPriority(priority);
		yadaJobRepository.save(yadaJob);
	}
	
	public void reschedule(YadaJob yadaJob, Date newScheduling) {
		yadaJob.setJobScheduledTime(newScheduling);
		yadaJobRepository.save(yadaJob);
	}

}
