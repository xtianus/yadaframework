package net.yadaframework.core;

import java.io.File;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.combined.ReloadingCombinedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.format.Formatter;

import net.yadaframework.exceptions.YadaConfigurationException;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.persistence.entity.YadaClause;
import net.yadaframework.raw.YadaIntDimension;
import net.yadaframework.web.YadaViews;

/**
 * Classe che estende CombinedConfiguration aggiungendo metodi di gestione della configurazione specifici.
 */
public abstract class YadaConfiguration {
	private static Logger log = LoggerFactory.getLogger(YadaConfiguration.class);

	protected ImmutableHierarchicalConfiguration configuration;
	protected CombinedConfigurationBuilder builder;

	// Cached values
	// Questi valori li memorizzo perchè probabilmente verranno controllati
	// ad ogni pageview e comunque non mi aspetto che cambino a runtime
	private String uploadsDir = null;
	private String contentUrl = null;
	private String contentName = null;
	private String environment = null;
	private String version = null;
	private String yadaVersion = null;
	private String releaseDate = null;
	private String build = null;
	private String logoImage = null;
	private Boolean production = null;
	private Boolean development = null;
	private Boolean beta = null;
	private Boolean alpha = null;
	private Map<Integer, String> roleIdToKeyMap = null;
	private Map<String, Integer> roleKeyToIdMap = null;
	private Object roleMapMonitor = new Object();
	private String googleClientId = null;
	private String googleSecret = null;
	private String facebookAppId = null;
	private String facebookPageId = null;
	private String facebookSecret = null;
	private String serverAddress = null;
	private String webappAddress = null;
	private int facebookType = -1;
	private int googleType = -1;
	private String tagReservedPrefix = null;
	private int tagMaxNum = -1;
	private int tagMaxSuggested = -1;
	private int tagFilterMax = -1;
	private int maxPwdLen = -1;
	private int minPwdLen = -1;
	private String errorPageForward = null;
	private List<String> locales = null;
	private Set<Locale> localeSet = null;
	private List<Locale> localeObjects = null;
	private Map<String, String> languageToCountry = null;
	private Boolean localeAddCountry = null;
	private Boolean localePathVariableEnabled = null;
	private Locale defaultLocale = null;
	private boolean defaultLocaleChecked = false;
	private Map<String, SortedSet<Entry<Integer,String>>> localSetCache = new HashMap<>(); // Deprecated
	private String targetImageExtension=null;
	private String preserveImageExtensions=null;
	private String defaultNotifyModalView = null;
	private File uploadsFolder = null;
	private File tempFolder = null;
	private String googleApiKey = null;
	private Integer bootstrapVersion = null;

	/**
	 * Returns the configured timeout for asynchronous requests in seconds.
	 * Equivalent to the Tomcat parameter asyncTimeout, but more effective (the application-defined parameter takes precedence).
	 * @return the configured timeout in minutes or 0 for the default
	 */
	public int getAsyncTimeoutMinutes() {
		return configuration.getInt("config/asyncTimeoutMinutes", 0);
	}

	/**
	 * @return true if the embedded db should be used instead of the external MySQL
	 */
	public boolean isUseEmbeddedDatabase() {
		return configuration.getBoolean("config/database/embedded/@enabled", false);
	}
	
	/**
	 * @return the location of the data folder for the embedded database
	 */
	public String getEmbeddedDatabaseDataDir() {
		return configuration.getString("config/database/embedded/datadir", "dbembedded");
	}
	
	/**
	 * Returns a pointer to the sql file configured for loading the embedded database at startup
	 * @return the File to use for reading or null if the file is not configured or not readable
	 */
	public File getEmbeddedDatabaseSourceSql() {
		String sourceSqlPath = configuration.getString("config/database/embedded/sourceSql", null);
		if (sourceSqlPath!=null) {
			File result = new File(sourceSqlPath);
			if (result.canRead()) {
				return result;
			}
		}
		log.debug("No source sql to load at startup");
		return null;
	}
	
	/**
	 * The configured bootstrap version may be used to return the correct html for modals etc.
	 * @return the configured bootstrap version, defaults to 5
	 */
	public int getBootstrapVersion() {
		if (bootstrapVersion==null) {
			bootstrapVersion = configuration.getInt("config/bootstrapVersion", 5);
		}
		return bootstrapVersion;
	}
	
	/**
	 * Returns the configured FormattingConversionService. Use <FormattingConversionService> in config.
	 * Defaults to DefaultFormattingConversionService
	 * @return
	 */
	public Formatter<Date> getDateFormatter() {
		String dateFormatterClass = configuration.getString("config/dateFormatter", null);
		try {
			if (dateFormatterClass!=null) {
				return (Formatter<Date>) Class.forName(dateFormatterClass).newInstance();
			}
		} catch (Exception e) {
			log.error("Can't make instance of {} (ignored)", dateFormatterClass, e);
		}
		return null;
	}

	public int getTomcatHttpPort() {
		return configuration.getInt("config/tomcat/ports/http", 8080);
	}
	
	public int getTomcatHttpsPort() {
		return configuration.getInt("config/tomcat/ports/https", 8443);
	}
	
	public int getTomcatAjpPort() {
		return configuration.getInt("config/tomcat/ports/ajp", 8009);
	}
	
	public int getTomcatAjpRedirectPort() {
		return configuration.getInt("config/tomcat/ports/ajpRedirect", 8443);
	}
	
	public int getTomcatShutdownPort() {
		return configuration.getInt("config/tomcat/ports/shutdown", 8005);
	}
	
	public File getTomcatKeystoreFile() {
		return new File(configuration.getString("config/tomcat/keystore/file", "/srv/devtomcatkeystore"));
	}
	
	public String getTomcatKeystorePassword() {
		return configuration.getString("config/tomcat/keystore/password", "changeit");
	}
	
	/**
	 * Google api key (for Maps etc) read from "security.properties"
	 * @return
	 */
	public String getGoogleApiKey() {
		if (googleApiKey==null) {
			// Does not start with "config/" because it is in security.properties
			googleApiKey = configuration.getString("google/api/key", "");
		}
		return googleApiKey;
	}

	/**
	 * Gets the value of any boolean config key defined in the /config/local configuration 
	 * file (it should reside on the developers computer in a personal folder, not shared).
	 * When not defined or not a boolean, the value is false.
	 * Only available in dev env.
	 * @param name
	 * @return
	 */
	public boolean isLocalFlag(String name) {
		if (!isDevelopmentEnvironment()) {
			return false;
		}
		return configuration.getBoolean("config/local/"+name, false);
	}
	
	/**
	 * Gets the value of any config key defined in the /config/local configuration 
	 * file (it should reside on the developers computer in a personal folder, not shared).
	 * When not defined, the value is an empty string.
	 * Only available in dev env.
	 * @param name
	 * @return
	 */
	public Object getLocalConfig(String name) {
		if (!isDevelopmentEnvironment()) {
			return "";
		}
		return configuration.getString("config/local/"+name, "");
	}

	/**
	 * Returns the configured path for the notification modal.
	 * The configuration path is config/paths/notificationModalView
	 * @return
	 */
	public String getNotifyModalView() {
		if (defaultNotifyModalView==null) {
			defaultNotifyModalView = configuration.getString("config/paths/notificationModalView", YadaViews.AJAX_NOTIFY);
		}
		return defaultNotifyModalView;
	}

	/**
	 * Get the width/height of an image, for desktop, mobile and pdf
	 * @param relativeKey the key like "/product/gallery", relative to "config/dimension"
	 * @return { desktopDimension, mobileDimension, pdfDimension }, the cell is null when not configured
	 */
	protected YadaIntDimension[] getImageDimensions(String relativeKey) {
		YadaIntDimension[] result = new YadaIntDimension[3];
		result[0] = splitDimension(relativeKey, "/desktop");
		result[1] = splitDimension(relativeKey, "/mobile");
		result[2] = splitDimension(relativeKey, "/pdf");
		return result;
	}

	/**
	 * Converts a value like &lt;desktop>1920,973&lt;/desktop> to a YadaIntDimension
	 * @param relativeKey relativeKey the key like "/product/gallery", relative to "config/dimension"
	 * @param type "/desktop" or "/mobile" etc as found in the configuration xml
	 * @return YadaIntDimension or null
	 */
	private YadaIntDimension splitDimension(String relativeKey, String type) {
		String widthHeight = configuration.getString("config/dimension" + relativeKey + type, null);
		if (widthHeight!=null) {
			String[] parts = widthHeight.split(",");
			int width = Integer.parseInt(parts[0]);
			int height = Integer.parseInt(parts[1]);
			return new YadaIntDimension(width, height);
		}
		return null;
	}

	/**
	 * Returns the image extension (without dot) to use when uploading user images. Defaults to "jpg".
	 * @return
	 */
	public String getTargetImageExtension() {
		if (targetImageExtension==null) {
			targetImageExtension = configuration.getString("config/dimension/@targetImageExtension", "jpg");
			targetImageExtension = StringUtils.removeStart(targetImageExtension, "."); // Remove dot if any
		}
		return targetImageExtension;
	}

	/**
	 * Check if the image extension has to be preserved when converting.
	 * The value is taken from &lt;dimension targetImageExtension="jpg" preserveImageExtension="gif,webp">
	 * @param extensionNoDot
	 * @return
	 */
	public boolean isPreserveImageExtension(String extensionNoDot) {
		if (preserveImageExtensions==null) {
			preserveImageExtensions = configuration.getString("config/dimension/@preserveImageExtensions", "");
			preserveImageExtensions = StringUtils.remove(preserveImageExtensions, '.'); // Remove dot if any
			// Add commas for easy search
			preserveImageExtensions = ','+preserveImageExtensions.toLowerCase()+',';
		}
		return preserveImageExtensions.contains(','+extensionNoDot.toLowerCase()+',');

	}

	/**
	 * Tells if the YadaFileManager has to delete uploaded files when attaching them, or to keep them in the uploads folder
	 * for later use. true by default.
	 * @return
	 */
	public boolean isFileManagerDeletingUploads() {
		return configuration.getBoolean("config/yadaFileManager/deleteUploads", true);
	}

	/**
	 * Looks in the configuration for a list of ids, then fetches from message.properties the localized text corresponding to the ids.
	 * The id is appended to the messageBaseKey parameter after a dot.
	 * The result is a sorted set of id-text, sorted on the text.
	 * <br>
	 * Example:<br>
	 * In the config we have
	 * <pre>
	 * &lt;countries>
	 * 	&lt;countryId>1&lt;/countryId>
	 * 	&lt;countryId>2&lt;/countryId>
	 * 	&lt;countryId>3&lt;/countryId>
	 * 	&lt;countryId>4&lt;/countryId>
	 * &lt;/countries>
	 * </pre>
	 * In message.properties we have
	 * <pre>
	 * customer.country.1 = England
	 * customer.country.2 = France
	 * customer.country.3 = USA
	 * customer.country.4 = Albania
	 * </pre>
	 * The call to this method will be:
	 * <pre>
	 * config.getLocalSet("config/countries/countryId", "customer.country", locale, messageSource);
	 * </pre>
	 * The resulting set will have the countries in the following order: Albania, England, France, USA.
	 * Results are cached forever.
	 *
	 * @param configPath item in the configuration file that holds the id. There should be more than one such item in the configuration.
	 * Example: config/countries/countryId
	 * @param messageBaseKey the prefix of the message.properties key to which the id should be appended in order to retrieve the localized text.
	 * Example: customer.country
	 * @param locale
	 * @param messageSource
	 * @return
	 * @see YadaLocalEnum
	 */
	@Deprecated // Never tested and never used. You are probably better off with a YadaLocalEnum
	public SortedSet<Entry<Integer,String>> getLocalSet(String configPath, String messageBaseKey, Locale locale, MessageSource messageSource) {
		String cacheKey = configPath + messageBaseKey + locale.toString();
		SortedSet<Entry<Integer,String>> result = localSetCache.get(cacheKey);
		if (result!=null) {
			return result;
		}
		result = new TreeSet<>(new Comparator<Entry<Integer, String>>() {
			@Override
			public int compare(Entry<Integer, String> element1, Entry<Integer, String> element2) {
				return element1.getValue().compareTo(element2.getValue());
			}
		});
		List<Integer> ids = configuration.getList(Integer.class, configPath);
		for (Integer id : ids) {
			String key = messageBaseKey + "." + id;
			String localizedText = messageSource.getMessage(key, null, key, locale);
			Entry<Integer,String> entry = new AbstractMap.SimpleImmutableEntry<>(id, localizedText);
			result.add(entry);
		}
		localSetCache.put(cacheKey, result);
		return result;
	}

	public String getUploadsDirname() {
		if (uploadsDir==null) {
			uploadsDir = configuration.getString("config/paths/uploadsDir", "uploads");
		}
		return uploadsDir;
	}

// This has been removed because uploaded files should not be public.
// They should be moved to a public folder in order to show them via apache.
//	/**
//	 * Returns the url for the uploads folder
//	 * @return
//	 */
//	public String getUploadsUrl() {
//		return this.getContentUrl() + "/" + getUploadsDirname();
//	}

	/**
	 * Folder where files are uploaded before processing. Should not be a public folder.
	 * @return
	 */
	public File getUploadsFolder() {
		if (uploadsFolder==null) {
			uploadsFolder = new File(getBasePathString(), getUploadsDirname());
			if (!uploadsFolder.exists()) {
				uploadsFolder.mkdirs();
			}
		}
		return uploadsFolder;
	}

	/**
	 * Returns true if YadaEmaiService should throw an exception instead of returning false when it receives an exception on send
	 * @return
	 */
	public boolean isEmailThrowExceptions() {
		return configuration.getBoolean("config/email/@throwExceptions", false);
	}

	/**
	 * @return the url to redirect to after sending the password reset email
	 */
	public String getPasswordResetSent(Locale locale) {
		String link = configuration.getString("config/security/passwordReset/passwordResetSent", "/");
		return fixLink(link, locale);
	}

	/**
	 * @return the link to use for the registration confirmation, e.g. "/my/registrationConfirmation" or "/en/my/registrationConfirmation"
	 */
	public String getRegistrationConfirmationLink(Locale locale) {
		String link = configuration.getString("config/security/registration/confirmationLink", "/registrationConfirmation");
		return fixLink(link, locale);
	}

	private String fixLink(String link, Locale locale) {
		if (!link.endsWith("/")) {
			link = link + "/";
		}
		if (!link.startsWith("/")) {
			link = "/" + link;
		}
		if (isLocalePathVariableEnabled()) {
			// Add the locale
			link = "/" + locale.getLanguage() + link;
		}
		return link;
	}

	/**
	 * Checks if an email address has been blacklisted in the configuration
	 * @param email
	 * @return true for a blacklisted email address
	 */
	public boolean emailBlacklisted(String email) {
		if (email==null) {
			log.warn("Blacklisting null email address");
			return true;
		}
		String[] patternStrings = configuration.getStringArray("config/email/blacklistPattern");
		for (String patternString : patternStrings) {
			// (?i) is for case-insensitive match
			if (email.matches("(?i)"+patternString)) {
				log.warn("Email '{}' blacklisted by '{}'", email, patternString);
				return true;
			}
		}
		return false;
	}

	/**
	 * True if during startup YadaAppConfig should run the FlyWay migrate operation
	 * @return
	 */
	public boolean useDatabaseMigrationAtStartup() {
		return configuration.getBoolean("config/database/databaseMigrationAtStartup", false);
	}
	
	/**
	 * Name of the flyway schema history table. Uses the default name of "flyway_schema_history" when not configured. 
	 * @return the table name, never null
	 */
	public String flywayTableName() {
		return configuration.getString("config/database/flywayTableName", "flyway_schema_history");
	}
	
	/**
	 * "Out of order" flag in FlyWay 
	 * https://flywaydb.org/documentation/configuration/parameters/outOfOrder
	 * @return
	 */
	public boolean useDatabaseMigrationOutOfOrder() {
		return configuration.getBoolean("config/database/databaseMigrationAtStartup/@outOfOrder", false);
	}

	/**
	 * Returns the configured default locale.
	 * @return the default locale, or null if no default is set
	 */
	public Locale getDefaultLocale() {
		if (!defaultLocaleChecked) {
			defaultLocaleChecked = true;
			String localeString = configuration.getString("config/i18n/locale[@default='true']", null);
			if (localeString!=null) {
				try {
					String country = getCountryForLanguage(localeString);
					if (country!=null) {
						localeString += "_" + country;
					}
					defaultLocale = LocaleUtils.toLocale(localeString);
				} catch (IllegalArgumentException e) {
			    	throw new YadaConfigurationException("Locale {} is invalid", localeString);
				}
			} else {
				log.warn("No default locale has been set with <locale default=\"true\">: set a default locale if you don't want empty strings returned for missing localized values in the database");
			}
		}
		return defaultLocale;
	}

	/**
	 * True if the filter enables the use of locales in the url, like /en/mypage
	 * @return
	 */
	public boolean isLocalePathVariableEnabled() {
		if (localePathVariableEnabled==null) {
			localePathVariableEnabled = configuration.getBoolean("config/i18n/@localePathVariable", false);
		}
		return localePathVariableEnabled.booleanValue();
	}

//	/**
//	 * Returns the locale to be injected in the request.
//	 * Used when the locale string only has the language and you want to store the full language_COUNTRY locale in your request.
//	 * The configuration must be like &lt;locale>es&lt;request>es_ES&lt;/request>&lt;/locale>
//	 * @param locale
//	 * @return either the input parameter unaltered or the "request" locale if configured
//	 */
//	public String getLocaleForRequest(String locale) {
//		return configuration.getString("config/i18n/locale[text()='" + locale + "']/request", locale);
//	}

	public String getCountryForLanguage(String language) {
		if (languageToCountry==null) {
			languageToCountry = new HashMap<>();
			List<ImmutableHierarchicalConfiguration> locales = configuration.immutableConfigurationsAt("config/i18n/locale");
			for (ImmutableHierarchicalConfiguration localeConfig : locales) {
				String languageKey = localeConfig.getString(".");
				String countryValue = localeConfig.getString("./@country");
				languageToCountry.put(languageKey, countryValue);
			}
		}
		return languageToCountry.get(language);
	}

	/**
	 * True if locale paths only have the language component ("en") but you also need the country component ("US") in the request Locale
	 */
	public boolean isLocaleAddCountry() {
		if (localeAddCountry==null) {
			localeAddCountry = configuration.containsKey("config/i18n/locale/@country");
		}
		return localeAddCountry.booleanValue();
	}

	/**
	 * Get the set of configured locales in no particular order.
	 * @return
	 */
	public Set<Locale> getLocaleSet() {
		if (localeSet==null) {
			getLocaleStrings(); // Init the set
		}
		return localeSet;
	}

	/**
	 * Get a list of iso2 locales that the webapp can handle
	 * @return
	 */
	public List<String> getLocaleStrings() {
		if (locales==null) {
			locales = Arrays.asList(configuration.getStringArray("config/i18n/locale"));
			localeSet = new HashSet<Locale>();
			for (String locale : locales) {
				try {
			        Locale localeObject = LocaleUtils.toLocale(locale);
			        localeSet.add(localeObject);
			    } catch (IllegalArgumentException e) {
			    	throw new YadaConfigurationException("Locale {} is invalid", locale);
			    }
			}
		}
		return locales;
	}
	
	/**
	 * Returns the configured locales as objects, using countries if configured
	 * @return
	 */
	public List<Locale> getLocales() {
		if (localeObjects==null) {
			localeObjects = new ArrayList<Locale>();
			List<ImmutableHierarchicalConfiguration> locales = configuration.immutableConfigurationsAt("config/i18n/locale");
			for (ImmutableHierarchicalConfiguration localeConfig : locales) {
				String languageKey = localeConfig.getString(".");
				String countryValue = localeConfig.getString("./@country", null);
				Locale locale = countryValue==null?new Locale(languageKey):new Locale(languageKey, countryValue);
				localeObjects.add(locale);
			}
			localeObjects = Collections.unmodifiableList(localeObjects);
		}
		return localeObjects;
	}

	/**
	 * Returns the page to forward to after an unhandled exception or HTTP error.
	 * The value is the @RequestMapping value, without language in the path
	 * @return
	 */
	public String getErrorPageForward() {
		if (errorPageForward==null) {
			errorPageForward = configuration.getString("/config/paths/errorPageForward", "/");
		}
		return errorPageForward;
	}

	public boolean isBeta() {
		if (beta==null) {
			beta=configuration.getBoolean("/config/info/beta", false);
		}
		return beta;
	}

	public boolean isAlpha() {
		if (alpha==null) {
			alpha=configuration.getBoolean("/config/info/alpha", false);
		}
		return alpha;
	}

	public int getMaxPasswordLength() {
		if (maxPwdLen<0) {
			maxPwdLen = configuration.getInt("config/security/passwordLength/@max", 16);
		}
		return maxPwdLen;
	}

	public int getMinPasswordLength() {
		if (minPwdLen<0) {
			minPwdLen = configuration.getInt("config/security/passwordLength/@min", 0);
		}
		return minPwdLen;
	}

	/**
	 * Ritorna il path del folder in cui sono memorizzate le immagini temporanee accessibili via web, ad esempio per il preview della newsletter
	 * @return
	 */
	public File getTempImageDir() {
		if (tempFolder==null) {
			tempFolder = new File(getContentPath(), getTempImageRelativePath());
			if (!tempFolder.exists()) {
				tempFolder.mkdirs();
			}
		}
		return tempFolder;
	}

	/**
	 * Ritorna il path del folder in cui sono memorizzate le immagini temporanee accessibili via web, relativamente al folder "contents"
	 * @return
	 */
	public String getTempImageRelativePath() {
		return "/tmp";
	}

	/**
	 * Return the webapp address without a trailing slash. E.g. http://www.mysite.com/app or http://www.mysite.com
	 * The address is computed from the request when not null, else it is read from the configuration.
	 * @return
	 */
	public String getWebappAddress(HttpServletRequest request) {
		if (webappAddress==null) {
			if (request!=null) {
				StringBuilder address = new StringBuilder(getServerAddress(request)); // http://www.example.com
				address.append(request.getContextPath()); // http://www.example.com/appname
				// Adding the language is a bug because the value is cached
				//			if (isLocalePathVariableEnabled() && locale!=null) {
				//				address.append("/").append(locale.getLanguage()); // http://www.example.com/en
				//			}
				webappAddress = address.toString();
			} else {
				webappAddress = getWebappAddress();
			}
		}
		return webappAddress;
	}

	/**
	 * Return the webapp address without a trailing slash. E.g. http://www.mysite.com/app or http://www.mysite.com
	 * @return
	 */
	public String getWebappAddress() {
		if (webappAddress==null) {
			String contextPath = StringUtils.removeEnd(configuration.getString("config/paths/contextPath", ""), "/");
			webappAddress = getServerAddress() + "/" + contextPath;
			webappAddress = StringUtils.removeEnd(webappAddress, "/");
		}
		return webappAddress;
	}

	/**
	 * Return the server address without a trailing slash. E.g. http://col.letturedametropolitana.it
	 * Warning: this version does not work properly behind an ajp connector
	 * @return
	 * @deprecated use {@link #getServerAddress()} instead
	 */
	@Deprecated
	public String getServerAddress(HttpServletRequest request) {
		if (serverAddress==null) {
			StringBuilder address = new StringBuilder();
			address.append(request.getScheme()).append("://").append(request.getServerName()); // http://www.example.com
			if (request.getServerPort()!=80 && request.getServerPort()!=443) {
				address.append(":").append(request.getServerPort()); // http://www.example.com:8080
			}
			serverAddress = address.toString();
		}
		return serverAddress;
	}

	/**
	 * Return the server address without a trailing slash. E.g. http://col.letturedametropolitana.it
	 * @return
	 */
	public String getServerAddress() {
		if (serverAddress==null) {
			serverAddress = StringUtils.removeEnd(configuration.getString("config/paths/serverAddress", "serverAddressUnset"), "/");
		}
		return serverAddress;
	}

	public String getEmailLogoImage() {
		if (logoImage==null) {
			logoImage = configuration.getString("config/email/logoImage", ""); // e.g. /res/img/logo-small.jpg
		}
		return logoImage;
	}

	/**
	 * Returns ".min" if this is not a development environment. Use like <script yada:src="@{|/res/dataTables/jquery.dataTables${@config.min}.js|}"
	 * @return ".min" or ""
	 */
	public String getMin() {
		if (isDevelopmentEnvironment()) {
			return "";
		}
		return ".min";
	}

	/**
	 *
	 * @return
	 */
	public int getAutologinExpirationHours() {
		return configuration.getInt("config/security/autologinExpirationHours", 10);
	}

	/**
	 * Ritorna il numero massimo di suggerimenti di tag per una storia
	 * @return
	 */
	public int getTagFilterMax() {
		if (tagFilterMax==-1) {
			tagFilterMax = configuration.getInt("config/tags/filter/maxTags", 10);
		}
 		return tagFilterMax;
	}

	/**
	 * Ritorna il numero massimo di suggerimenti di tag per una storia
	 * @return
	 */
	public int getTagMaxSuggested() {
		if (tagMaxSuggested==-1) {
			tagMaxSuggested = configuration.getInt("config/tags/story/maxSuggested", -1);
		}
		return tagMaxSuggested;
	}

	/**
	 * Ritorna il numero massimo di tag normali che è possibile assegnare a una storia
	 * @return
	 */
	public int getTagMaxNum() {
		if (tagMaxNum==-1) {
			tagMaxNum = configuration.getInt("config/tags/story/maxNum", -1);
		}
		return tagMaxNum;
	}

	/**
	 * Ritorna il prefisso di un tag redazionale (special)
	 * @return
	 */
	public String getTagReservedPrefix() {
		if (tagReservedPrefix==null) {
			tagReservedPrefix = configuration.getString("config/tags/reserved/prefix", "@").toLowerCase();
		}
 		return tagReservedPrefix;
	}

	public String[] getSupportRequestRecipients() {
		return configuration.getStringArray("config/email/support/to");
	}

	public int getFacebookType() {
		if (facebookType==-1) {
			facebookType = configuration.getInt("config/social/facebook/type", -1);
		}
 		return facebookType;
	}

	public int getGoogleType() {
		if (googleType==-1) {
			googleType = configuration.getInt("config/social/google/type", -1);
		}
		return googleType;
	}

	/**
	 * Ritorna la url da usare per postare una story su facebook (a cui appendere l'id)
	 * @return
	 */
	public String getFacebookBaseStoryUrl() {
 		return configuration.getString("config/social/facebook/baseStoryUrl", "unset");
	}

	public String getFacebookTestPageAccessToken() {
		return configuration.getString("config/social/facebook/test/pageAccessToken", "unset");
	}

	public String getFacebookPageAccessToken() {
		return configuration.getString("config/social/facebook/pageAccessToken", "unset");
	}

	public String getFacebookTestSecret() {
		return configuration.getString("config/social/facebook/test/secret", "unset");
	}

	public String getFacebookSecret() {
		if (facebookSecret==null) {
			facebookSecret = configuration.getString("config/social/facebook/secret", "unset");
		}
		return facebookSecret;
	}

	public String getFacebookTestPageId() {
		return configuration.getString("config/social/facebook/test/pageId", "unset");
	}

	public String getFacebookPageId() {
		if (facebookPageId==null) {
			facebookPageId = configuration.getString("config/social/facebook/pageId", "unset");
		}
		return facebookPageId;
	}

	public String getFacebookTestAppId() {
		return configuration.getString("config/social/facebook/test/appId", "unset");
	}

	public String getFacebookAppId() {
		if (facebookAppId==null) {
			facebookAppId = configuration.getString("config/social/facebook/appId", "unset");
		}
		return facebookAppId;
	}

	/**
	 */
	public String getGoogleSecret() {
		if (googleSecret==null) {
			googleSecret = configuration.getString("config/social/google/secret", "unset");
		}
		return googleSecret;
	}

	/**
	 * @return
	 */
	public String getGoogleClientId() {
		if (googleClientId==null) {
			googleClientId = configuration.getString("config/social/google/clientId", "unset");
		}
		return googleClientId;
	}

	/**
	 * Given a role id, returns the configured role name prefixed by "ROLE_", e.g. "ROLE_USER"
	 * @param roleId e.g. 9
	 * @return e.g. "ROLE_ADMIN"
	 * @see #getRoleName(Integer)
	 */
	public String getRoleSpringName(Integer roleId) {
		String roleKey = getRoleKey(roleId);
		return "ROLE_" + roleKey;
	}
	
	/**
	 * Given a role id, returns the configured role name e.g. "USER".
	 * Equivalent to {@link #getRoleKey(Integer)}.
	 * @param roleId e.g. 9
	 * @return e.g. "ADMIN"
	 */
	public String getRoleName(Integer roleId) {
		return this.getRoleKey(roleId);
	}

	/**
	 *
	 * @return the configured role ids, sorted ascending
	 */
	public List<Integer> getRoleIds() {
		ensureRoleMaps();
		List<Integer> result = new ArrayList<>(roleIdToKeyMap.keySet());
		Collections.sort(result);
		return result;
	}

	/**
	 * Convert from role names to role ids
	 * @param roleNames an array of role names, like ["USER", "ADMIN"]
	 * @return the corresponding role ids
	 */
	public Integer[] getRoleIds(String[] roleNames) {
		ensureRoleMaps();
		Integer[] result = new Integer[roleNames.length];
		for (int i = 0; i < roleNames.length; i++) {
			Integer roleId = roleKeyToIdMap.get(roleNames[i]);
			if (roleId==null) {
				throw new YadaConfigurationException("Invalid role name " + roleNames[i]);
			}
			result[i]=roleId;
		}
		return result;
	}

	/**
	 * @param roleKey e.g. "ADMIN"
	 * @return e.g. 9
	 */
	public Integer getRoleId(String roleKey) {
		ensureRoleMaps();
		Integer id = roleKeyToIdMap.get(roleKey.toUpperCase());
		if (id==null) {
			throw new YadaInvalidValueException("Role " + roleKey + " not configured");
		}
		return id;
	}
	
	/**
	 * @param roleId e.g. 9
	 * @return e.g. "ADMIN"
	 */
	public String getRoleKey(Integer roleId) {
		ensureRoleMaps();
		String key = roleIdToKeyMap.get(roleId);
		if (key==null) {
			throw new YadaInvalidValueException("Role " + roleId + " not configured");
		}
		return key;
	}

	private void ensureRoleMaps() {
		synchronized (roleMapMonitor) {
			if (roleIdToKeyMap!=null) {
				return;
			}
			roleIdToKeyMap = new HashMap<>();
			roleKeyToIdMap = new HashMap<>();
			for (ImmutableHierarchicalConfiguration sub : configuration.immutableConfigurationsAt("config/security/roles/role")) {
				Integer id = sub.getInteger("id", null);
				String key = sub.getString("key", null);
				// Controllo che non ci sia duplicazione di id
				if (id==null || roleIdToKeyMap.get(id)!=null) {
					throw new YadaInternalException("Invalid role configuration in conf.webapp.xml");
				}
				// Controllo che non ci sia duplicazione di key
				if (key==null || roleKeyToIdMap.get(key)!=null) {
					throw new YadaInternalException("Invalid role configuration in conf.webapp.xml");
				}
				roleIdToKeyMap.put(id, key.toUpperCase());
				roleKeyToIdMap.put(key.toUpperCase(), id);
			}
			roleIdToKeyMap = Collections.unmodifiableMap(roleIdToKeyMap);
			roleKeyToIdMap = Collections.unmodifiableMap(roleKeyToIdMap);
		}
	}

	/**
	 * Ritorna la lista di YadaClause trovate nella configurazione
	 */
	public List<YadaClause> getSetupClauses() {
		List<YadaClause> result = new ArrayList<>();
		for (ImmutableHierarchicalConfiguration sub : configuration.immutableConfigurationsAt("config/setup/clauses")) {
			for (Iterator<String> names = sub.getKeys(); names.hasNext();) {
				String name = names.next();
				String content = sub.getString(name);
				YadaClause clause = new YadaClause();
				clause.setName(name);
				clause.setContent(content.trim().replaceAll("\\s+", " ")); // Collasso gli spazi multipli in spazio singolo
				clause.setClauseVersion(1);
				result.add(clause);
			}
		}
		return result;
	}

	/**
	 * Check if content urls must be local to the server or remote with a http address
	 * @return
	 */
	public boolean isContentUrlLocal() {
		String contentUrl = getContentUrl();
		return contentUrl!=null && contentUrl.charAt(0)=='/' && contentUrl.charAt(1)!='/';
	}


	/**
	 * Url where to download user-uploaded content, without final slash.
	 * Can be webapp-relative or absolute, for example "/contents" or "http://cdn.my.com/contents" or "//cdn.my.com/contents"
	 * @return the configured value, or "/contents" by default
	 */
	public String getContentUrl() {
		if (contentUrl==null) {
			contentUrl = configuration.getString("config/paths/contentDir/@url", "/contents");
			if (contentUrl.endsWith("/")) {
				contentUrl = StringUtils.chop(contentUrl); // Remove last character
			}
			if (!contentUrl.startsWith("http") && !contentUrl.startsWith("/")) {
				contentUrl = '/' + contentUrl;
			}
			if (contentUrl.length()==0 || contentUrl.equals("/")) {
				throw new YadaConfigurationException("The configured contentDir url is invalid: it should either be a full url or a folder-like value, as in '/xxx/yyy'");
			}
		}
		return contentUrl;
	}

	/**
	 * Base folder for uploaded content
	 * @return
	 */
	public File getContentsFolder() {
		return new File(getContentPath());
	}

	/**
	 * Path del filesystem in cui vengono memorizzati i "contenuti" caricati dall'utente, per esempio /srv/ldm/contents
	 * @return
	 */
	public String getContentPath() {
		return getBasePathString() + "/" + getContentName();
	}

	/**
	 * Name of the folder inside basePath where contents are stored
	 * @return
	 */
	public String getContentName() {
		if (contentName==null) {
			contentName = configuration.getString("config/paths/contentDir/@name");
			if (contentName==null) {
				log.error("contentDir name missing in configuration. Example: <paths><contentDir name=\"contents\" url=\"/contents\">");
			}
		}
		return contentName;
	}

	/**
	 * Absolute path on the filesystem where application files not belonging to the webapp war are stored.
	 * Example: /srv/myproject
	 * @return
	 */
	public String getBasePathString() {
		return configuration.getString("config/paths/basePath");
	}

	/**
	 * Absolute path on the filesystem where application files not belonging to the webapp war are stored.
	 * Example: /srv/myproject
	 * @return
	 */
	public Path getBasePath() {
		return new File(configuration.getString("config/paths/basePath")).toPath();
	}

	/**
	 *
	 * @return e.g. "res"
	 */
	public String getResourceDir() {
		return configuration.getString("config/paths/resourceDir", "res");
	}

	/**
	 *
	 * @return e.g. "yadares"
	 */
	public String getYadaResourceDir() {
		return configuration.getString("config/paths/yadaResourceDir", "yadares");
	}

	/**
	 *
	 * @return e.g. "res-0012"
	 */
	public String getVersionedResourceDir() {
		return getResourceDir() + "-" + getApplicationBuild();
	}

	/**
	 *
	 * @return e.g. "yadares-0012"
	 */
	public String getVersionedYadaResourceDir() {
		return getYadaResourceDir() + "-" + getYadaVersion();
	}

//	/**
//	 * Add project-specific user attributes from the configuration
//	 * @param user
//	 * @param sub
//	 */
//	@Deprecated // should not be needed anymore, now that any tag can be used to define a user
//	abstract protected void addSetupUserAttributes(Map<String, Object> user, ImmutableHierarchicalConfiguration sub);

	/**
	 * Every tag in the config/setup/users/user configuration is added to the map as a key-value pair
	 * except roles, added as a list to the "roles" key.
	 */
	public List<Map<String, Object>> getSetupUsers() {
		List<Map<String, Object>> result = new ArrayList<>();
		for (ImmutableHierarchicalConfiguration sub : configuration.immutableConfigurationsAt("config/setup/users/user")) {
			Map<String, Object> user = new HashMap<>();
			Set<Integer> roles = new HashSet<>();
			result.add(user);
			// Every tag except <role> is added to the map
			for (Iterator<String> keys = sub.getKeys(); keys.hasNext();) {
				String key = keys.next();
				if (!"role".equals(key)) {
					user.put(key, sub.getString(key));
				}
			}
//			user.put("name", sub.getString("name"));
//			user.put("surname", sub.getString("surname"));
//			user.put("email", sub.getString("email"));
//			user.put("password", sub.getString("password"));
			user.put("roles", roles);
//			addSetupUserAttributes(user, sub);
			List<String> configRoles = sub.getList(String.class, "role");
			for (String code : configRoles) { // 'ADMIN'
				Integer role = getRoleId(code);
				if (role==null) {
					throw new YadaInternalException("Invalid user role name (skipped): " + code);
				} else {
					roles.add(role);
				}
			}
		}
		return result;
	}

	public String getMaxFileUploadSizeMega() {
		return "" + getMaxFileUploadSizeBytes() / 1024 / 1024; // 50 mega default
	}

	public int getMaxFileUploadSizeBytes() {
		return configuration.getInt("config/maxFileUploadSizeBytes", 50000000); // 50 mega default
	}

	/**
	 * Used internally to configure JPA EntityManagerFactory
	 * @return
	 */
	List<String> getDbEntityPackages() {
		String key = "config/database/entityPackage";
		List<String> result = configuration.getList(String.class, key);
		if (result==null) {
			throw new YadaConfigurationException("{} missing in configuration (conf.webapp.xml)", key);
		}
		return result;
	}

	public String getDbJndiName() {
		return configuration.getString("config/database/jndiname");
	}

	/**
	 * Get the email address and the personal name of the sender.
	 * @return an array with email and personal name
	 */
	public String[] getEmailFrom() {
		String address = configuration.getString("config/email/from/address");
		String personal = configuration.getString("config/email/from/name");
		if (address!=null) {
			return new String[] {address, personal};
		} else {
			// Legacy
			return new String[] { configuration.getString("config/email/from"), null };
		}
	}

	public boolean isEmailEnabled() {
		return configuration.getBoolean("config/email/enabled", false);
	}

	public String getEmailHost() {
		try {
			String result = configuration.getString("/config/email/smtpserver/host");
			log.info("Mail Server Host = {}", result);
			return result;
		} catch (Exception e) {
			log.warn("No SMTP Server Host defined at /config/email/smtpserver/host - (ignored)");
			return null;
		}
	}

	public int getEmailPort() {
		try {
			int result = configuration.getInt("/config/email/smtpserver/port");
			log.info("Mail Server Port = {}", result);
			return result;
		} catch (Exception e) {
			log.warn("No SMTP Server Port defined at /config/email/smtpserver/port - (ignored)");
			return 0;
		}
	}

	public String getEmailProtocol() {
		try {
			String result = configuration.getString("/config/email/smtpserver/protocol");
			log.info("Mail Server Protocol = {}", result);
			return result;
		} catch (Exception e) {
			log.warn("No SMTP Server Protocol defined at /config/email/smtpserver/protocol - (ignored)");
			return null;
		}
	}

	public String getEmailUsername() {
		try {
			String result = configuration.getString("/config/email/smtpserver/username");
			log.info("Mail Server Username = {}", result);
			return result;
		} catch (Exception e) {
			log.warn("No SMTP Server Username defined at /config/email/smtpserver/username - (ignored)");
			return null;
		}
	}

	public String getEmailPassword() {
		try {
			String result = configuration.getString("/config/email/smtpserver/password");
			log.info("Mail Server Password = ******");
			return result;
		} catch (Exception e) {
			log.warn("No SMTP Server Password defined at /config/email/smtpserver/password - (ignored)");
			return null;
		}
	}

	public Properties getEmailProperties() {
		try {
			Properties result = configuration.getProperties("/config/email/smtpserver/properties");
			log.info("Mail Server Properties = {}", result);
			return result;
		} catch (Exception e) {
			log.warn("No SMTP Server Properties defined at /config/email/smtpserver/properties - (ignored)");
			return null;
		}
	}

	/**
	 * Ritorna le email per le quali è abilitato l'invio
	 * @return
	 */
	public List<String> getValidDestinationEmails() {
		return new ArrayList<>(Arrays.asList(configuration.getStringArray("config/email/validEmail")));
	}

	public boolean isProductionEnvironment() {
		if (production==null) {
			production="prod".equalsIgnoreCase(getApplicationEnvironment());
		}
		return production;
	}

	public boolean isDevelopmentEnvironment() {
		if (development==null) {
			development="dev".equalsIgnoreCase(getApplicationEnvironment());
		}
		return development;
	}

	/**
	 * Ritorna la stringa che rappresenta la versione di yada
	 * @return
	 */
	public String getYadaVersion(){
		if (yadaVersion==null) {
			yadaVersion = configuration.getString("net/yadaframework/yadaweb/version");
			if (yadaVersion==null || "@YADA_VERSION@".equals(yadaVersion)) {
				// While working in Eclipse, the version is not set because the build is not done by Gradle
				log.info("YADA VERSION NOT SET, using timestamp");
				yadaVersion="dev"+System.currentTimeMillis();
			}
		}
		return yadaVersion;
	}

	/**
	 * Ritorna la stringa che rappresenta la versione, completa di build
	 * @return
	 */
	public String getApplicationVersion(){
		if (version==null) {
			version=new StringBuffer(configuration.getString("config/info/version")).append("-").append(getApplicationBuild()).toString();
		}
		return version;
	}

	/**
	 * Get the configured environment name: config/info/env
	 * @return the environment name, or ""
	 */
	public String getApplicationEnvironment() {
		if (environment==null) {
			environment = configuration.getString("config/info/env", "");
		}
 		return environment;
	}

	/**
	 * @return il numero di build. Viene usato soprattutto per eliminare il problema della cache sui file dentro a res
	 */
	public String getApplicationBuild() {
		if (build==null) {
			boolean canOverride = configuration.getBoolean("config/info/build/@canOverride", true);
			if (canOverride && isDevelopmentEnvironment()) {
				// While developing you get a new build number each time you restart the server
				// so that you don't get stale (cached) files
				build = "dev" + System.currentTimeMillis();
				log.info("Using timestamp for application build: " + build);
			} else {
				build = configuration.getString("config/info/build", null);
				if (build==null) {
					build="unset";
					log.error("!!! BUILD NUMBER NOT SET !!! Check that .../main/webapp/WEB-INF/build.properties has been created");
				}
			}
		}
		return build;
	}

	/**
	 * @return la data di rilascio oppure null oppure "" se non è stata settata dal build.xml
	 */
	public String getApplicationDate() {
		if (releaseDate==null) {
			releaseDate=configuration.getString("release/date", null);
			if ("@@DATA_RILASCIO@@".equals(releaseDate)) { // Il pattern non è stato rimpiazzato con la data
				releaseDate=""; // Non metto a null altrimenti continua a loggare
				log.error("Data non settata durante il rilascio (ignored)");
			}
		}
		return releaseDate;
	}

//	/**
//	 * Ritorna il valore di "config.resources.basepath" come File
//	 * @param config
//	 * @return
//	 */
//	public File getResourcesBasepath() {
//		return new File(this.getString("config.resources.basepath", ""));
//	}
//
//	/**
//	 * Ritorna il valore di "config.resources.basepath" come URL, terminata con "/"
//	 * @param config
//	 * @return
//	 */
//	public String getResourcesBaseurl() {
//		return BabkaFileUtil.getNormalisedPath(this, "config.resources.baseurl", "/");
//	}
//

	/**
	 * Cerca all'interno delle options il testo toSearch
	 * @param options
	 * @param toSearch
	 * @return le properties filtrate
	 */
	public static Properties containsInProperties(Properties options, String toSearch){
		Properties returnList = new Properties();
		for(Object k: options.keySet()){
			String ks =  (String)k ;
			String value = options.getProperty(ks);
			if(value.toLowerCase().contains(toSearch.toLowerCase())){
				returnList.put(ks, value);
			}
		}
		return returnList;
	}

	public YadaConfiguration() {
	}

//	public YadaConfiguration(NodeCombiner comb) {
//		super(comb);
//	}
//
//	public YadaConfiguration(Lock lock) {
//		super(lock);
//	}
//
//	public YadaConfiguration(NodeCombiner comb, Lock lock) {
//		super(comb, lock);
//	}

	public boolean getShowSql() {
		return configuration.getBoolean("config/database/showSql", false);
	}

	public boolean encodePassword() {
		return configuration.getBoolean("config/security/encodePassword", false);
	}

	public int getMaxPasswordFailedAttempts() {
		return configuration.getInteger("config/security/maxFailedAttempts", 50);
	}

	public int getPasswordFailedAttemptsLockoutMinutes() {
		return configuration.getInteger("config/security/failedAttemptsLockoutMinutes", 50);
	}

	// Selenium Web Driver

	public int seleniumWaitQuick() {
		return configuration.getInt("config/selenium/timeout/waitQuickSeconds", 8);
	}
	public int seleniumWait() {
		return configuration.getInt("config/selenium/timeout/waitSeconds", 10);
	}
	public int seleniumWaitSlow() {
		return configuration.getInt("config/selenium/timeout/waitSlowSeconds", 18);
	}

	/**
	 * The url of the Selenium HUB or the ChromeDriver server. Example: http://localhost:4444/wd/hub, http://127.0.0.1:9515
	 * @return
	 */
	public String getSeleniumHubAddress() {
		return configuration.getString("config/selenium/hubAddress");
	}

	public long getSeleniumTimeoutSlowPageLoadSeconds() {
		return configuration.getInt("config/selenium/timeout/slowPageLoadSeconds", 60);
	}

	public long getSeleniumTimeoutProxyTestPageLoadSeconds() {
		return configuration.getInt("config/selenium/timeout/proxyTestPageLoadSeconds", 10);
	}

	public long getSeleniumTimeoutPageLoadSeconds() {
		return configuration.getInt("config/selenium/timeout/pageLoadSeconds", 10);
	}

	public long getSeleniumTimeoutScriptSeconds() {
		return configuration.getInt("config/selenium/timeout/scriptSeconds", 5);
	}

	public long getSeleniumTimeoutImplicitlyWaitSeconds() {
		return configuration.getInt("config/selenium/timeout/implicitlyWaitSeconds", 5);
	}

	//

	public ImmutableHierarchicalConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(ImmutableHierarchicalConfiguration configuration) {
		this.configuration = configuration;
	}

	public String getString(String key, String defaultValue) {
		return configuration.getString(key, defaultValue);
	}

	public String getString(String key) {
		return configuration.getString(key);
	}

	public int getInt(String key, int defaultValue) {
		return configuration.getInt(key, defaultValue);
	}

	public long getLong(String key, long defaultValue) {
		return configuration.getLong(key, defaultValue);
	}

	public void setBuilder(CombinedConfigurationBuilder builder) throws ConfigurationException {
		this.builder = builder;
		this.configuration = ConfigurationUtils.unmodifiableConfiguration(builder.getConfiguration());
	}

	/**
	 * Call this method to trigger a configuration reload, but only if the file has changed and the timeout since last reload has passed
	 * @throws ConfigurationException
	 */
	public void reloadIfNeeded() throws ConfigurationException {
		// TODO Doesn't seem to work
		if (builder instanceof ReloadingCombinedConfigurationBuilder) {
			((ReloadingCombinedConfigurationBuilder)builder).getReloadingController().checkForReloading(null);
			this.configuration = ConfigurationUtils.unmodifiableConfiguration(builder.getConfiguration());
		}
	}

	/**
	 * The YadaJobScheduler period in milliseconds. <1 means do not schedule
	 * @return
	 */
	public long getYadaJobSchedulerPeriod() {
		return this.configuration.getLong("config/yada/jobScheduler/periodMillis", 0); // by default it doesn't start
	}

	/**
	 * The YadaJobScheduler thread pool size. When the number of concurrent jobs is higher, they are queued.
	 * @return
	 */
	public int getYadaJobSchedulerThreadPoolSize() {
		return this.configuration.getInt("config/yada/jobScheduler/threadPoolSize", 10);
	}

	/**
	 * Milliseconds after a running job is considered to be stale and killed.
	 * @return
	 */
	public long getYadaJobSchedulerStaleMillis() {
		return this.configuration.getLong("config/yada/jobScheduler/jobStaleMillis", 1000*60);
	}

	/**
	 * Number of YadaJob entities to keep in cache. It should be equal to the estimated maximum number
	 * of concurrent running jobs.
	 * If the number is too small, concurrent writes to the same job data could result in "concurrent modification"
	 * exceptions. Also the running job could be evicted from cache and terminated prematurely with a log saying "Evicting job {} while still running".
	 * This is a possible scenario:
	 * <pre>
	 * - thread A loads instance J1 for a job, puts it in the cache and starts a long running job
	 * - after some time, while the job is still running, the J1 instance is evicted because of cache size limits
	 * - thread B asks for a job instance of the same job, which is not found and loaded new from db as J2. It then
	 * changes some values and saves J2 to database, incrementing @version
	 * - thread A terminates the long running job by writing J1 to the database
	 * In this scenario, thread A will receive an exception because its @version differs from what is in the database
	 * </pre>
	 * @return
	 */
	public int getYadaJobSchedulerCacheSize() {
		return this.configuration.getInt("config/yada/jobScheduler/jobCacheSize", 500);
	}

}
