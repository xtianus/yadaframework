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
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;
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
// For some reason @Transactional+Runnable causes autowiring problems, so I created a YadaJobSchedulerDao:
// org.springframework.beans.factory.BeanNotOfRequiredTypeException: Bean named 'yadaJobScheduler' is expected to be of type '...YadaJobScheduler' but was actually of type 'com.sun.proxy.$Proxy112'
// Package visibility
class YadaJobScheduler implements Runnable {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired private YadaJobRepository yadaJobRepository;
    @Autowired private YadaJobSchedulerDao yadaJobSchedulerDao;
    @Autowired private YadaUtil yadaUtil;
    @Autowired private YadaConfiguration config;
	
	private ListeningExecutorService jobScheduler;
	
	/**
	 * Map from YadaJob id to its running thread handle
	 */
	private ConcurrentMap<Long, ListenableFuture<Void>> jobHandles = new ConcurrentHashMap<>();
	
	@PostConstruct
	public void init() throws Exception {
		log.debug("init called");
		// Using Guava to create a ListenableFuture: https://github.com/google/guava/wiki/ListenableFutureExplained
		ExecutorService executorService = Executors.newFixedThreadPool(config.getYadaJobSchedulerThreadPoolSize());
		jobScheduler = MoreExecutors.listeningDecorator(executorService);
	}
	
	@Override
	public void run() {
//		log.debug("RUN");
		cleanupStaleJobs();
		startJobs();
	}

	/** 
	 * Run the jobs that are scheduled to run in the next X seconds.
	 */
	private void startJobs() {
		List<String> runCache = new ArrayList<>();
		MDC.put("yadaThreadLevel", "info"); // Increase the log level to info so that you can remove sql dumps when in debug mode.
		List<? extends YadaJob> jobsToRun = yadaJobSchedulerDao.internalFindJobsToRun();
		MDC.remove("yadaThreadLevel");
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
	public boolean jobIsRunning(Long yadaJobId) {
		ListenableFuture<?> jobHandle = jobHandles.get(yadaJobId);
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
	public void runJob(Long yadaJobId) {
		log.debug("Running job id {}", yadaJobId);
		YadaJob toRun = yadaJobRepository.findOne(yadaJobId);
		if (toRun==null) {
			log.info("Job not found when trying to run it, id={}", toRun);
			return;
		}
		yadaJobRepository.internalSetRunning(yadaJobId, YadaJobState.RUNNING.toId(), YadaJobState.ACTIVE.toId());
		final YadaJob wiredYadaJob = (YadaJob) yadaUtil.autowire(toRun); // YadaJob instances can have @Autowire fields
		ListenableFuture<Void> jobHandle = jobScheduler.submit(wiredYadaJob);
		jobHandles.put(yadaJobId, jobHandle);
		Futures.addCallback(jobHandle, new FutureCallback<Void>() {
			// The callback is run in executor
			public void onSuccess(Void result) {
				// result is always null
				jobHandles.remove(yadaJobId);
				yadaJobSchedulerDao.internalJobSuccessful(wiredYadaJob);
			}
			public void onFailure(Throwable thrown) {
				jobHandles.remove(yadaJobId);
				yadaJobSchedulerDao.internalJobFailed(wiredYadaJob, thrown);
			}
		},  MoreExecutors.directExecutor());
	}
//	
//    // Starts a job, adding it to the job scheduler.
//    private ListenableFuture<YadaJob> internalRunJob(Long yadaJobId) {
// 		// Perform autowiring of the job instance
//		yadaJob = (YadaJob) yadaUtil.autowire(yadaJob);
//		@SuppressWarnings("unchecked")
//    	return jobHandle;
//    }    

	
	/**
	 * Interrupt the job and make it ACTIVE
	 * @param yadaJob
	 */
	public void interruptJob(Long yadaJobId) {
		log.debug("Interrupting job id {}", yadaJobId);
		ListenableFuture<?> jobHandle = jobHandles.get(yadaJobId);
		if (jobHandle!=null) {
			jobHandle.cancel(true);
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
