package net.yadaframework.web.datatables.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.web.datatables.YadaDataTable;

public class YadaDataTableLanguage extends YadaFluentBase<YadaDataTable> {
	protected String languageBaseUrl = "https://cdn.datatables.net/plug-ins/2.1.8/i18n/";
	protected Map<String, String> languageMap = new HashMap<>();
	
	protected YadaDataTableLanguage(String languageBaseUrl, YadaDataTable parent) {
		super(parent);
		if (StringUtils.isEmpty(languageBaseUrl)) {
			languageBaseUrl = "https://cdn.datatables.net/plug-ins/2.1.8/i18n/";
		}
		languageBaseUrl = languageBaseUrl.replaceAll("^@\\{|\\}$", ""); // Strip any enclosing @{}
		this.languageBaseUrl = StringUtils.appendIfMissing(languageBaseUrl, "/");
		// Add default translations
		languageMap.put("it", "it-IT.json");
		languageMap.put("de", "de-DE.json");
		languageMap.put("es", "es-ES.json");
		languageMap.put("fr", "fr-FR.json");
	}

	/**
	 * Add a language definition. Default values for it, de, es and fr are already set.
	 * See <a href="https://datatables.net/plug-ins/i18n/">Internationalisation plug-ins</a> for a list of available translations.
	 * The language must also be enabled in the application configuration under the &lt;i18n> tag.
	 * @param language the language ISO2 code like "pt" as used by Java Locale.
	 * @param jsonFile the JSON file name to use in the language url, e.g. "pt-PT.json", can be found from the DataTables site.
	 * @return an instance of YadaDataTableLanguage for method chaining
	 */
	public YadaDataTableLanguage dsAddLanguage(String language, String jsonFile) {
		languageMap.put(language, jsonFile);
		return this;
	}
}
