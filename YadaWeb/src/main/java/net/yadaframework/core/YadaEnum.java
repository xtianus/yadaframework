package net.yadaframework.core;
import java.util.Locale;

import org.springframework.context.MessageSource;

public interface YadaEnum {

	public String toString(MessageSource messageSource, Locale locale);

}
