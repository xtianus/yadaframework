package net.yadaframework.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

import net.yadaframework.persistence.entity.YadaPersistentEnum;

class YadaPersistentEnumLocaleTest {

	@Test
	void currentLocaleUsesRegionalTextWhenPresentAndLanguageTextOtherwise() {
		YadaPersistentEnum<?> status = new YadaPersistentEnum<>();
		status.setLangToText(Map.of("fr", "Solde", "fr_CA", "Acquitte"));
		LocaleContext previous = LocaleContextHolder.getLocaleContext();
		try {
			LocaleContextHolder.setLocale(Locale.CANADA_FRENCH);
			assertEquals("Acquitte", status.getLocalText());
			LocaleContextHolder.setLocale(Locale.FRANCE);
			assertEquals("Solde", status.getLocalText());
		} finally {
			LocaleContextHolder.setLocaleContext(previous);
		}
	}
}
