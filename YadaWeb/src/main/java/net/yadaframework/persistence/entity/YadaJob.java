package net.yadaframework.persistence.entity;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

// TODO spostare in YadaBones?

/**
 * The base class for jobs handled by the YadaScheduler
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaJob {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	@OneToOne(cascade=CascadeType.ALL, orphanRemoval=true, fetch = FetchType.EAGER)
	protected YadaPersistentEnum<YadaJobState> state;
	
	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date jobScheduledTime;
	
	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date jobLastSuccessfulRun;
	
	@Column(length=32)
	protected String jobName;
	@Column(length=32)
	protected String jobGroup;
	@Column(length=64)
	protected String jobDescription;

	protected int jobPriority = 10; // 0 = lowest priority, MAX_INTEGER = highest priority
	
	/**
	 * Run the current job only while the jobMustBeActive instance is active.
	 * If that instance is deleted, the current job is deactivated
	 */
	@OneToOne
	protected YadaJob jobMustBeActive; // TODO list of jobs?

	/**
	 * Run the current job only if the jobMustNotBeActive instance is not active.
	 * If that instance is deleted or deactivated, the current job can be activated
	 */
	@OneToOne
	protected YadaJob jobMustNotBeActive; // TODO list of jobs?

	/**
	 * Run the current job after the jobMustComplete completes successfully.
	 */
	@OneToOne
	protected YadaJob jobMustComplete; // TODO list of jobs?
	
	/**
	 * Tell if a job should be started immediately again from the beginning after a system crash/shutdown while running.
	 * When true, the job should take care of deleting or skipping any partial results, if needed. 
	 * When false, the job will become DISABLED after a crash
	 * (true by default)
	 */
	protected boolean recoverable = true;
	
	/**
	 * Needed for DataTables integration
	 */
	@Transient 
	@JsonProperty("DT_RowId")
	public String getDT_RowId() {
		return this.getClass().getSimpleName()+"#"+this.id; // YadaJob#142
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

	public YadaJob getJobMustBeActive() {
		return jobMustBeActive;
	}

	public void setJobMustBeActive(YadaJob jobMustBeActive) {
		this.jobMustBeActive = jobMustBeActive;
	}

	public YadaJob getJobMustNotBeActive() {
		return jobMustNotBeActive;
	}

	public void setJobMustNotBeActive(YadaJob jobMustNotBeActive) {
		this.jobMustNotBeActive = jobMustNotBeActive;
	}

	public YadaPersistentEnum<YadaJobState> getState() {
		return state;
	}

	public YadaJob getJobMustComplete() {
		return jobMustComplete;
	}

	public void setJobMustComplete(YadaJob jobMustComplete) {
		this.jobMustComplete = jobMustComplete;
	}

	public boolean isRecoverable() {
		return recoverable;
	}

	public void setRecoverable(boolean recoverable) {
		this.recoverable = recoverable;
	}

	public void setState(YadaPersistentEnum<YadaJobState> state) {
		this.state = state;
	}

	public void setJobScheduledTime(Date jobScheduledTime) {
		this.jobScheduledTime = jobScheduledTime;
	}

	public void setJobLastSuccessfulRun(Date jobLastSuccessfulRun) {
		this.jobLastSuccessfulRun = jobLastSuccessfulRun;
	} 
	
	
}
