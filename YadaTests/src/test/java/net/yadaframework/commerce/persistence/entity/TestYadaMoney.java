package net.yadaframework.commerce.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

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

		// An explicit Unicode numbering-system extension is the stable choice on every JDK
		assertEquals(yadaMoney.toString(Locale.forLanguageTag("ar-u-nu-arab")), "١٢٣٤٥٫٦٨");
		assertEquals(yadaMoney.toString(Locale.forLanguageTag("ar-u-nu-latn")), "12345.68");

		// CLDR changed the default numbering system of the plain "ar" locale in JDK 24:
		// Arabic-Indic digits before, Latin digits since
		String expectedPlainArabic = Runtime.version().feature() >= 24 ? "12345.68" : "١٢٣٤٥٫٦٨";
		assertEquals(yadaMoney.toString(Locale.forLanguageTag("ar")), expectedPlainArabic);
//		yadaMoney.setAmount(1230000);
//		System.out.println(yadaMoney);
//		yadaMoney.setAmount(123000);
//		System.out.println(yadaMoney);
	}
}
