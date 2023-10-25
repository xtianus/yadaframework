package net.yadaframework.commerce.persistence.entity;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

import net.yadaframework.persistence.YadaMoney;

public class TestYadaMoney {

	@Test
	public void testRounding() {
		double toTest = 12345.6789;
		YadaMoney yadaMoney = new YadaMoney(toTest);
		double pow = 1;
		for (int i = 0; i < 5; i++) {
			double value = yadaMoney.getRoundValue(i);
			double check =  Math.round(toTest*pow)/pow;
			// System.out.println("value = " + value);
			assertEquals(value, check, 0);
			pow = pow * 10;
		}
		assertEquals(yadaMoney.toString(Locale.ENGLISH), "12345.68");
//		yadaMoney.setAmount(1230000);
//		System.out.println(yadaMoney);
//		yadaMoney.setAmount(123000);
//		System.out.println(yadaMoney);
	}
}
