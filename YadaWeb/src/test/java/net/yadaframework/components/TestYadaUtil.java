package net.yadaframework.components;
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = {YadaTestConfig.class})
public class TestYadaUtil {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void formatTimeInterval() {
		assertEquals("00:00", YadaUtil.formatTimeInterval(0, TimeUnit.DAYS));
		assertEquals("00:20", YadaUtil.formatTimeInterval(20, TimeUnit.SECONDS));
		assertEquals("01:00", YadaUtil.formatTimeInterval(60, TimeUnit.SECONDS));
		assertEquals("01:10", YadaUtil.formatTimeInterval(70, TimeUnit.SECONDS));
		assertEquals("01:01:10", YadaUtil.formatTimeInterval(3600+70, TimeUnit.SECONDS));
		assertEquals("23:01:10", YadaUtil.formatTimeInterval(23*3600+70, TimeUnit.SECONDS));
		assertEquals("23:00:00", YadaUtil.formatTimeInterval(23, TimeUnit.HOURS));
		assertEquals("1d:00:00:00", YadaUtil.formatTimeInterval(24, TimeUnit.HOURS));
		assertEquals("123d:10:22:15", YadaUtil.formatTimeInterval(123*24*3600+10*3600+22*60+15, TimeUnit.SECONDS));
	}

	
}
