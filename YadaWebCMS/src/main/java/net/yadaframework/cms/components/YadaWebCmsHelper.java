package net.yadaframework.cms.components;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Helper for html fragments
 */
@Component
public class YadaWebCmsHelper {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * Returns a space-separated list of classes taken from the flagClasses list when the corresponding flag on target is true.
	 * Non-existing flags are ignored.
	 * @param target some object with boolean attributes, like an Entity being sorted by entitySorter.html
	 * @param flagClasses attributes that become classes when true, e.g. "enabled important expired"
	 * @return space-separated list of classes, or empty string
	 */
	public String getFlagClasses(Object target, String flagClasses) {
		flagClasses = StringUtils.trimToNull(flagClasses);
		if (flagClasses==null) {
			return "";
		}
       StringBuilder result = new StringBuilder();
        String[] flags = flagClasses.split("\\s+");

        for (String flag : flags) {
        	String getterName = "is" + Character.toUpperCase(flag.charAt(0)) + flag.substring(1);
            try {
            	Method getter = target.getClass().getMethod(getterName);
                Object value = getter.invoke(target);   
                if (value instanceof Boolean && (Boolean) value) {
                    result.append(flag).append(" ");
                }
//                Field field = target.getClass().getDeclaredField(flag);
//                field.setAccessible(true);
//                if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
//                    if (field.getBoolean(target)) {
//                        result.append(flag).append(" ");
//                    }
//                }
            } catch (Exception e) {
            	log.debug("Can't get field {} (ignored)", flag);
            }
        }

        return result.toString().trim();		
	}
}
