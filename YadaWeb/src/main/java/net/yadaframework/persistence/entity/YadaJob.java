package net.yadaframework.persistence.entity;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.ListenableFuture;

// TODO spostare in YadaBones?

/**
 * The base class for jobs handled by the YadaScheduler.
 * Subclasses must implement the run() method.
 * Uses joined inheritance so that subclasses have their own table; the subclass id, which must not be declared in java but exists in the table, gets the same value as the YadaJob id.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class YadaJob implements Callable<Void> {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
//	// For optimistic locking
//	@Version
//	protected long version;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	@OneToOne(fetch = FetchType.EAGER)
	private YadaPersistentEnum<YadaJobState> jobStateObject = YadaJobState.PAUSED.toYadaPersistentEnum();
	
	protected boolean jobGroupPaused = false;
	
	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date jobScheduledTime;
	
	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date jobLastSuccessfulRun;
	
	@Column(length=128)
	protected String jobName;
	
	/**
	 * Jobs in the same jobGroup are subject to preemption
	 */
	@Column(length=128)
	protected String jobGroup;
	@Column(length=256)
	protected String jobDescription;

	/**
	 * Higher priority jobs can preempt lower priority jobs of the same jobGroup
	 */
	protected int jobPriority = 10; // 0 = lowest priority, MAX_INTEGER = highest priority
	
	/**
	 * Run the current job only while the jobMustBeActive instance is active.
	 * If that instance is deleted, the current job is deactivated
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name="YadaJob_BeActive")
	protected List<YadaJob> jobsMustBeActive;

	/**
	 * Run the current job only if the jobsMustBeInactive instance is not active/paused nor running.
	 * If that instance is deleted or deactivated, the current job can be activated
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name="YadaJob_BeInactive")
	protected List<YadaJob> jobsMustBeInactive;

	/**
	 * Run the current job after the jobMustComplete completes successfully.
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name="YadaJob_BeCompleted")
	protected List<YadaJob> jobsMustComplete;
	
	/**
	 * Tell if a job should be started immediately again from the beginning after a system crash/shutdown while running.
	 * When true, the job should take care of deleting or skipping any partial results, if needed. 
	 * When false, the job will become DISABLED after a system crash
	 * (true by default)
	 */
	protected boolean jobRecoverable = true;

	/**
	 * Should be incremented every time a job does not execute successfully and set to zero on successful execution.
	 */
	protected int errorStreakCount = 0;
	
	/**
	 * The time at which the job was started - null if the job is not in the RUNNING state.
	 */
	protected Date jobStartTime = null;
	
	/**
	 * True when the current invocation is on a job that was running when the server crashed and has the jobRecoverable flag true.
	 * Implementations can choose what to do when recovering from a crash.
	 */
	@Transient
	protected boolean recovered = false;
	
	/**
	 * Handle returned by ListeningExecutorService.submit() - internal use only
	 */
	@Transient
	public ListenableFuture<Void> yadaInternalJobHandle;
	
	/**
	 * Needed for DataTables integration
	 */
	@Transient 
	@JsonProperty("DT_RowId")
	public String getDT_RowId() {
		return this.getClass().getSimpleName()+"#"+this.id; // YadaJob#142
	}
	
	/**
	 * Returns the plain enum of the job state
	 * @return
	 */
	@Transient
	public YadaJobState getJobState() {
		return jobStateObject.toEnum();
	}
	
	/**
	 * Set the persistent state of this job
	 * @param jobState
	 */
	@Transient
	public void setJobState(YadaJobState jobState) {
		this.jobStateObject = jobState.toYadaPersistentEnum();
	}
	
	/**
	 * Set the job state to active and return the previous state
	 * @return the previous state
	 */
	public YadaJobState activate() {
		YadaJobState result = getJobState();
		this.jobStateObject = YadaJobState.ACTIVE.toYadaPersistentEnum();
		return result;
	}
	
	/**
	 * Set the job state to paused and return the previous state
	 * @return the previous state
	 */
	public YadaJobState pause() {
		YadaJobState result = getJobState();
		this.jobStateObject = YadaJobState.PAUSED.toYadaPersistentEnum();
		return result;
	}
	
	/**
	 * Set the job state to disabled and return the previous state
	 * @return
	 */
	public YadaJobState disable() {
		YadaJobState result = getJobState();
		this.jobStateObject = YadaJobState.DISABLED.toYadaPersistentEnum();
		return result;
	}
	
	/**
	 * Set the job state to completed and return the previous state
	 * @return
	 */
	public YadaJobState complete() {
		YadaJobState result = getJobState();
		this.jobStateObject = YadaJobState.COMPLETED.toYadaPersistentEnum();
		return result;
	}
	
	@Transient
	public boolean isActive() {
		return getJobState() == YadaJobState.ACTIVE;
	}
	
	@Transient
	public boolean isCompleted() {
		return getJobState() == YadaJobState.COMPLETED;
	}
	
	@Transient
	public boolean isDisabled() {
		return getJobState() == YadaJobState.DISABLED;
	}
	
	@Transient
	public boolean isPaused() {
		return getJobState() == YadaJobState.PAUSED;
	}
	
	@Transient
	public boolean isRunning() {
		return getJobState() == YadaJobState.RUNNING;
	}
	
	/**
	 * Increment the error streak count returning the new value
	 * @return
	 */
	public int incrementErrorStreak() {
		return ++errorStreakCount;
	}

	/**
	 * Two YadaJob are equal only if the ids are equal and not null
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final YadaJob other = (YadaJob) obj;
        if (id==null && other.id==null) {
        	return false;
        }
        return Objects.equals(this.id, other.id);
	}

	/**
	 * The hashCode is derived from the id
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getJobScheduledTime() {
		return jobScheduledTime;
	}

	public void setJobScheduledTime(Timestamp jobScheduledTime) {
		this.jobScheduledTime = jobScheduledTime;
	}

	/**
	 * Return the time that the job last ended successfully. Null if the job was never run successfully.
	 * @return
	 */
	public Date getJobLastSuccessfulRun() {
		return jobLastSuccessfulRun;
	}

	public void setJobLastSuccessfulRun(Timestamp jobLastSuccessfulRun) {
		this.jobLastSuccessfulRun = jobLastSuccessfulRun;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getJobGroup() {
		return jobGroup;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

	public String getJobDescription() {
		return jobDescription;
	}

	public void setJobDescription(String jobDescription) {
		this.jobDescription = jobDescription;
	}

	public int getJobPriority() {
		return jobPriority;
	}

	/**
	 * 0 = lowest priority, MAX_INTEGER = highest priority
	 * @param jobPriority
	 */
	public void setJobPriority(int jobPriority) {
		this.jobPriority = jobPriority;
	}

	/**
	 * Do not use directly unless you know the implications. It could be stale and be different from the database value.
	 * @return
	 */
	public YadaPersistentEnum<YadaJobState> getJobStateObject() {
		return jobStateObject;
	}

	/**
	 * Do not use directly unless you know the implications. It could be stale and overwrite a more recent value in the database.
	 * @param jobStateObject
	 */
	public void setJobStateObject(YadaPersistentEnum<YadaJobState> jobStateObject) {
		this.jobStateObject = jobStateObject;
	}
	
	public boolean isJobRecoverable() {
		return jobRecoverable;
	}

	public void setJobRecoverable(boolean recoverable) {
		this.jobRecoverable = recoverable;
	}

	public void setJobScheduledTime(Date jobScheduledTime) {
		this.jobScheduledTime = jobScheduledTime;
	}

	public void setJobLastSuccessfulRun(Date jobLastSuccessfulRun) {
		this.jobLastSuccessfulRun = jobLastSuccessfulRun;
	}

	public List<YadaJob> getJobsMustBeActive() {
		return jobsMustBeActive;
	}

	public void setJobsMustBeActive(List<YadaJob> jobsMustBeActive) {
		this.jobsMustBeActive = jobsMustBeActive;
	}

	public List<YadaJob> getJobsMustBeInactive() {
		return jobsMustBeInactive;
	}

	public void setJobsMustBeInactive(List<YadaJob> jobsMustBeInactive) {
		this.jobsMustBeInactive = jobsMustBeInactive;
	}

	public List<YadaJob> getJobsMustComplete() {
		return jobsMustComplete;
	}

	public void setJobsMustComplete(List<YadaJob> jobsMustComplete) {
		this.jobsMustComplete = jobsMustComplete;
	}

	public boolean isJobGroupPaused() {
		return jobGroupPaused;
	}

	public void setJobGroupPaused(boolean jobGroupPaused) {
		this.jobGroupPaused = jobGroupPaused;
	}

	public Date getJobStartTime() {
		return jobStartTime;
	}

	public void setJobStartTime(Date jobRunTime) {
		this.jobStartTime = jobRunTime;
	}

	public boolean isRecovered() {
		return recovered;
	}

	public void setRecovered(boolean recovered) {
		this.recovered = recovered;
	}

//	public long getVersion() {
//		return version;
//	}

	public int getErrorStreakCount() {
		return errorStreakCount;
	}

	public void setErrorStreakCount(int errorStreakCount) {
		this.errorStreakCount = errorStreakCount;
	}

	
}
