package net.yadaframework.persistence.entity;

import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.core.YadaLocalEnum;

/**
 * The localized state of a YadaJob:
 * <ul>
 * <li>ACTIVE: the job is waiting to be run</li>
 * <li>RUNNING: the job is running</li>
 * <li>PAUSED: scheduling on this job has been paused (by the user) and the job should not run</li>
 * <li>COMPLETED: the job has completed its purpose and should not run again</li>
 * <li>DISABLED: the job has been disabled because of errors</li>
 * </ul>
 *
 */
public enum YadaJobState implements YadaLocalEnum<YadaJobState> {
	// In messages.properties:
	//	yada.jobstate.ACTIVE = Active
	//	yada.jobstate.RUNNING = Running
	//	yada.jobstate.PAUSED = Paused
	//	yada.jobstate.COMPLETED = Completed
	//	yada.jobstate.DISABLED = Disabled	

	ACTIVE("yada.jobstate.ACTIVE"),
	RUNNING("yada.jobstate.RUNNING"),
	PAUSED("yada.jobstate.PAUSED"),
	COMPLETED("yada.jobstate.COMPLETED"),
	DISABLED("yada.jobstate.DISABLED");
	
	private String messageKey;
	private YadaPersistentEnum<YadaJobState> yadaPersistentEnum;
	
	private YadaJobState(String messageKey) {
		this.messageKey = messageKey;
	}
	
	public YadaPersistentEnum<YadaJobState> toYadaPersistentEnum() {
		return yadaPersistentEnum;
	}
	
	/**
	 * @return the database id for this enum value
	 */
	public long toId() {
		return yadaPersistentEnum.getId();
	}
	
	// TODO fix generics
	public void setYadaPersistentEnum(YadaPersistentEnum yadaPersistentEnum) {
		this.yadaPersistentEnum = yadaPersistentEnum;
		
	}
	
	/**
	 * Return the localized text for this enum
	 * @param messageSource
	 * @param locale
	 * @return
	 */
	public String toString(MessageSource messageSource, Locale locale) {
		return messageSource.getMessage(messageKey, null, locale);
	}
}
