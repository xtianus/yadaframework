package net.yadaframework.web.datatables.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.datatables.YadaDataTable;

// Class not needed: 
// as we now have the Locale in the table, the table is locale-specific so the full url of the language file can be set on the instance, no need to have them all
@Deprecated 
public class YadaDataTableLanguage extends YadaFluentBase<YadaDataTable> {
	private String languageBaseUrl = "https://cdn.datatables.net/plug-ins/2.1.8/i18n/";
	private Map<String, String> languageMap = new HashMap<>();
	
	public YadaDataTableLanguage(String languageBaseUrl, YadaDataTable parent) {
		super(parent);
		languageBaseUrl = StringUtils.appendIfMissing(languageBaseUrl, "/");
		this.languageBaseUrl = YadaWebUtil.INSTANCE.ensureThymeleafUrl(languageBaseUrl);
		languageMap.put("it", "it-IT.json");
		languageMap.put("de", "de-DE.json");
		languageMap.put("es", "es-ESjson");
		languageMap.put("fr", "fr-FR.json");
	}

	/**
	 * Add a language definition. Default values for it, de, es and fr are already set.
	 * See <a href="https://datatables.net/plug-ins/i18n/">Internationalisation plug-ins</a> for a list of available translations.
	 * @param language the language code like "cs"
	 * @param jsonFile the JSON file name inside the languageBaseUrl, e.g. "cs.json"
	 * @return an instance of YadaDataTableLanguage for method chaining
	 */
	public YadaDataTableLanguage dsAddLanguage(String language, String jsonFile) {
		languageMap.put(language, jsonFile);
		return this;
	}
	
	public String getLanguageUrl(String language) {
		String languageFile = languageMap.get(language);
		if (languageFile!=null) {
			return languageBaseUrl + languageFile;
		}
		return "";
	}
	
}
