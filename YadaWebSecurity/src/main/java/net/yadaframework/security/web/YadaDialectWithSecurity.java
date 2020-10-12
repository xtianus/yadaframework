package net.yadaframework.security.web;

import java.util.Set;

import org.thymeleaf.processor.IProcessor;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.web.dialect.YadaDialect;

public class YadaDialectWithSecurity extends YadaDialect {
	
	/**
	 * 
	 * @param config
	 */
	public YadaDialectWithSecurity(YadaConfiguration config) {
		super(config);
	}

	@Override
    public Set<IProcessor> getProcessors(final String dialectPrefix) {
		Set<IProcessor> processors = super.getProcessors(dialectPrefix);
        processors.add(new YadaActionUploadAttrProcessor(dialectPrefix));
        return processors;
    }
}
