package net.yadaframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.junit.jupiter.api.Test;

import net.yadaframework.web.YadaViews;

/**
 * Verifies that Bootstrap-version-specific view names are resolved through the folder that carries
 * the version, which is the only place where the version appears since 0.7.9.
 */
class YadaConfigurationBootstrapViewTest {

	private YadaConfiguration buildConfiguration(String bootstrapVersionElement) throws Exception {
		String xml = "<combined><config>" + bootstrapVersionElement + "</config></combined>";
		XMLConfiguration xmlConfiguration = new XMLConfiguration();
		xmlConfiguration.setExpressionEngine(new XPathExpressionEngine());
		new org.apache.commons.configuration2.io.FileHandler(xmlConfiguration).load(new StringReader(xml));
		YadaConfiguration config = new YadaConfiguration() {};
		config.setConfiguration(xmlConfiguration);
		return config;
	}

	@Test
	void modalViewsAreResolvedInTheVersionFolder() throws Exception {
		YadaConfiguration b3 = buildConfiguration("<bootstrapVersion>3</bootstrapVersion>");
		assertEquals("/yada/b3/modalConfirm", b3.getBootstrapView(YadaViews.CONFIRM));
		assertEquals("/yada/b3/modalNotify", b3.getBootstrapView(YadaViews.AJAX_NOTIFY));
		assertEquals("/yada/b3/modalGeneric", b3.getBootstrapView(YadaViews.MODAL_GENERIC));

		YadaConfiguration b4 = buildConfiguration("<bootstrapVersion>4</bootstrapVersion>");
		assertEquals("/yada/b4/modalConfirm", b4.getBootstrapView(YadaViews.CONFIRM));
	}

	@Test
	void formFragmentsAreResolvedInTheVersionFolder() throws Exception {
		YadaConfiguration b3 = buildConfiguration("<bootstrapVersion>3</bootstrapVersion>");
		assertEquals("/yada/form/b3/text", b3.getBootstrapFormView("text"));
		assertEquals("/yada/form/b3/fileUpload", b3.getBootstrapFormView("fileUpload"));
	}

	@Test
	void theDefaultVersionIsFive() throws Exception {
		YadaConfiguration defaultConfig = buildConfiguration("");
		assertEquals("/yada/b5/modalConfirm", defaultConfig.getBootstrapView(YadaViews.CONFIRM));
		assertEquals("/yada/form/b5/text", defaultConfig.getBootstrapFormView("text"));
	}

	@Test
	void theNotifyModalViewDefaultsToTheResolvedView() throws Exception {
		YadaConfiguration b3 = buildConfiguration("<bootstrapVersion>3</bootstrapVersion>");
		assertEquals("/yada/b3/modalNotify", b3.getNotifyModalView());
	}

	@Test
	void theNotifyModalViewCanBeConfigured() throws Exception {
		YadaConfiguration configured = buildConfiguration(
			"<bootstrapVersion>3</bootstrapVersion><paths><notificationModalView>/myViews/myModal</notificationModalView></paths>");
		assertEquals("/myViews/myModal", configured.getNotifyModalView());
	}
}
