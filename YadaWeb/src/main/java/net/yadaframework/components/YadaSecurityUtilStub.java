package net.yadaframework.components;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.exceptions.YadaInvalidUsageException;

/**
 * SecurityUtils methods that are used in YadaWeb only when YadaWebSecurity is present.
 * Avoids circular dependency with YadaWebSecurity.
 */
@Component
public class YadaSecurityUtilStub implements ApplicationListener<ContextRefreshedEvent> {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final String beanNname = "yadaSecurityUtil";
	private Object yadaSecurityUtilInstance = null;
	private boolean alreadyInitialized = false;
	
	@Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
		if (!alreadyInitialized) {
			alreadyInitialized = true;
			yadaSecurityUtilInstance = YadaUtil.getBean(beanNname);
			if (yadaSecurityUtilInstance == null) {
				log.debug("No {} bean found - YadaWebSecurity is not on the classpath", beanNname);
			}
		}
	}

	 /**
     * Checks if the current user has access to the specified path
     * @param request The current HttpServletRequest
     * @param path The path to check access for, e.g. "/dashboard"
     * @return true if access is granted, false otherwise
     */
	public boolean checkUrlAccess(HttpServletRequest request, String path) {
		if (yadaSecurityUtilInstance!=null) {
			try {
				Method method = yadaSecurityUtilInstance.getClass().getMethod(
				    "checkUrlAccess", 
				    HttpServletRequest.class, 
				    String.class
				);
				return (boolean) method.invoke(yadaSecurityUtilInstance, request, path);
			} catch (Exception e) {
				// This should not happen if everything is correctly configured and used
				throw new YadaInvalidUsageException("Error invoking checkUrlAccess in {}", yadaSecurityUtilInstance.getClass().getName(), e);
			}
		}
		return true; // Always allow access if YadaWebSecurity is not present
	}
}
