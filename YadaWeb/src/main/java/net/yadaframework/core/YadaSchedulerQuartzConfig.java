package net.yadaframework.core;

import org.quartz.JobListener;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import net.yadaframework.async.YadaQuartzJobFactory;

@Configuration
public class YadaSchedulerQuartzConfig {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	ApplicationContext applicationContext;
	
	/**
	 * Can be overridden in a SchedulerConfig to create a different factory
	 */
	protected YadaQuartzJobFactory createJobFactoryInstance() {
		return new YadaQuartzJobFactory();
	}
	
	/**
	 * To be overridden in a SchedulerConfig
	 */
	protected JobListener[] createGlobalJobListeners() {
		return null;
	}
	
	/**
	 * To be overridden in a SchedulerConfig
	 */
	protected TriggerListener[] createGlobalTriggerListeners() {
		return null;
	}

	@Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
		try {
			Class.forName("org.quartz.SchedulerException");
			YadaQuartzJobFactory yadaQuartzJobFactory = createJobFactoryInstance();
			yadaQuartzJobFactory.setApplicationContext(applicationContext);
			SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
			schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext");
			schedulerFactoryBean.setSchedulerName("yadaScheduler");
			schedulerFactoryBean.setJobFactory(yadaQuartzJobFactory);
			Resource configFile = applicationContext.getResource("classpath:quartz.properties"); // Configuration file (optional)
			if (configFile.exists()) {
				log.info("Loading Quartz configuration from {}", configFile.toString());
				schedulerFactoryBean.setConfigLocation(configFile);
			}
			JobListener[] jobListeners = createGlobalJobListeners();
			TriggerListener[] triggerListener = createGlobalTriggerListeners();
			if (jobListeners!=null) {
				schedulerFactoryBean.setGlobalJobListeners(jobListeners);
			}
			if (triggerListener!=null) {
				schedulerFactoryBean.setGlobalTriggerListeners(triggerListener);
			}
			log.info("Quartz scheduler activated");
			return schedulerFactoryBean;
		} catch (ClassNotFoundException e) {
			log.info("No Quartz library in package - scheduler not started");
			return null;
		}
    } 
	

}

