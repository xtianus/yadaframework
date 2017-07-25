package net.yadaframework.components;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.yadaframework.core.YadaJpaConfig;
import net.yadaframework.core.YadaTestConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {YadaTestConfig.class, YadaJpaConfig.class})
public class TestYadaJobScheduler extends AbstractJUnit4SpringContextTests {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired private YadaJobScheduler yadaJobScheduler;

	@Test
	public void firstTest() {
		 assert yadaJobScheduler !=null;
	}

	
}
