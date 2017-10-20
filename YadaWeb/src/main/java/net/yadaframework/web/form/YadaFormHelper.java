package net.yadaframework.web.form;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.yadaframework.core.YadaLocalEnum;

/**
 * Utility methods to be used in thymeleaf forms
 *
 */
@Component
public class YadaFormHelper {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Check if a class is a YadaLocalEnum. For example YadaJobState is a YadaLocalEnum.
	 * Used by list.html to check for enum options.
	 * @param someClass the class to check (or a non-class object, in which case the result will be false)
	 * @return true if the class implements YadaLocalEnum
	 */
	public boolean isYadaLocalEnum(Object someClass) {
		try {
			return (someClass instanceof Class) && YadaLocalEnum.class.isAssignableFrom((Class<?>)someClass);
		} catch (Exception e) {
			log.debug("Not a YadaLocalEnum", e);
		}
		return false;
	}
}
