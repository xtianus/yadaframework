package net.yadaframework.async;
import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * A copy of SpringBeanJobFactory (from spring-context-support-4.2.5.RELEASE) that pulls job instances from the 
 * Spring ApplicationContext. Jobs must be @Component, either singleton or prototype. Remember that singletons must be thread-safe. 
 * @see SpringBeanJobFactory
 */
public class YadaQuartzJobFactory extends AdaptableJobFactory implements SchedulerContextAware {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	private ApplicationContext applicationContext;

	private String[] ignoredUnknownProperties;

	private SchedulerContext schedulerContext;

	/**
	 * Create the job instance, populating it with property values taken
	 * from the scheduler context, job data map and trigger data map.
	 */
	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Object job = getFromApplicationContext(bundle);
		if (job==null) {
			return null;
		}
		initJob(bundle, job);
		return job;
	}

	/**
	 * Specify the unknown properties (not found in the bean) that should be ignored.
	 * <p>Default is {@code null}, indicating that all unknown properties
	 * should be ignored. Specify an empty array to throw an exception in case
	 * of any unknown properties, or a list of property names that should be
	 * ignored if there is no corresponding property found on the particular
	 * job class (all other unknown properties will still trigger an exception).
	 */
	public void setIgnoredUnknownProperties(String... ignoredUnknownProperties) {
		this.ignoredUnknownProperties = ignoredUnknownProperties;
	}

	@Override
	public void setSchedulerContext(SchedulerContext schedulerContext) {
		this.schedulerContext = schedulerContext;
	}

	/**
	 * Return whether the given job object is eligible for having
	 * its bean properties populated.
	 * <p>The default implementation ignores {@link QuartzJobBean} instances,
	 * which will inject bean properties themselves.
	 * @param jobObject the job object to introspect
	 * @see QuartzJobBean
	 */
	protected boolean isEligibleForPropertyPopulation(Object jobObject) {
		return (!(jobObject instanceof QuartzJobBean));
	}

	protected void initJob(TriggerFiredBundle bundle, Object job) {
		// The following code is copied from SpringBeanJobFactory in spring-context-support-4.2.5.RELEASE
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(job);
		if (isEligibleForPropertyPopulation(bw.getWrappedInstance())) {
			MutablePropertyValues pvs = new MutablePropertyValues();
			if (schedulerContext != null) {
				pvs.addPropertyValues(this.schedulerContext);
			}
			pvs.addPropertyValues(bundle.getJobDetail().getJobDataMap());
			pvs.addPropertyValues(bundle.getTrigger().getJobDataMap());
			if (this.ignoredUnknownProperties != null) {
				for (String propName : this.ignoredUnknownProperties) {
					if (pvs.contains(propName) && !bw.isWritableProperty(propName)) {
						pvs.removePropertyValue(propName);
					}
				}
				bw.setPropertyValues(pvs);
			}
			else {
				bw.setPropertyValues(pvs, true);
			}
		}
	}

	protected Object getFromApplicationContext(TriggerFiredBundle bundle) {
		Class jobClass = bundle.getJobDetail().getJobClass();
		Object jobBean = applicationContext.getBean(jobClass);
		if (jobBean==null) {
			log.error("Can't find bean of type '{}' in ApplicationContext", jobClass);
		}
		return jobBean;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
}
