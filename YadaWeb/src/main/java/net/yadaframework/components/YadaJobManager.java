package net.yadaframework.components;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
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
				yadaJobScheduler.runJob(yadaJob);
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
	 * @return true if the job has been activated, false if it doesn't exist in the database
	 */
	public boolean startJob(YadaJob yadaJob) {
		// If the job has been deleted, return false
		if (!yadaJobRepository.findById(yadaJob.getId()).isPresent()) {
			log.debug("Job {} not found in DB when activating", yadaJob);
			return false;
		}
		if (yadaJob.getJobScheduledTime()==null) {
			yadaJob.setJobScheduledTime(new Date());
		}
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
	 * Returns the number of jobs for the given group that are in the ACTIVE/RUNNING state 
	 * @param jobGroup
	 * @return
	 */
	public int countActiveOrRunningJobs(String jobGroup) {
		List<YadaPersistentEnum<YadaJobState>> states = new ArrayList<>();
		states.add(YadaJobState.ACTIVE.toYadaPersistentEnum());
		states.add(YadaJobState.RUNNING.toYadaPersistentEnum());
		return yadaJobRepository.countByJobGroupAndStates(jobGroup, states);
	}
	
	/**
	 * Returns all jobs for the given group that are in the ACTIVE/RUNNING state, ordered by state and scheduled time.
	 * The running jobs are the actual cached instances present in the YadaJobScheduler.
	 * @param jobGroup
	 * @return
	 */
	public List<YadaJob> getAllActiveOrRunningJobs(String jobGroup) {
		List<YadaPersistentEnum<YadaJobState>> states = new ArrayList<>();
		states.add(YadaJobState.ACTIVE.toYadaPersistentEnum());
		states.add(YadaJobState.RUNNING.toYadaPersistentEnum());
		List<YadaJob> result = yadaJobRepository.findByJobGroupAndStates(jobGroup, states);
		// If a job is running, replace its instance with the cached one
		ListIterator<YadaJob> iter = result.listIterator();
		while (iter.hasNext()) {
			YadaJob yadaJob = iter.next();
			if (yadaJob.isRunning()) {
				YadaJob cached = yadaJobScheduler.getJobInstanceIfRunning(yadaJob.getId());
				if (cached!=null) {
					iter.set(cached); // Replace with the cached one
				}
			}
		}
		return result;
	}
	
	/**
	 * Set a list of jobs to DISABLED then interrupt the threads.
	 * All jobs are first set to disabled, then they are all interrupted.
	 * @param yadaJob
	 */
	public void disableAndInterruptJobs(List<? extends YadaJob> yadaJobs) {
		try {
			for (YadaJob yadaJob : yadaJobs) {
				YadaJob cached = yadaJobScheduler.getJobInstanceIfRunning(yadaJob.getId());
				if (cached!=null) {
					yadaJob = cached;
				}
				yadaJob.disable();
			}
			for (YadaJob yadaJob : yadaJobs) {
				YadaJob cached = yadaJobScheduler.getJobInstanceIfRunning(yadaJob.getId());
				if (cached!=null) {
					yadaJob = cached;
				}
				if (!yadaJobScheduler.interruptJob(yadaJob)) {
					yadaJobRepository.save(yadaJob); // Save it because nobody else will
				}
			}
		} catch (Exception e) {
			log.error("Error while disabling and interrupting jobs", e);
		}
	}
	
	/**
	 * Set a job to COMPLETED
	 * @param yadaJob
	 */
	public void completeJob(Long yadaJobId) {
		YadaJob yadaJob = yadaJobScheduler.getJobInstance(yadaJobId);
		yadaJob.complete();
		yadaJobRepository.save(yadaJob);
	}
	
	/**
	 * Toggle between paused and disabled.
	 * @param yadaJob
	 */
	public void toggleDisabledAndPaused(Long yadaJobId) {
		YadaJob yadaJob = yadaJobScheduler.getJobInstance(yadaJobId);
		if (yadaJob.isPaused()) {
			yadaJob.disable();
		} else if (yadaJob.isDisabled()) {
			yadaJob.pause();
		}
		yadaJobRepository.save(yadaJob);
//		yadaJobRepository.stateToggleBetween(yadaJobId, YadaJobState.PAUSED.toYadaPersistentEnum(), YadaJobState.DISABLED.toYadaPersistentEnum());
	}
	
	/**
	 * Set a job to DISABLED then interrupt its thread
	 * @param yadaJob
	 */
	public void disableAndInterruptJob(Long yadaJobId) {
		YadaJob yadaJob = yadaJobScheduler.getJobInstance(yadaJobId);
		yadaJob.disable();
		if (!yadaJobScheduler.interruptJob(yadaJob)) {
			yadaJobRepository.save(yadaJob); // Save it because nobody else will
		}	
	}
	
	/**
	 * Set a job to PAUSED then interrupt its thread
	 * @param yadaJob
	 */
	public void pauseAndInterruptJob(Long yadaJobId) {
		YadaJob yadaJob = yadaJobScheduler.getJobInstance(yadaJobId);
		yadaJob.pause();
		if (!yadaJobScheduler.interruptJob(yadaJob)) {
			yadaJobRepository.save(yadaJob); // Save it because nobody else will
		}	
	}

	public void changeJobPriority(YadaJob yadaJob, int priority) {
		yadaJob.setJobPriority(priority);
		yadaJobRepository.save(yadaJob);
	}
	
	public void reschedule(YadaJob yadaJob, Date newScheduling) {
		yadaJob.setJobScheduledTime(newScheduling);
		yadaJobRepository.save(yadaJob);
	}
	
	/**
	 * Returns an instance of a YadaJob. The instance is freshly loaded from the database if no other thread
	 * already holds a reference to it, otherwise the instance is shared among threads.
	 * It is thus possible for concurrent threads to modify the same entity (= table row) without incurring in a
	 * concurrent modification exception. The instance is removed from the cache when no one holds a reference to it anymore.
	 * The typical scenario is when a job is already running (an instance has been cached by the scheduler) and a user 
	 * from the web interface changes its name.
	 * @param id the job id
	 * @return a YadaJob instance that can be freely modified and saved
	 */
	public YadaJob getJobInstance(Long id) {
		return yadaJobScheduler.getJobInstance(id);
	}
	
	/**
	 * Call this method after fetching a job instance from the database to replace it with a cached running instance if any.
	 * @param yadaJob
	 * @return the cached instance or the original argument
	 */
	public YadaJob replaceWithCached(YadaJob yadaJob) {
		YadaJob result =  yadaJobScheduler.getJobInstanceIfRunning(yadaJob.getId());
		if (result == null) {
			result = yadaJob;
		}
		return result;
	}

//	/**
//	 * Returns an instance of YadaJob only if the job is currently running.
//	 * @param id
//	 * @return the instance or null
//	 */
//	public YadaJob getJobInstanceIfRunning(Long id) {
//		return yadaJobScheduler.getJobInstanceIfRunning(id);
//	}

}
