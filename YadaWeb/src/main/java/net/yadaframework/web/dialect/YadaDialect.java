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
		// The precedence is higher than the standard "th:" dialect so that th: attributes are processed before.
		// The only th: tag that doesn't work is th:field because it checks which tag it's being used on
		// and skips all custom tags.
		super("Yada Dialect", YadaDialectUtil.YADA_PREFIX, StandardDialect.PROCESSOR_PRECEDENCE+1);
		this.config = config;
	}

	@Override
    public Set<IProcessor> getProcessors(final String dialectPrefix) {
        final Set<IProcessor> processors = new HashSet<>();
        processors.add(new YadaHrefAttrProcessor(dialectPrefix, config));
        processors.add(new YadaSrcAttrProcessor(dialectPrefix, config));
        processors.add(new YadaSrcsetAttrProcessor(dialectPrefix, config));
        processors.add(new YadaAjaxAttrProcessor(dialectPrefix));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "paginationHistory", "data-yadaPaginationHistory"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "submitHandler", "data-yadaSubmitHandler"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "successHandler", "data-yadaSuccessHandler"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "updateOnSuccess", "data-yadaUpdateOnSuccess"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "deleteOnSuccess", "data-yadaDeleteOnSuccess"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "confirm", "data-yadaConfirm"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "title", "data-yadaTitle"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "okButton", "data-yadaOkButton"));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "cancelButton", "data-yadaCancelButton"));
        processors.add(new YadaNewlineTextAttrProcessor(dialectPrefix, false)); // unewlinetext
        processors.add(new YadaNewlineTextAttrProcessor(dialectPrefix, true));	// newlinetext
        processors.add(new YadaBrOnFirstSpaceAttrProcessor(dialectPrefix, false));	// ubrspace
        processors.add(new YadaBrOnFirstSpaceAttrProcessor(dialectPrefix, true));	// brspace
        processors.add(new YadaInputTagProcessor(dialectPrefix, config));	// yada:input
        processors.add(new YadaInputCounterTagProcessor(dialectPrefix, config));	// yada:inputCounter
        // TODO move YadaActionUploadAttrProcessor to a yada security dialect
        // processors.add(new YadaActionUploadAttrProcessor(dialectPrefix));
        // Rimuove lo yada:xxx namespace dal tag <html>
        processors.add(new StandardXmlNsTagProcessor(TemplateMode.HTML, dialectPrefix));
        return processors;
    }
}
