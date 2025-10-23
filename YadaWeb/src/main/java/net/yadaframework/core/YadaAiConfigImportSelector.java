package net.yadaframework.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Conditionally imports YadaAiConfig only if YadaAi module is present on the classpath.
 * This allows YadaAi to be an optional module that doesn't cause errors when not included.
 */
public class YadaAiConfigImportSelector implements ImportSelector {
	private final static Logger log = LoggerFactory.getLogger(YadaAiConfigImportSelector.class);
	
	private static final String YADA_AI_CONFIG_CLASS = "net.yadaframework.ai.YadaAiConfig";

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		try {
			// Try to load YadaAiConfig class
			Class.forName(YADA_AI_CONFIG_CLASS);
			log.debug("YadaAi module detected - importing YadaAiConfig");
			return new String[] { YADA_AI_CONFIG_CLASS };
		} catch (ClassNotFoundException e) {
			log.debug("YadaAi module not found on classpath - skipping YadaAiConfig import");
			return new String[] {};
		}
	}
}
