package net.yadaframework.components;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.persistence.repository.YadaJobDao;
import net.yadaframework.persistence.repository.YadaJobRepository;

/**
 * Takes care of running and managing YadaJob instances.
 * It is itself scheduled by a TaskScheduler configured in YadaAppConfig and run every config/yada/jobSchedulerPeriodMillis milliseconds
 * @see YadaConfiguration#getJobSchedulerPeriod()
 */
@Service
// For some reason @Transactional causes autowiring problems
public class YadaJobScheduler implements Runnable {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired private YadaJobRepository yadaJobRepository;
    @Autowired private YadaJobDao yadaJobDao;
    @Autowired private YadaConfiguration config;
	@Autowired private TaskScheduler taskScheduler;
	
	private Map<YadaJob, ScheduledFuture> jobHandles = new HashMap<>();
	
	@PostConstruct
	public void init() throws Exception {
		log.debug("init called");
		long period = config.getJobSchedulerPeriod();
		if (period>0) {
			taskScheduler.scheduleAtFixedRate(this, new Date(System.currentTimeMillis() + 10000), period);
		}
		// TODO allo startup del server bisogna cercare i job in stato RUNNING (che non siano jobGroupPaused)
		// (insieme a quelli che stanno per partire) avevnti il flag jobRecoverable=true, mentre quelli che
		// l'hanno false vengono messi a disabled.
		// Quando un job è recovered, viene chiamato un suo metodo recover() che si occupa di capire se deve completare il run precedente
		// e decide quando schedularsi di nuovo.
	}
	
	@Override
	public void run() {
		log.debug("RUN");
		checkJobStatus();
		startJobs();
	}

	/**
	 * Activate the job. The start time is left unchanged unless null, when it is set to NOW.
	 * @param yadaJob
	 */
	public void startJob(YadaJob yadaJob) {
		if (yadaJob.getJobScheduledTime()==null) {
			yadaJob.setJobScheduledTime(new Date());
		}
		yadaJob.setJobState(YadaJobState.ACTIVE);
		yadaJobRepository.save(yadaJob);
	}
	
	/**
	 * Check if the job group has been paused
	 * @param jobGroup
	 * @return
	 */
	public boolean isJobGroupPaused(String jobGroup) {
		return yadaJobRepository.isJobGroupPaused(jobGroup);
	}
	
	/**
	 * Pause all jobs in the given group
	 * @param jobGroup
	 * @param force true to interrupt a job that is in execution, false to let it complete before pausing
	 */
	public void pauseJobGroup(String jobGroup, boolean force) {
		if (force) {
			// There should be just one job in execution, but we look for many anyway
			List<YadaJob> running = yadaJobRepository.findByJobGroupAndState(jobGroup, YadaJobState.RUNNING.toYadaPersistentEnum(), null);
			for (YadaJob yadaJob : running) {
				interruptJob(yadaJob);
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
		
		// TODO force stop the task
		
		// Remove the association with other jobs
		List<YadaJob> relatedJobs = yadaJobRepository.findByJobsMustCompleteContains(yadaJob);
		for (YadaJob related : relatedJobs) {
			related.getJobsMustComplete().remove(yadaJob);
			yadaJobRepository.save(related);
		}
		relatedJobs = yadaJobRepository.findByJobsMustBeInactiveContains(yadaJob);
		for (YadaJob related : relatedJobs) {
			related.getJobsMustComplete().remove(yadaJob);
			yadaJobRepository.save(related);
		}
		relatedJobs = yadaJobRepository.findByJobsMustBeActiveContains(yadaJob);
		for (YadaJob related : relatedJobs) {
			related.getJobsMustComplete().remove(yadaJob);
			yadaJobRepository.save(related);
		}
		//
		yadaJobRepository.delete(yadaJob);
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
	
	public void pauseJob(YadaJob yadaJob) {
		yadaJob.pause();
		yadaJobRepository.save(yadaJob);
	}

	public void changeJobPriority(YadaJob yadaJob, int priority) {
		yadaJob.setJobPriority(priority);
		yadaJobRepository.save(yadaJob);
	}
	
	public void reschedule(YadaJob yadaJob, Date newScheduling) {
		yadaJob.setJobScheduledTime(newScheduling);
		yadaJobRepository.save(yadaJob);
	}
	

/////////////////////////
	// Private methods //
	/////////////////////
	
	/** 
	 * Run the jobs that are scheduled to run in the next X seconds.
	 */
	private void startJobs() {
		List<String> runCache = new ArrayList<>();
		List<? extends YadaJob> jobsToRun = yadaJobDao.findJobsToRun();
		log.debug("Found {} job candidates to run", jobsToRun.size());
		// The list contains all candidate jobs for any jobGroup.
		for (YadaJob candidate : jobsToRun) {
			String candidateGroup = candidate.getJobGroup();
			if (runCache.contains(candidateGroup)) {
				continue; // We just started a job in this group, which can't be of lower priority because of the query order
			}
			// Check if a job of the same group is already running
			YadaJob running = yadaJobRepository.findRunning(candidateGroup);
			if (running!=null) {
				// If a job is already running, compare the priority for preemption
				if (running.getJobPriority()<candidate.getJobPriority()) {
					// Preemption
					interruptJob(running);
				} else if (jobIsRunning(running)) {
					runCache.add(candidateGroup); // The running job has higher priority and is actually running, so no other job in the group can have a higher priority because of the query order
					continue; // Next candidate
				}
			}
			// Either there was no running job or it has been preempted
			runJob(candidate);
			runCache.add(candidateGroup);
		}
	}

	/**
	 * Returns true if the job is actually running in a thread
	 * @param yadaJob
	 * @return
	 */
	private boolean jobIsRunning(YadaJob yadaJob) {
		ScheduledFuture jobHandle = jobHandles.get(yadaJob);
		if (jobHandle!=null) {
			boolean running = !jobHandle.isDone();
			if (!running) {
				jobHandles.remove(yadaJob); // As a side effect, the map is cleaned up
			}
			return running;
		}
		return false;
	}

	/**
	 * Run the job now
	 * @param yadaJob
	 * @return
	 */
	private void runJob(YadaJob yadaJob) {
		log.debug("Running job {}", yadaJob);
		yadaJob.setJobState(YadaJobState.RUNNING);
		yadaJobRepository.save(yadaJob);
		ScheduledFuture jobHandle = taskScheduler.schedule(yadaJob, new Date());
		jobHandles.put(yadaJob, jobHandle);
	}
	
	/**
	 * Interrupt the job and make it ACTIVE
	 * @param yadaJob
	 */
	private void interruptJob(YadaJob yadaJob) {
		log.debug("Interrupting job {}", yadaJob);
		ScheduledFuture jobHandle = jobHandles.get(yadaJob);
		if (jobHandle!=null) {
			jobHandle.cancel(true);
			yadaJob.setJobState(YadaJobState.ACTIVE);
			yadaJobRepository.save(yadaJob);
			jobHandles.remove(yadaJob);
		} else {
			log.error("No job handle found for {} when interrupting", yadaJob);
			// TODO do something?
		}
	}
	
	private void checkJobStatus() {
		for (YadaJob yadaJob : jobHandles.keySet()) {
			ScheduledFuture jobHandle = jobHandles.get(yadaJob);
			if (jobHandle.isDone()) {
				// Job terminated
				jobHandles.remove(yadaJob);
				yadaJob.setJobState(YadaJobState.ACTIVE);
				yadaJobRepository.save(yadaJob);
			}
		}
	}

	

}
