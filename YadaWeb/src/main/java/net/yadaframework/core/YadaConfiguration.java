package net.yadaframework.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.configuration2.builder.combined.ReloadingCombinedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.exceptions.InternalException;
import net.yadaframework.exceptions.YadaConfigurationException;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.persistence.entity.YadaClause;

/**
 * Classe che estende CombinedConfiguration aggiungendo metodi di gestione della configurazione specifici.
 */
public abstract class YadaConfiguration {
	private static Logger log = LoggerFactory.getLogger(YadaConfiguration.class);
	
	protected ImmutableHierarchicalConfiguration configuration;
	protected ReloadingCombinedConfigurationBuilder builder;
	
	// Cached values
	// Questi valori li memorizzo perchè probabilmente verranno controllati 
	// ad ogni pageview e comunque non mi aspetto che cambino a runtime
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
	private String facebookAppId = null;
	private String facebookSecret = null;
	private String serverAddress = null;
	private String webappAddress = null;
	private int facebookType = -1;
	private String tagReservedPrefix = null;
	private int tagMaxNum = -1;
	private int tagMaxSuggested = -1;
	private int tagFilterMax = -1;
	private int maxPwdLen = -1;
	private int minPwdLen = -1;
	private String errorPageForward = null;
	private List<String> locales = null;
	private Map<String, String> languageToCountry = null;
	private Boolean localeAddCountry = null;
	private Boolean localePathVariableEnabled = null;
	private Locale defaultLocale = null;
	private boolean defaultLocaleChecked = false;
	
	
	/**
	 * @return the link (only a part of it) to use for the registration confirmation
	 */
	public String getRegistrationConfirmationLink() {
		return configuration.getString("config/security/registration/confirmationLink", "/registrationConfirmation");
	}

	/**
	 * Checks if an email address has been blacklisted in the configuration
	 * @param email
	 * @return true for a blacklisted email address
	 */
	public boolean emailBlacklisted(String email) {
		String[] patternStrings = configuration.getStringArray("config/email/blacklistPattern");
		for (String patternString : patternStrings) {
			if (email.matches(patternString)) {
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
	 * Returns the default locale when getting a string that doesn't have a value for the requested locale.
	 * If the default locale is not set, the string will have an empty value and no attempt on another locale will be made.
	 * @return the default locale, or null if no default is set
	 */
	public Locale getDefaultLocale() {
		if (!defaultLocaleChecked) {
			defaultLocaleChecked = true;
			String localeString = configuration.getString("config/i18n/locale[@default='true']", null);
			if (localeString!=null) {
				try {
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
	 * Get a list of iso2 locales that the webapp can handle
	 * @return
	 */
	public List<String> getLocaleStrings() {
		if (locales==null) {
			locales = Arrays.asList(configuration.getStringArray("config/i18n/locale"));
			for (String locale : locales) {
				try {
			        LocaleUtils.toLocale(locale); // Validity check
			    } catch (IllegalArgumentException e) {
			    	throw new YadaConfigurationException("Locale {} is invalid", locale);
			    }			
			}
		}
		return locales;
	}
	
	/**
	 * Returns the page to forward to after an unhandled exception or HTTP error
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
		return new File(getContentPath(), getTempImageRelativePath());
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

	/**
	 * Ritorna la url da usare per postare una story su facebook (a cui appendere l'id)
	 * @return
	 */
	public String getFacebookBaseStoryUrl() {
 		return configuration.getString("config/social/facebook/baseStoryUrl", "unset");
	}
	
//	/**
//	 * Ritorna il Facebook page ID
//	 * @return
//	 */
//	public String getFacebookPageId() {
//		return configuration.getString("config/social/facebook/pageId", "unset");
//	}
//	
	/**
	 * Ritorna il Facebook pageAccessToken
	 * @return
	 */
	public String getFacebookPageAccessToken() {
		return configuration.getString("config/social/facebook/pageAccessToken", "unset");
	}
	
	/**
	 * Ritorna il Facebook Secret
	 * @return
	 */
	public String getFacebookSecret() {
		if (facebookSecret==null) {
			facebookSecret = configuration.getString("config/social/facebook/secret", "unset");
		}
		return facebookSecret;
	}
	
	/**
	 * Ritorna la Facebook AppID
	 * @return
	 */
	public String getFacebookAppId() {
		if (facebookAppId==null) {
			facebookAppId = configuration.getString("config/social/facebook/appId", "unset");
		}
		return facebookAppId;
	}

	/**
	 * Ritorna il nome del ruolo come usato in spring security, ovvero ROLE_USER
	 * @param roleId
	 * @return
	 */
	public String getRoleSpringName(Integer roleId) {
		String roleKey = getRoleKey(roleId);
		return "ROLE_" + roleKey;
	}
	
	/**
	 * 
	 * @return the configured role ids, sorted ascending
	 */
	public List<Integer> getRoleIds() {
		ensureRoleMaps();
		List<Integer> result = new ArrayList<Integer>(roleIdToKeyMap.keySet());
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
			roleIdToKeyMap = new HashMap<Integer, String>();
			roleKeyToIdMap = new HashMap<String, Integer>();
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
		}
	}

	/**
	 * Ritorna la lista di YadaClause trovate nella configurazione
	 */
	public List<YadaClause> getSetupClauses() {
		List<YadaClause> result = new ArrayList<YadaClause>();
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
	 * Url da cui prelevare i "contenuti" caricati dall'utente, senza slash finale.
	 * Può essere relativa alla root della webapp oppure completa, per esempio "/contents" oppure "http://cdn.my.com/contents" o "//cdn.my.com/contents"
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
	 * Path del filesystem in cui vengono memorizzati i "contenuti" caricati dall'utente, per esempio /srv/ldm/contents
	 * @return
	 */
	public String getContentPath() {
		return getBasePath() + "/" + getContentName();
	}

	/**
	 * Name of the folder where uploaded contents are stored
	 * @return
	 */
	public String getContentName() {
		if (contentName==null) {
			contentName = configuration.getString("config/paths/contentDir/@name");
		}
		return contentName;
	}
	
	/**
	 * Path del filesystem che costituisce la base in cui vengono memorizzati i file dell'applicazione che non appartengono alla webapp,
	 * per esempio /srv/ldm
	 * @return
	 */
	protected String getBasePath() {
		return configuration.getString("config/paths/basePath");
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
	
	/**
	 * Add project-specific user attributes from the configuration 
	 * @param user
	 * @param sub
	 */
	@Deprecated // should not be needed anymore, now that any tag can be used to define a user
	abstract protected void addSetupUserAttributes(Map<String, Object> user, ImmutableHierarchicalConfiguration sub);
	
	public List<Map<String, Object>> getSetupUsers() {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (ImmutableHierarchicalConfiguration sub : configuration.immutableConfigurationsAt("config/setup/users/user")) {
			Map<String, Object> user = new HashMap<String, Object>();
			Set<Integer> roles = new HashSet<Integer>();
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
			addSetupUserAttributes(user, sub);
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

	public int getMaxFileUploadSizeBytes() {
		return configuration.getInt("config/maxFileUploadSizeBytes", 5000000); // 5 giga default
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
	
	public String getEmailFrom() {
		return configuration.getString("config/email/from");
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
		return new ArrayList<String>(Arrays.asList(configuration.getStringArray("config/email/validEmail")));
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
			if (isDevelopmentEnvironment()) {
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
		return configuration.getInt("config/selenium/timeout/waitQuickSeconds", 4);
	}
	public int seleniumWait() {
		return configuration.getInt("config/selenium/timeout/waitSeconds", 8);
	}
	public int seleniumWaitSlow() {
		return configuration.getInt("config/selenium/timeout/waitSlowSeconds", 16);
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

	public void setBuilder(ReloadingCombinedConfigurationBuilder builder) throws ConfigurationException {
		this.builder = builder;
		this.configuration = ConfigurationUtils.unmodifiableConfiguration(builder.getConfiguration());
	}
	
	/**
	 * Call this method to trigger a configuration reload, but only if the file has changed and the timeout since last reload has passed
	 * @throws ConfigurationException 
	 */
	public void reloadIfNeeded() throws ConfigurationException {
		// TODO Doesn't seem to work
		builder.getReloadingController().checkForReloading(null);
		this.configuration = ConfigurationUtils.unmodifiableConfiguration(builder.getConfiguration());
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
	
}
