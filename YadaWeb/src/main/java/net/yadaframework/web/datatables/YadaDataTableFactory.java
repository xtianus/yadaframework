package net.yadaframework.web.datatables;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class YadaDataTableFactory {
	@Autowired MessageSource messageSource; 

	public YadaDataTable create(String id, Locale locale) {
		YadaDataTable result = new YadaDataTable(id, messageSource, locale);
		return result;
	}
}
