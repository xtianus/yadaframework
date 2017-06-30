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
	//	yada.jobstate.active = Active
	//	yada.jobstate.running = Running
	//	yada.jobstate.paused = Paused
	//	yada.jobstate.completed = Completed
	//	yada.jobstate.disabled = Disabled	

	ACTIVE("yada.jobstate.active"),
	RUNNING("yada.jobstate.running"),
	PAUSED("yada.jobstate.paused"),
	COMPLETED("yada.jobstate.completed"),
	DISABLED("yada.jobstate.disabled");
	
	private String messageKey;
	
	private YadaJobState(String messageKey) {
		this.messageKey = messageKey;
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
