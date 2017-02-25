package net.yadaframework.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

// Classe copiata da http://www.thymeleaf.org/doc/html/Thymeleaf-Spring3.html#integration-with-requestdatavalueprocessor
/**
 * La formattazione automatica della data usa il formato html5 "yyyy-MM-dd" indipendentemente dal locale, altrimenti la cosa non può funzionare.
 * Ho però aggiunto un metodo 
 */
@Component
public class YadaDateFormatter implements Formatter<Date> {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired MessageSource messageSource;
    
    // Questo è il formato usato da html5 quando si ha un campo type="date"
    private final SimpleDateFormat html5Formatter = new SimpleDateFormat("yyyy-MM-dd");  // Occhio a non usare ISO perchè usa UTC invece di CEST e mi sballa tutto (?)

    public YadaDateFormatter() {
        super();
    }

    /**
     * Chiamato automaticamente da Spring per convertire una stringa in un oggetto Date con il formato html5
     */
    public Date parse(final String text, final Locale locale) throws ParseException {
		try {
			return html5Formatter.parse(text);
		} catch (ParseException e1) {
			log.error("Can't format date '{}'", text, e1);
			return null;
		}
    }
    
    /**
     * Chiamato automaticamente da Spring per convertire un oggetto Date in una stringa con il formato html5
     */
    public String print(final Date object, final Locale locale) {
    	return html5Formatter.format(object);
   }

    /**
     * Metodo di utilità da usare per convertire una stringa in una data formattata come specificato in messages.properties (quindi locale-dependent)
     */
    public Date parseLocal(final String text, final Locale locale) throws ParseException {
    	final SimpleDateFormat dateFormat = createDateFormat(locale);
    	try {
    		return dateFormat.parse(text);
    	} catch (ParseException e) {
    		// Riprovo con un formatter ISO
    		log.error("Invalid date format for '{}', trying HTML5 format", text);
    		try {
    			return html5Formatter.parse(text);
    		} catch (ParseException e1) {
    			log.error("Can't format date '{}'", text, e1);
    			return null;
    		}
    	}
    }

    /**
     * Metodo di utilità da usare per convertire una data in una stringa formattata come specificato in messages.properties (quindi locale-dependent).
     * Si può usare in pagina così: 
     * th:text="${@yadaDateFormatter.printLocal(story.publishDate, #locale)}"
     * Però non è molto meglio dello standard thymeleaf 
     * th:with="df=#{date.format}" th:text="${#calendars.format(story.publishDate, df)}"
     */
    public String printLocal(final Date object, final Locale locale) {
    	if (object!=null) {
    		final SimpleDateFormat dateFormat = createDateFormat(locale);
    		return dateFormat.format(object);
    	}
    	log.debug("Null date in printLocal");
    	return "";
    }
    
    private SimpleDateFormat createDateFormat(final Locale locale) {
        final String format = this.messageSource.getMessage("date.format", null, locale);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(format, locale);
        dateFormat.setLenient(false);
        return dateFormat;
    }
    
}