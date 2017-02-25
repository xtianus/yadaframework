package net.yadaframework.web;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Date serializer that produces a date in the format "yyyy-MM-dd" using the thread's timezone and locale
 *
 */
public class YadaJsonDateSimpleSerializer extends JsonSerializer<Date> {
	
	@Override
	public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
		if (date!=null) {
			Locale locale = LocaleContextHolder.getLocale();
			TimeZone timeZone = LocaleContextHolder.getTimeZone();
			FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd", timeZone, locale);
			jsonGenerator.writeString(fastDateFormat.format(date));
		} else {
			jsonGenerator.writeString("");
		}
	}
}
