package net.yadaframework.components;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;
import net.yadaframework.persistence.repository.YadaJobDao;
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
// Package visibility - it should not be used by the application
class YadaJobScheduler implements Runnable {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired private YadaJobDao yadaJobDao;
    @Autowired private YadaJobSchedulerDao yadaJobSchedulerDao;
    @Autowired private YadaUtil yadaUtil;
    @Autowired private YadaConfiguration config;
	
	private ListeningExecutorService jobScheduler;
	
//	/**
//	 * Map from YadaJob id to its running thread handle
//	 */
//	private ConcurrentMap<Long, ListenableFuture<Void>> jobHandles = new ConcurrentHashMap<>();
	
	/**
	 * Cache for detached YadaObject entities
	 */
	private LoadingCache<Long, YadaJob> jobCache;
	
	@PostConstruct
	void init() throws Exception {
		log.debug("init called");
		// Using Guava to create a ListenableFuture: https://github.com/google/guava/wiki/ListenableFutureExplained
		ExecutorService executorService = Executors.newFixedThreadPool(config.getYadaJobSchedulerThreadPoolSize());
		jobScheduler = MoreExecutors.listeningDecorator(executorService);
		// Job cache
		RemovalListener<Long, YadaJob> removalListener = new RemovalListener<Long, YadaJob>() {
			  public void onRemoval(RemovalNotification<Long, YadaJob> removal) {
				  // If a job is being removed before being stopped (should only happen if the cache is too small), we stop it
				  // otherwise we can't tell if its running anymore
				  YadaJob removed = removal.getValue();
				  if (removed!=null && removed.yadaInternalJobHandle!=null) {
					  if (!removed.yadaInternalJobHandle.isDone()) {
						  log.error("Evicting job {} while still running - interrupting job", removed);
						  long totCached = jobCache.size();
						  if (totCached >= config.getYadaJobSchedulerCacheSize()*0.9) {
							  log.error("Job cache has {} elements. Consider increasing the configured jobCacheSize", totCached);
						  } else {
							  log.error("There is still space in the cache, so this could be due to a programming error");
						  }
						  removed.yadaInternalJobHandle.cancel(true);
					  }
				  }
			  }
			};
		jobCache = CacheBuilder.newBuilder()
			.weakValues() // This allows entries to be garbage-collected if there are no other references to the values
			.maximumSize(config.getYadaJobSchedulerCacheSize()) 
			.removalListener(removalListener)
			.build(
				new CacheLoader<Long, YadaJob>() {
					public YadaJob load(Long id) {
						return yadaJobDao.findById(id).orElse(null);
					}
				}
			);
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
			// Check if a job of the same group is already running, by looking into the cache.
			// This works because when a job is evicted we interrupt it if still running.
			YadaJob runningSameGroup = yadaJobDao.findRunning(candidateGroup);
			if (runningSameGroup!=null) {
				// The running instance is NOT the same as the one in the jobCache, so replace the instance
				YadaJob cachedRunning = jobCache.getIfPresent(runningSameGroup.getId());
				// If a job is already running, compare the priority for preemption
				if (runningSameGroup.getJobPriority()<candidate.getJobPriority()) {
					// Preemption
					// Can't use "running" here because the instance is different from the cached one
					interruptJob(cachedRunning);
				} else if (jobIsRunning(cachedRunning)) {
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
	boolean jobIsRunning(YadaJob yadaJob) {
		if (yadaJob==null) {
			return false;
		}
		ListenableFuture<?> jobHandle = yadaJob.yadaInternalJobHandle;
		if (jobHandle!=null) {
			boolean running = !jobHandle.isDone();
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
	void runJob(final YadaJob yadaJobToWire) {
		log.debug("Running job {}", yadaJobToWire);
		YadaJob existing = jobCache.getIfPresent(yadaJobToWire.getId());
		if (existing!=null && existing.yadaInternalJobHandle!=null) {
			// We can do nothing about it because if we interrupt it, the onFailure will mess with the new job.
			throw new YadaInternalException("Starting a job when another with the same id is still running: {}", existing);
		}
		final YadaJob yadaJob = (YadaJob) yadaUtil.autowireAndInitialize(yadaJobToWire); // YadaJob instances can have @Autowire fields
		jobCache.put(yadaJob.getId(), yadaJob);
//		YadaJob toRun = yadaJobRepository.findOne(yadaJobId);
//		if (toRun==null) {
//			log.info("Job not found when trying to run it, id={}", toRun);
//			return;
//		}
		yadaJobDao.stateChangeFromTo(yadaJob, YadaJobState.ACTIVE, YadaJobState.RUNNING); // Needed for database queries on running jobs
		yadaJob.setJobStateObject(YadaJobState.RUNNING.toYadaPersistentEnum()); // Needed to check for stale jobs
		Date startTime = new Date();
		yadaJobDao.setStartTime(yadaJob.getId(), startTime);
		yadaJob.setJobStartTime(startTime); // Needed to check for stale jobs
		ListenableFuture<Void> jobHandle = jobScheduler.submit(yadaJob);
		yadaJob.yadaInternalJobHandle = jobHandle;
//		jobHandles.put(yadaJobId, jobHandle);
		Futures.addCallback(jobHandle, new FutureCallback<Void>() {
			// The callback is run in executor
			public void onSuccess(Void result) {
				// result is always null
				yadaJobSchedulerDao.internalJobSuccessful(yadaJob);
				// Change back to active if the state has not been changed already
//				yadaJobRepository.stateChangeFromTo(yadaJob, YadaJobState.RUNNING, YadaJobState.ACTIVE);
				invalidateCompletedJob(yadaJob);
//				jobHandles.remove(yadaJobId);
			}
			public void onFailure(Throwable thrown) {
//				jobHandles.remove(yadaJobId);
				yadaJobSchedulerDao.internalJobFailed(yadaJob, thrown);
				// Change back to active if the state has not been changed already
//				yadaJobRepository.stateChangeFromTo(toRun, YadaJobState.RUNNING, YadaJobState.ACTIVE);
				invalidateCompletedJob(yadaJob);
			}
		},  MoreExecutors.directExecutor());
	}
	
	private void invalidateCompletedJob(YadaJob yadaJob) {
		// Always remove the handle first, so that the removelistener doesn't get fooled
		yadaJob.yadaInternalJobHandle = null;
		jobCache.invalidate(yadaJob.getId());
	}
	
	/**
	 * Interrupt the job and make it ACTIVE
	 * @param yadaJob
	 * @return true if the job could be interrupted
	 */
	boolean interruptJob(Long yadaJobId) {
		YadaJob yadaJob = jobCache.getIfPresent(yadaJobId);
		return interruptJob(yadaJob);
	}
	
	/**
	 * Interrupt the job and make it ACTIVE
	 * @param yadaJob
	 * @return true if the job could be interrupted
	 */
	boolean interruptJob(YadaJob yadaJob) {
		if (yadaJob==null) {
			return false;
		}
		try {
			log.debug("Interrupting job {}", yadaJob);
			ListenableFuture<?> jobHandle = yadaJob.yadaInternalJobHandle;
			if (jobHandle!=null) {
				jobHandle.cancel(true);
				return true;
			} else {
				log.debug("No job handle found for job {} when interrupting", yadaJob);
			}
			return false;
		} finally {
			invalidateCompletedJob(yadaJob); // Remove from the cache
		}
	}

	private void cleanupStaleJobs() {
		long timeToLive = config.getYadaJobSchedulerStaleMillis();
		Map<Long, YadaJob> jobMap = jobCache.asMap();
		for (Map.Entry<Long, YadaJob> entry : jobMap.entrySet()) {
			YadaJob toCheck = entry.getValue();
			if (!toCheck.isRunning()) {
				continue;
			}
			Date jobStartTime = toCheck.getJobStartTime();
			if (jobStartTime!=null) {
				Date staleDate = new Date(System.currentTimeMillis() - timeToLive);
				if (jobStartTime.before(staleDate)) {
					log.warn("Job {} is stale", toCheck);
					interruptJob(toCheck);
				}
			}
		}
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
		return jobCache.getUnchecked(id);
	}

	/**
	 * Returns an instance of YadaJob only if the job is currently running.
	 * @param id
	 * @return the instance or null
	 */
	public YadaJob getJobInstanceIfRunning(Long id) {
		return jobCache.getIfPresent(id);
	}
	


}
