package net.yadaframework.web.dialect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that handles the convenience attribute yada:href="url".
 * The result will be a data-yadaHref="url" attribute, and yadaAjax added to class.
 * Identical to yada:ajax
 * @see YadaAjaxAttrProcessor
 * @since 0.7.0
 */
public class YadaHrefAttrProcessor extends YadaAjaxAttrProcessor {
	private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String ATTR_NAME = "href";

	/**
	 * @param config
	 */
	public YadaHrefAttrProcessor(final String dialectPrefix) {
        super(dialectPrefix, ATTR_PRECEDENCE, ATTR_NAME);

	}

}
