package net.yadaframework.web.dialect;

import java.util.HashSet;
import java.util.Set;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardXmlNsTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

import net.yadaframework.core.YadaConfiguration;

public class YadaDialect extends AbstractProcessorDialect {
	private YadaConfiguration config;
	
	/**
	 * 
	 * @param config
	 */
	public YadaDialect(YadaConfiguration config) {
        // We will set this dialect the same "dialect processor" precedence as
        // the Standard Dialect, so that processor executions can interleave.
		super("Yada Dialect", "yada", StandardDialect.PROCESSOR_PRECEDENCE);
		this.config = config;
	}

//	@Override
//	public String getPrefix() {
//		return "yada";
//	}

	 @Override
    public Set<IProcessor> getProcessors(final String dialectPrefix) {
        final Set<IProcessor> processors = new HashSet<IProcessor>();
        processors.add(new YadaHrefAttrProcessor(dialectPrefix, config));
        processors.add(new YadaSrcAttrProcessor(dialectPrefix, config));
        processors.add(new YadaAjaxAttrProcessor(dialectPrefix));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "updateOnSuccess", "data-updateOnSuccess"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "confirm", "data-yadaConfirm"));
        // TODO move YadaActionUploadAttrProcessor to a yada security dialect
        // processors.add(new YadaActionUploadAttrProcessor(dialectPrefix));
        // Rimuove lo yada:xxx namespace dal tag <html>
        processors.add(new StandardXmlNsTagProcessor(TemplateMode.HTML, dialectPrefix));
        return processors;
    }
}
