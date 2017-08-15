package net.yadaframework.commerce.persistence.entity;

import org.junit.Test;

import net.yadaframework.persistence.YadaMoney;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

public class TestYadaMoney {

	@Test
	public void testRounding() {
		long toTest = 123456789l;
		YadaMoney yadaMoney = new YadaMoney(toTest);
		double pow = 1;
		for (int i = 0; i < 5; i++) {
			double value = yadaMoney.toCurrency(i);
			double check =  Math.round(toTest*pow/10000d)/pow;
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
