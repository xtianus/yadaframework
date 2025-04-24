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
		// NO: The precedence is higher than the standard "th:" dialect so that th: attributes are processed before.
		// The only th: tag that doesn't work is th:field because it checks which tag it's being used on
		// and skips all custom tags.
		super("Yada Dialect", YadaDialectUtil.YADA_PREFIX, StandardDialect.PROCESSOR_PRECEDENCE);
		this.config = config;
	}

	@Override
    public Set<IProcessor> getProcessors(final String dialectPrefix) {
        final Set<IProcessor> processors = new HashSet<>();
        // processors.add(new YadaHrefAttrProcessor(dialectPrefix, config));
        processors.add(new YadaSrcAttrProcessor(dialectPrefix, config));
        processors.add(new YadaSrcsetAttrProcessor(dialectPrefix, config));
        processors.add(new YadaAjaxAttrProcessor(dialectPrefix)); // yada:ajax
        processors.add(new YadaHrefAttrProcessor(dialectPrefix)); // yada:href
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "ajaxElementLoader", "data-yadaAjaxElementLoader", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "popover", "data-yadaPopover", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "formGroup", "data-yadaFormGroup", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "triggerInViewport", "data-yadaTriggerInViewport", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "paginationHistory", "data-yadaPaginationHistory", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "submitHandler", "data-yadaSubmitHandler", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "successHandler", "data-yadaSuccessHandler", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "updateOnSuccess", "data-yadaUpdateOnSuccess", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "showAjaxFeedback", "data-yadaShowAjaxFeedback", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "dropUpload", "data-yadaDropUpload", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "dropUploadAccept", "data-yadaDropUploadAccept", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "singleFileOnly", "data-yadaSingleFileOnly", config));
        // For append, use the $append() extended selector syntax in updateOnSuccess
        // processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "appendOnSuccess", "data-yadaAppendOnSuccess", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "deleteOnSuccess", "data-yadaDeleteOnSuccess", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "confirm", "data-yadaConfirm", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "title", "data-yadaTitle", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "okButton", "data-yadaOkButton", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "cancelButton", "data-yadaCancelButton", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "okShowsPrevious", "data-yadaOkShowsPrevious", config));
        processors.add(new YadaSimpleAttrProcessor(dialectPrefix, "hash", "data-yadaHash", config));
        processors.add(new YadaNewlineTextAttrProcessor(dialectPrefix, false)); // unewlinetext
        processors.add(new YadaNewlineTextAttrProcessor(dialectPrefix, true));	// newlinetext
        processors.add(new YadaBrOnFirstSpaceAttrProcessor(dialectPrefix, false));	// ubrspace
        processors.add(new YadaBrOnFirstSpaceAttrProcessor(dialectPrefix, true));	// brspace
        processors.add(new YadaInputTagProcessor(dialectPrefix, config));	// yada:input
        processors.add(new YadaInputCounterTagProcessor(dialectPrefix, config));	// yada:inputCounter
        processors.add(new YadaTextareaTagProcessor(dialectPrefix, config));	// yada:textarea
        // TODO move YadaActionUploadAttrProcessor to a yada security dialect
        // processors.add(new YadaActionUploadAttrProcessor(dialectPrefix));
        // Rimuove lo yada:xxx namespace dal tag <html>
        processors.add(new StandardXmlNsTagProcessor(TemplateMode.HTML, dialectPrefix));
        return processors;
    }
}
