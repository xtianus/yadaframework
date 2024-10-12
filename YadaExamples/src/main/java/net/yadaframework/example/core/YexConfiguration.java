package net.yadaframework.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.core.YadaConfiguration;

// Use XPath
public class YexConfiguration extends YadaConfiguration {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	// Cached values
	// Stored for speed as they don't change at runtime
	private Integer sessionTimeoutMinutes=null;
	
	public Integer getSessionTimeoutMinutes() {
		if (sessionTimeoutMinutes==null) {
			sessionTimeoutMinutes = configuration.getInt("config/security/sessionTimeoutMinutes", -1);
			if (sessionTimeoutMinutes==-1) {
				// The session timeout must be configured with the same value set in web.xml 
				// otherwise the session might never expire because the javascript code would
				// keep it awake.
				log.error("Session timeout not configured correctly (defaulting to 20). Please configure session timeout in conf.webapp.xml equal to web.xml");
				sessionTimeoutMinutes = 20;
			}
		}
		return sessionTimeoutMinutes;
	}

}
