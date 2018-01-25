package net.yadaframework.components;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.persistence.repository.YadaJobDao;
import net.yadaframework.persistence.repository.YadaJobRepository;

/**
 * Job handling.
 * Use this class to add/delete/remove jobs. Do not use the YadaJobScheduler directly.
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
	
	private AtomicBoolean initialized = new AtomicBoolean(false);
	
	/**
	 * Start the scheduler.
	 * Method called after Spring has finished initializing the Application Context, so that YadaPersistentEnums have been initialized.
	 * @param event
	 * @throws Exception
	 */
	@EventListener
	public void init(ContextRefreshedEvent event) {
		if (initialized.getAndSet(true)) {
			return;
		}
		// It is itself scheduled by a TaskScheduler configured in YadaAppConfig and run every config/yada/jobSchedulerPeriodMillis milliseconds
		log.debug("init called");
		long period = config.getYadaJobSchedulerPeriod();
		if (period>0) {
			// Disable any job that is already RUNNING but jobRecoverable is false
			yadaJobDao.setUnrecoverableJobState();
			// Recover any job that is still in the RUNNING state and its group is not paused
			List<YadaJob> recoverableJobs = yadaJobDao.getRecoverableJobs();
			for (YadaJob yadaJob : recoverableJobs) {
				yadaJob.setRecovered(true);
				yadaJob.setJobStartTime(new Date()); // Needed to prevent stale cleaning
				yadaJobRepository.save(yadaJob);
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
		// Shouldn't set the state by calling setJobStateObject() but we don't care here because we want to force our value onto the database
		yadaJob.setJobStateObject(YadaJobState.ACTIVE.toYadaPersistentEnum());
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
		pauseAndInterruptJob(yadaJob.getId());
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
	 * Set a list of jobs to DISABLED then interrupt the threads.
	 * All jobs are first set to disabled, then they are all interrupted.
	 * @param yadaJob
	 */
	public void disableAndInterruptJobs(List<? extends YadaJob> yadaJobs) {
		for (YadaJob yadaJob : yadaJobs) {
			yadaJobRepository.setState(yadaJob.getId(), YadaJobState.DISABLED.toYadaPersistentEnum());
		}
		for (YadaJob yadaJob : yadaJobs) {
			yadaJobScheduler.interruptJob(yadaJob.getId());
		}
	}
	
	/**
	 * Set a job to COMPLETED
	 * @param yadaJob
	 */
	public void completeJob(Long yadaJobId) {
		yadaJobRepository.setState(yadaJobId, YadaJobState.COMPLETED.toYadaPersistentEnum());
	}
	
	/**
	 * Toggle between paused and disabled.
	 * @param yadaJob
	 */
	public void toggleDisabledAndPaused(Long yadaJobId) {
		yadaJobRepository.stateToggleBetween(yadaJobId, YadaJobState.PAUSED.toYadaPersistentEnum(), YadaJobState.DISABLED.toYadaPersistentEnum());
	}
	
	/**
	 * Set a job to DISABLED then interrupt its thread
	 * @param yadaJob
	 */
	public void disableAndInterruptJob(Long yadaJobId) {
		yadaJobRepository.setState(yadaJobId, YadaJobState.DISABLED.toYadaPersistentEnum());
		yadaJobScheduler.interruptJob(yadaJobId);
	}
	
	/**
	 * Set a job to PAUSED then interrupt its thread
	 * @param yadaJob
	 */
	public void pauseAndInterruptJob(Long yadaJobId) {
		yadaJobRepository.setState(yadaJobId, YadaJobState.PAUSED.toYadaPersistentEnum());
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
