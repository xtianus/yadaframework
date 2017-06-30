package net.yadaframework.core;
import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.persistence.entity.YadaJobState;

/**
 * This interface must be applied to a normal enum that needs to be localized. 
 * Example:
 * <pre>
 * public enum YadaJobState implements YadaLocalEnum<YadaJobState> {
 * }
 * </pre>
 * @param <E> the enum
 * @see YadaJobState
 */
public interface YadaLocalEnum<E extends Enum<E>> {

	/**
	 * Convert the current enum to a localized string
	 * @param messageSource
	 * @param locale
	 * @return
	 */
	String toString(MessageSource messageSource, Locale locale);

	// The following methods are implemented by enum: there's no need to implement them
	
	/**
	 * Already implemented by enum
	 * @return
	 */
	String name();
	/**
	 * Already implemented by enum
	 * @return
	 */
	int ordinal();
	/**
	 * Already implemented by enum
	 * @return
	 */
	Class<E> getDeclaringClass();

}
