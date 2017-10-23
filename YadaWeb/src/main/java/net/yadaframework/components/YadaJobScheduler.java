package net.yadaframework.components;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.persistence.repository.YadaJobDao;
import net.yadaframework.persistence.repository.YadaJobRepository;
import net.yadaframework.persistence.repository.YadaJobSchedulerDao;

/**
 * Takes care of running and managing YadaJob instances.
 * It is invoked every config/yada/jobSchedulerPeriodMillis milliseconds.
 * At every invocation, it starts all jobs that have a start date that falls in the next period.
 * This means that the jobSchedulerPeriodMillis is the minimum resolution for job scheduling.
 * So if the jobSchedulerPeriodMillis is set to 9000 it means that the real start time of a job can 
 * be anticipated by 9 seconds from the expected start time. It is up to the job itself to delay start if needed.
 * @see YadaConfiguration#getYadaJobSchedulerPeriod()
 */
@Service
// For some reason @Transactional causes autowiring problems, so I created a YadaJobSchedulerDao
public class YadaJobScheduler implements Runnable {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired private YadaJobRepository yadaJobRepository;
    @Autowired private YadaJobSchedulerDao yadaJobSchedulerDao;
    @Autowired private YadaJobDao yadaJobDao;
    @Autowired private YadaConfiguration config;
	@Autowired private TaskScheduler yadaJobSchedulerTaskScheduler; // Used only to schedule the YadaJobScheduler
	
	private ListeningExecutorService jobScheduler; // Used for job scheduling
	
	/**
	 * Map from YadaJob id to its running thread handle
	 */
	private ConcurrentMap<Long, ListenableFuture<YadaJob>> jobHandles = new ConcurrentHashMap<>();
	
	@PostConstruct
	public void init() throws Exception {
		// It is itself scheduled by a TaskScheduler configured in YadaAppConfig and run every config/yada/jobSchedulerPeriodMillis milliseconds
		log.debug("init called");
		long period = config.getYadaJobSchedulerPeriod();
		if (period>0) {
			// Using Guava to create a ListenableFuture: https://github.com/google/guava/wiki/ListenableFutureExplained
			ExecutorService executorService = Executors.newFixedThreadPool(config.getYadaJobSchedulerThreadPoolSize());
			jobScheduler = MoreExecutors.listeningDecorator(executorService);
			// Disable any job that is already RUNNING but jobRecoverable is false
			yadaJobDao.setUnrecoverableJobState();
			// Recover any job that is still in the RUNNING state and its group is not paused
			List<YadaJob> recoverableJobs = yadaJobDao.getRecoverableJobs();
			for (YadaJob yadaJob : recoverableJobs) {
				yadaJob.setRecovered(true);
				runJob(yadaJob.getId());
			}
			// Scheduling the YadaJobScheduler according to the configured period
			yadaJobSchedulerTaskScheduler.scheduleAtFixedRate(this, new Date(System.currentTimeMillis() + 10000), period);
		} else {
			log.info("YadaJobScheduler not started");
		}
	}
	
	@Override
	public void run() {
		log.debug("RUN");
		cleanupStaleJobs();
		startJobs();
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
				interruptJob(yadaJob.getId());
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
		yadaJobSchedulerDao.internalSetState(yadaJobId, YadaJobState.PAUSED);
		interruptJob(yadaJobId);
	}

//	/**
//	 * Set the job state to PAUSE without forcing the thread interruption
//	 * @param yadaJob
//	 */
//	public void pauseJob(Long yadaJobId) {
//		yadaJob.pause();
//		yadaJobRepository.save(yadaJob);
//	}
	
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
		List<? extends YadaJob> jobsToRun = yadaJobSchedulerDao.internalFindJobsToRun();
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
					interruptJob(running.getId());
				} else if (jobIsRunning(running.getId())) {
					runCache.add(candidateGroup); // The running job has higher priority and is actually running, so no other job in the group can have a higher priority because of the query order
					continue; // Next candidate
				}
			}
			// Either there was no running job or it has been preempted
			runJob(candidate.getId());
			runCache.add(candidateGroup);
		}
	}

	/**
	 * Returns true if the job is actually running in a thread
	 * @param yadaJob
	 * @return
	 */
	private boolean jobIsRunning(Long yadaJobId) {
		ListenableFuture<YadaJob> jobHandle = jobHandles.get(yadaJobId);
		if (jobHandle!=null) {
			boolean running = !jobHandle.isDone();
//			if (!running) {
//				jobHandles.remove(yadaJob); // As a side effect, the map is cleaned up
//			}
			return running;
		}
		return false;
	}

	/**
	 * Run the job now.
	 * The job must set its own state to DISABLED or PAUSED when failed, otherwise it is set to ACTIVE.
	 * @param yadaJob
	 * @return
	 */
	private void runJob(Long yadaJobId) {
		ListenableFuture<YadaJob> jobHandle = yadaJobSchedulerDao.internalRunJob(yadaJobId, jobScheduler);
		jobHandles.put(yadaJobId, jobHandle);
		Futures.addCallback(jobHandle, new FutureCallback<Object>() {
			// The callback is run in executor
			public void onSuccess(Object result) {
				jobHandles.remove(yadaJobId);
				yadaJobSchedulerDao.internalJobSuccessful(yadaJobId);
			}
			public void onFailure(Throwable thrown) {
				jobHandles.remove(yadaJobId);
				yadaJobSchedulerDao.internalJobFailed(yadaJobId, thrown);
			}
		},  MoreExecutors.directExecutor());
	}
	
	/**
	 * Interrupt the job and make it ACTIVE
	 * @param yadaJob
	 */
	private void interruptJob(Long yadaJobId) {
		log.debug("Interrupting job id {}", yadaJobId);
		ListenableFuture<YadaJob> jobHandle = jobHandles.get(yadaJobId);
		if (jobHandle!=null) {
			jobHandle.cancel(true);
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// TODO controllare che onFailure sia chiamato, altrimenti fare le cose che seguono
//			yadaJob.setJobState(YadaJobState.ACTIVE);
//			yadaJobRepository.save(yadaJob);
//			jobHandles.remove(yadaJob);
		} else {
			log.debug("No job handle found for job id {} when interrupting", yadaJobId);
		}
	}
	
	private void cleanupStaleJobs() {
		for (Long yadaJobId : jobHandles.keySet()) {
			long timeToLive = config.getYadaJobSchedulerStaleMillis();
			Date jobStartTime = yadaJobRepository.getJobStartTime(yadaJobId);
			if (jobStartTime!=null) {
				Date staleDate = new Date(System.currentTimeMillis() - timeToLive);
				if (jobStartTime.before(staleDate)) {
					log.warn("Job id {} is stale", yadaJobId);
					interruptJob(yadaJobId);
				}
			}
		}
	}

	

}
