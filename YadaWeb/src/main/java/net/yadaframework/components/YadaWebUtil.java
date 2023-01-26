package net.yadaframework.components;

import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_AUTOCLOSE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_BODY;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_CALLSCRIPT;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_REDIRECT;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_RELOADONCLOSE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_SEVERITY;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_TITLE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_TOTALSEVERITY;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_ERROR;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_INFO;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_OK;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaConstants;
import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.YadaPageRequest;
import net.yadaframework.web.YadaPageRows;

@Lazy // Lazy because used in YadaCmsConfiguration, and it woud give a circular refecence exception otherwise
@Service
public class YadaWebUtil {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaConfiguration config;
	@Autowired private YadaUtil yadaUtil;
	@Autowired private MessageSource messageSource;

	public final YadaPageRequest FIND_ONE = YadaPageRequest.of(0, 1);

	// Characters that should never be found or placed in a slug
	private static final String PATTERN_INVALID_SLUG = "[?%:,;=&!+~()@*$'\"\\s]";

	private Map<String, List<?>> sortedLocalEnumCache = new HashMap<>();


	public boolean isEmpty(YadaPageRows<?> yadaPageRows) {
		return yadaPageRows==null || yadaPageRows.isEmpty();
	}

	/**
	 * Returns the last part of the current request, for example from "/some/product" returns "product"
	 * @param request
	 * @return
	 */
	public String getRequestMapping(HttpServletRequest request) {
		String path = request.getServletPath();
		String[] parts = path.split("/");
		if (parts.length==0) {
			return "/";
		}
		return parts[parts.length-1];
	}

	/**
	 * If the url is a full url that points to our server, make it relative to the server and strip any language in the path.
	 * The result can be used in thymeleaf @{url} statements and will get the proper browser language when needed.
	 * @param fullUrlWithHttp something like "https://my.site.com/en/something/here", can be empty or null
	 * @param request
	 * @return someting like "/something/here", or null
	 */
	public String removeLanguageFromOurUrl(String fullUrlWithHttp, HttpServletRequest request) {
		String url = StringUtils.trimToNull(fullUrlWithHttp); // "https://my.site.com/en/something/here"
		if (url==null) {
			return null;
		}
		String ourAddress = this.getWebappAddress(request); // "https://my.site.com"
		if (url.startsWith(ourAddress)) {
			url = url.substring(ourAddress.length()); // "/en/something/here"
			if (config.isLocalePathVariableEnabled() && url.length()>=3) {
				// If the url starts with a configured language, strip it
				List<String> localeStrings = config.getLocaleStrings();
				for (String language : localeStrings) {
					if (url.startsWith("/" + language + "/")) {
						url = url.substring(language.length()+1); // "/something/here"
						break;
					}
				}
			}
		}
		return url;
	}

	/**
	 * Adds all request parameters to the Model, optionally filtering by name.
	 * Existing model attributes are not overwritten.
	 * @param model
	 * @param request
	 * @param nameFilter parameter names that should pass through to the Model
	 */
	public void passThrough(Model model, HttpServletRequest request, String ... nameFilter) {
		Map<String, String[]> parameterMap = request.getParameterMap();
		Set<String> nameFilterSet = new HashSet<>();
		nameFilterSet.addAll(Arrays.asList(nameFilter));

		for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
			String key = param.getKey();
			if (nameFilterSet.isEmpty() || nameFilterSet.contains(key)) {
				String[] valueArray = param.getValue();
				Object value = valueArray.length==1?valueArray[0]:valueArray;
				if (value!=null && !model.containsAttribute(key)) {
					model.addAttribute(key, value);
				}
			}
		}
	}

	/**
	 * Add a url parameter or change its value if present
	 * @param sourceUrl a full or relative url. Can also be just the query string starting with "?"
	 * @param paramName the name of the parameter, not urlencoded
	 * @param paramValue the value of the parameter, not urlencoded. Can be null to only have the paramName in the url
	 * @return
	 */
	// Not tested yet
	public String addOrUpdateUrlParameter(String sourceUrl, String paramName, String paramValue) {
		String encodedParamName = urlEncode(paramName);
		String encodedParamValue = urlEncode(paramValue); // Can be null
		String equalsAndValue = encodedParamValue==null? "" : "=" + encodedParamValue;
		StringBuilder result = new StringBuilder();
		int queryPos = sourceUrl.indexOf("?");
		boolean found=false;
		if (queryPos<0) {
			// There is no query string yet
			result.append(sourceUrl);
		} else if (queryPos>0) {
			// There is a query string already
			result.append(sourceUrl.substring(0, queryPos));
		}
		result.append("?");
		if (queryPos>-1) {
			// Check existing parameters
			String query = sourceUrl.substring(queryPos); // "?xxx=yyy&zzz"
			String[] params = query.split("[?&]"); // ["xxx=yyy", "zzz"]
			for (int i = 0; i < params.length; i++) {
				String[] parts = params[i].split("=");
				String name = parts[0];
				if (name.equals(encodedParamName)) {
					result.append(encodedParamName).append(equalsAndValue);
					found = true;
				} else {
					result.append(params[i]);
				}
				if (i<params.length) {
					result.append("&");
				}
			}
		}
		if (!found) {
			result.append(encodedParamName).append(equalsAndValue);
		}
		return result.toString();
	}

	/**
	 * Returns a full url, including the server address and any optional request parameters.
	 * @param relativeUrl a server-relative url without language component
	 * @param locale
	 * @param params optional request parameters to be set on the url, in the form of comma-separated name,value pairs. E.g. "id","123","name","joe".
	 * 			Existing parameters are not replaced. Null values become empty strings. Null names are skipped with their values.
	 * @return a full url like https://myapp.com/en/relative/url
	 */
	public String getFullUrl(String relativeUrl, Locale locale, String...params) {
		String webappAddress = config.getWebappAddress();
		String urlWithLocale = enhanceUrl(relativeUrl, locale, params);
		return makeUrl(webappAddress, urlWithLocale);
	}

	/**
	 * Returns true if we are in a forward that should display an error handled by YadaController.yadaError() or YadaGlobalExceptionHandler
	 * @param request
	 * @return
	 */
	public boolean isErrorPage(HttpServletRequest request) {
		return request.getAttribute(YadaConstants.REQUEST_HASERROR_FLAG)!=null;
	}

	/**
	 * Copies the content of a file to the Response then deletes the file.
	 * To be used when the client needs to download a previously-created temporary file that you don't want to keep on server.
	 * Remember to set the "produces" attribute on the @RequestMapping with the appropriate content-type.
	 * 
	 * This version is different from {@link #downloadFile(Path, boolean, String, String, HttpServletResponse)} because
	 * it can be used when you don't have the source temp File but only its name, for example because the temp file
	 * has been created in a previous request that redirected to the download url sending just the name of the temp file.
	 * @param tempFilename the name (without path) of the existing temporary file, created with Files.createTempFile()
	 * @param contentType the content-type header
	 * @param clientFilename the filename that will be used on the client browser
	 * @param response the HTTP response
	 * @return true if the file was sent without errors
	 */
	public boolean downloadTempFile(String tempFilename, String contentType, String clientFilename, HttpServletResponse response) {
		Path tempFile = null;
		try {
			Path dummy = Files.createTempFile("x", null);
			Path tempFolder = dummy.getParent();
			yadaUtil.deleteFileSilently(dummy);
			tempFile = tempFolder.resolve(tempFilename);
			if (!Files.exists(tempFile)) {
				log.info("Trying to download non-existent file {}", tempFile);
				return false;
			}
		} catch (IOException e) {
			log.error("Error sending temporary file to client", e);
			return false;
		}
		return downloadFile(tempFile, true, contentType, clientFilename, response);
	}

	/**
	 * Copies the content of a file to the Response then optionally deletes the file.
	 * To be used when the client needs to download a previously-created file that you don't want to keep on server.
	 * No need to set the "produces" attribute on the @RequestMapping with the appropriate content-type.
	 * @param fileToDownload the existing file to download and delete
	 * @param thenDeleteFile true to delete the file when download ends (or fails)
	 * @param contentType the content-type header
	 * @param clientFilename the filename that will be used on the client browser
	 * @param response the HTTP response
	 * @return true if the file was sent without errors
	 */
	public boolean downloadFile(Path fileToDownload, boolean thenDeleteFile, String contentType, String clientFilename, HttpServletResponse response) {
		try (InputStream resultStream = Files.newInputStream(fileToDownload)) {
			response.setContentType(contentType);
			response.setHeader("Content-Disposition", "attachment; filename=" + clientFilename);
			StreamUtils.copy(resultStream, response.getOutputStream()); // StreamUtils doesn't close any stream
			return true;
		} catch (IOException e) {
			log.error("Can't send temporary file to client", e);
		} finally {
			if (thenDeleteFile) {
				boolean deleted = yadaUtil.deleteFileSilently(fileToDownload);
				if (!deleted) {
					log.error("Temporary file could not be deleted: {}", fileToDownload);
				}
			}
		}
		return false;
	}

	/**
	 * Create a redirect string to be returned by a @Controller, taking into account the locale in the path.
	 * If you can do a redirect with a relative url ("some/url") you don't need to use this method because the language path
	 * won't be overwritten. Otherwise if you need to use an absolute url ("/some/url") then this method inserts the appropriate language path
	 * in the url (and any parameters at the end too).
	 * @param targetUrl the redirect target, like "/some/place"
	 * @param locale can be null if the locale is not in the path, but then why use this method?
	 * @param params optional request parameters to be set on the url, in the form of comma-separated name,value pairs. E.g. id,123,name,"joe"
	 * 			Existing parameters are not replaced. Null values become empty strings. Null names are skipped with their values.
	 * @return a url like "redirect:/en/some/place?par1=val1&par2=val2"
	 * @throws YadaInvalidUsageException if path locale is configured and the url is absolute and the locale is null
	 */
	public String redirectString(String url, Locale locale, String...params) {
		if (config.isLocalePathVariableEnabled() && url.startsWith("/") && locale==null) {
			throw new YadaInvalidUsageException("Locale is needed when using the locale path variable with an absolute redirect");
		}
		String enhancedUrl = enhanceUrl(url, locale, params);
		return "redirect:" + enhancedUrl;
	}

	/**
	 * Creates a new URL string from a starting one, taking into account the locale in the path and optional url parameters.
	 * @param url the original url, like "/some/place"
	 * @param locale added as a path in the url, only if the url is absolute. Can be null if the locale is not needed in the path
	 * @param params optional request parameters to be set on the url, in the form of comma-separated name,value pairs. E.g. "id","123","name","joe".
	 * 			Existing parameters are not replaced. Null values become empty strings. Null names are skipped with their values.
	 * @return a url like "/en/some/place?id=123&name=joe"
	 */
	public String enhanceUrl(String url, Locale locale, String...params) {
		StringBuilder result = new StringBuilder();
		if (config.isLocalePathVariableEnabled()) {
			// The language is added only to absolute urls, if it doesn't exist yet
			if (url.startsWith("/") && locale!=null) {
				String language = locale.getLanguage();
				if (!url.startsWith("/"+language+"/")) {
					result.append("/").append(language);
				}
			}
		}
		result.append(url);
		if (params!=null && params.length>0) {
			boolean isStart = true;
			int questionPos = result.indexOf("?");
			if (questionPos<0) {
				result.append("?");
			} else {
				if (url.length()>questionPos+1) {
					// There is some parameter already
					isStart = false;
				}
			}
			boolean isName = true;
			String lastName = null;
			for (String param : params) {
				if (isName && !isStart) {
					result.append("&");
				}
				if (isName) { // name
					// null names are skipped together with their value
					if (param!=null) {
						result.append(param);
						result.append("=");
					}
					lastName = param;
				} else { // value
					if (lastName!=null) {
						// Null values remain empty
						if (param!=null) {
							result.append(param);
						}
					} else {
						log.debug("Skipping null name and its value '{}'", param);
					}
				}
				isStart=false;
				isName=!isName;
			}
		}
		return result.toString();
	}

	/**
	 * Make a zip file and send it to the client. The temp file is automatically deleted.
	 * @param returnedFilename the name of the file to create and send, with extension. E.g.: data.zip
	 * @param sourceFiles the files to zip
	 * @param filenamesNoExtension the name of each file in the zip - null to keep the original names
	 * @param ignoreErrors true to ignore errors when adding a file and keep going, false for an exception when a file can't be zipped
	 * @param response
	 */
	public void downloadZip(String returnedFilename, File[] sourceFiles, String[] filenamesNoExtension, boolean ignoreErrors, HttpServletResponse response) throws Exception {
		File zipFile = null;
		try {
			zipFile = File.createTempFile("downloadZip", null);
			yadaUtil.createZipFile(zipFile, sourceFiles, filenamesNoExtension, ignoreErrors);
			response.setContentType("application/zip");
			response.setHeader("Content-disposition", "attachment; filename=" + returnedFilename);
			try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(zipFile)) {
				IOUtils.copy(in,out);
			}
		} finally {
			if (zipFile!=null) {
				zipFile.delete();
			}
		}

	}

	/**
	 * Sorts a localized enum according to the locale specified
	 * @param localEnum the class of the enum, e.g. net.yadaframework.persistence.entity.YadaJobState.class
	 * @param locale
	 * @return a list of sorted enums of the given class
	 */
	public <T extends YadaLocalEnum<?>> List<T> sortLocalEnum(Class<T> localEnum, Locale locale) {
		String key = localEnum.getName() + "." + locale.toString();
		@SuppressWarnings("unchecked")
		List<T> result = (List<T>) sortedLocalEnumCache.get(key);
		if (result==null) {
			T[] enums = localEnum.getEnumConstants();
			Arrays.sort(enums, new Comparator<T>() {
				@Override
				public int compare(T o1, T o2) {
					String v1 = o1.toString(messageSource, locale);
					String v2 = o2.toString(messageSource, locale);
					return v1.compareTo(v2);
				}
			});
			result = Arrays.asList(enums);
			sortedLocalEnumCache.put(key, result);
		}
		return result;
	}

	/**
	 * Returns the first language in the request language header as a string.
	 * @return the language string, like "en_US", or "" if not found
	 */
	public String getBrowserLanguage() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String languageHeader = StringUtils.trimToEmpty(request.getHeader("Accept-Language")); // en-US,en-GB;q=0.9,en;q=0.8,it;q=0.7,es;q=0.6,la;q=0.5
		int pos = languageHeader.indexOf(',');
		if (pos>4) {
			try {
				return languageHeader.substring(0, pos);
			} catch (Exception e) {
				// Invalid header - ignored
			}
		}
		return "";
	}

	/**
	 * Returns the first country in the request language header as a string.
	 * @return the country string, like "US", or "" if not found
	 */
	public String getBrowserCountry() {
		String browserLanguage = getBrowserLanguage();
		int pos = browserLanguage.indexOf('-');
		if (pos>1) {
			try {
				return browserLanguage.substring(pos+1);
			} catch (Exception e) {
				// Invalid header - ignored
			}
		}
		return "";
	}

	/**
	 * Save an uploaded file to a temporary file
	 * @param attachment
	 * @return the temporary file holding the uploaded file, or null if no file has bee attached
	 * @throws IOException, IllegalStateException
	 */
	public File saveAttachment(MultipartFile attachment) throws IOException {
		if (!attachment.isEmpty()) {
			File targetFile = File.createTempFile("upload-", null);
			saveAttachment(attachment, targetFile);
			return targetFile;
		}
		return null;
	}

	/**
	 * Save an uploaded file to the given target file
	 * @param attachment
	 * @param targetPath
	 * @throws IOException, IllegalStateException
	 */
	public void saveAttachment(MultipartFile attachment, Path targetPath) throws IOException {
		saveAttachment(attachment, targetPath.toFile());
		//		try (InputStream inputStream = attachment.getInputStream(); OutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
		//			IOUtils.copy(inputStream, outputStream);
		//		}
	}

	/**
	 * Save an uploaded file to the given target file
	 * @param attachment
	 * @param targetFile
	 * @throws IOException, IllegalStateException
	 */
	public void saveAttachment(MultipartFile attachment, File targetFile) throws IOException {
		attachment.transferTo(targetFile);
		//
		//		try (InputStream inputStream = attachment.getInputStream(); OutputStream outputStream = new FileOutputStream(targetFile)) {
		//			IOUtils.copy(inputStream, outputStream);
		//		}
	}

	/**
	 * Fix a url so that it valid and doesn't allow XSS attacks
	 * @see https://cheatsheetseries.owasp.org/cheatsheets/XSS_Filter_Evasion_Cheat_Sheet.html
	 * @param url some text typed by the user
	 * @return the same url or null if blank or a fixed one that may or may not work as expected, but won't pose a security risk
	 */
	public String sanitizeUrl(String url) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		url = url.replaceAll("[^-A-Za-z0-9+&@#/%?=~_|!:,.;\\(\\)]", "");
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "http://" + url;
		}
		return url;
	}

	/**
	 * Assembles a url given its parts as string.
	 * @param segments the initial parts of the url up to the query string. Leading and trailing slashes are added when missing.
	 * URLEncoding is not performed.
	 * @return
	 * @see #makeUrl(String[], Map, Boolean)
	 */
	public String makeUrl(String...segments) {
		return makeUrl(segments, null, null);
	}

	/**
	 * Assembles a url given its parts as string.
	 * @param segments the initial parts of the url up to the query string. Leading and trailing slashes are added when missing.
	 * @param requestParams optional name/value pairs of request parameters that will compose the query string.
	 * Use null for no parameters, use a null value for no value (just the name will be added)
	 * @param urlEncode use Boolean.TRUE to encode the parameters, null or anything else not to encode
	 * @return
	 */
	public String makeUrl(String[] segments, Map<String, String> requestParams, Boolean urlEncode) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			result.append(segments[i]);
			// Add a separator when needed
			if (i<segments.length-1 && !segments[i].endsWith("/") && !segments[i+1].startsWith("/")) {
				result.append("/");
			}
		}
		if (requestParams!=null) {
			boolean encode = Boolean.TRUE == urlEncode;
			result.append("?");
			String[] keys = requestParams.keySet().toArray(new String[0]);
			for (int i=0; i<keys.length; i++) {
				String key = keys[i];
				String value = requestParams.get(key);
				result.append(encode?urlEncode(key):key);
				if (value!=null) {
					result.append("=").append(encode?urlEncode(value):value);
				}
				if (i<keys.length-1) {
					result.append("&");
				}
			}
		}
		return result.toString();
	}

	/**
	 * From a given string, creates a "slug" that can be inserted in a url and still be readable.
	 * @param source the string to convert
	 * @return the slug
	 */
	public String makeSlug(String source) {
		return makeSlugStatic(source);
	}

	/**
	 * From a given string, creates a "slug" that can be inserted in a url and still be readable.
	 * It is static so that it can be used in Entity objects (no context)
	 * @param source the string to convert
	 * @return the slug, which is empty for a null string
	 */
	public static String makeSlugStatic(String source) {
		if (StringUtils.isBlank(source)) {
			return "";
		}
		String slug = removeHtmlStatic(source); //rimuove tutti gli eventuali tag.
		slug = source.trim().toLowerCase().replace('à', 'a').replace('è', 'e').replace('é', 'e')
			.replace('ì', 'i').replace('ò', 'o').replace('ù', 'u').replace('.', '-').replace(',', '-');
		slug = slug.replaceAll(" +", "-"); // Spaces become dashes
		slug = slug.replaceAll(PATTERN_INVALID_SLUG, "");
		slug = StringUtils.removeEnd(slug, ".");
		slug = StringUtils.removeEnd(slug, ";");
		slug = StringUtils.removeEnd(slug, "\\");
		slug = slug.replaceAll("/", "_");
		slug = slug.replaceAll("__", "_");
		slug = slug.replaceAll("--", "-");
		slug = slug.replaceAll("-+", "-"); // Multiple dashes become one dash
		return slug;
	}

	/**
	 * Find invalid characters from a slug: white space, :,;=&!+~\()@*$'
	 * The char "-" is excluded.
	 * @param text
	 * @return
	 */
	public boolean checkInvalidSlugCharacters(String text) {
		Pattern pattern = Pattern.compile(PATTERN_INVALID_SLUG);
		Matcher matcher = pattern.matcher(text);

		while(matcher.find()){
			return true;
		}
		return false;
	}

	/**
	 * Decodes a string with URLDecoder, handling the useless try-catch that is needed
	 * @param source
	 * @return
	 */
	public String urlDecode(String source) {
		final String encoding = "UTF-8";
		try {
			return URLDecoder.decode(source, encoding);
		} catch (UnsupportedEncodingException e) {
			log.error("Invalid encoding: {}", encoding);
		}
		return source;
	}

	/**
	 * Encodes a string with URLEncoder, handling the useless try-catch that is needed
	 * @param source
	 * @return the encoded source or null if the source is null
	 */
	public String urlEncode(String source) {
		if (source==null) {
			return null;
		}
		final String encoding = "UTF-8";
		try {
			return URLEncoder.encode(source, encoding);
		} catch (UnsupportedEncodingException e) {
			log.error("Invalid encoding: {}", encoding);
		}
		return source;
	}

	/**
	 * ATTENZIONE: non sempre va!
	 * @return
	 */
	public HttpServletRequest getCurrentRequest() {
		return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
	}

	/**
	 * Ritorna true se la request corrente è ajax
	 * @return
	 */
	public boolean isAjaxRequest() {
		return isAjaxRequest(getCurrentRequest());
	}

	/**
	 * Ritorna true se la request passata è ajax
	 * @return
	 */
	public boolean isAjaxRequest(HttpServletRequest request) {
		String ajaxHeader = request.getHeader("X-Requested-With");
		return "XMLHttpRequest".equalsIgnoreCase(ajaxHeader);
	}

	/**
	 * Trasforma /res/img/favicon.ico in /res-0002/img/favicon.ico
	 * @param urlNotVersioned
	 * @return
	 */
	public String versionifyResourceUrl(String urlNotVersioned) {
		// Esempio: var urlPrefix=[[@{${@yadaWebUtil.versionifyResourceUrl('/res/js/facebook/sdk.js')}}]];

		final String resFolder="/" + config.getResourceDir() + "/"; // "/res/"
		final int resLen = resFolder.length();
		if (urlNotVersioned.startsWith(resFolder)) {
			try {
				String prefix = urlNotVersioned.substring(0, resLen-1);
				String suffix = urlNotVersioned.substring(resLen-1);
				return prefix + "-" + config.getApplicationBuild() + suffix;
			} catch (Exception e) {
				log.error("Impossibile versionificare la url {} (ignored)", urlNotVersioned, e);
			}
		}
    	return urlNotVersioned;
    }

	/**
	 * Removes all HTML tags. Static to be used from Entity beans without forcing autowiring on each instance.
	 * @param source html content
	 * @return text content
	 * @see #cleanContent(String, String...)
	 */
	public static String removeHtmlStatic(String source) {
		Whitelist allowedTags = Whitelist.none();
		Document dirty = Jsoup.parseBodyFragment(source, "");
		Cleaner cleaner = new Cleaner(allowedTags);
		Document clean = cleaner.clean(dirty);
		clean.outputSettings().escapeMode(EscapeMode.xhtml); // Non fa l'escape dei caratteri utf-8
		String safe = clean.body().html();
		return safe;
	}

	/**
	 * Cleans the html content leaving only the following tags: b, em, i, strong, u, br, cite, em, i, p, strong, img, li, ul, ol, sup, sub, s
	 * @param content html content
	 * @param extraTags any other tags that you may want to keep, e. g. "a"
	 * @return
	 */
	public String cleanContent(String content, String ... extraTags) {
		Whitelist allowedTags = Whitelist.simpleText(); // This whitelist allows only simple text formatting: b, em, i, strong, u. All other HTML (tags and attributes) will be removed.
		allowedTags.addTags("br", "cite", "em", "i", "p", "strong", "img", "li", "ul", "ol", "sup", "sub", "s");
		allowedTags.addTags(extraTags);
		allowedTags.addAttributes("p", "style"); // Serve per l'allineamento a destra e sinistra
		allowedTags.addAttributes("img", "src", "style", "class");
		if (Arrays.asList(extraTags).contains("a")) {
			allowedTags.addAttributes("a", "href", "target");
		}
		Document dirty = Jsoup.parseBodyFragment(content, "");
		Cleaner cleaner = new Cleaner(allowedTags);
		Document clean = cleaner.clean(dirty);
		clean.outputSettings().escapeMode(EscapeMode.xhtml); // Non fa l'escape dei caratteri utf-8
		String safe = clean.body().html();
		return safe;
	}

	/**
	 * Ritorna l'indirizzo completo della webapp, tipo http://www.yadaframework.net:8080/site, senza slash finale
	 * Da thymeleaf si usa con ${@yadaWebUtil.getWebappAddress(#httpServletRequest)}
	 * @param request
	 * @return
	 * @deprecated use YadaConfiguration.getWebappAddress instead, because this version does not work behind an ajp connector
	 */
	@Deprecated
	public String getWebappAddress(HttpServletRequest request) {
		int port = request.getServerPort();
		String pattern = port==80||port==443?"%s://%s%s":"%s://%s:%d%s";
		String myServerAddress = port==80||port==443?
				String.format(pattern, request.getScheme(),  request.getServerName(), request.getContextPath())
				:
				String.format(pattern, request.getScheme(),  request.getServerName(), request.getServerPort(), request.getContextPath());
		return myServerAddress;
	}

	/**
	 * Indica se l'estensione appartiene a un'immagine accettabile
	 * @param extension e.g. "jpg"
	 * @return
	 */
	public boolean isWebImage(String extension) {
		extension = extension.toLowerCase();
		return extension.equals("jpg")
			|| extension.equals("png")
			|| extension.equals("gif")
			|| extension.equals("tif")
			|| extension.equals("tiff")
			|| extension.equals("jpeg");
	}

	/**
	 * ritorna ad esempio "jpg" lowercase
	 * @param multipartFile
	 * @return
	 */
	public String getFileExtension(MultipartFile multipartFile) {
		String result = null;
		if (multipartFile!=null) {
			String originalName = multipartFile.getOriginalFilename();
			result = yadaUtil.getFileExtension(originalName);
		}
		return result;
	}

	/**
	 * Visualizza un errore se non viene fatto redirect
	 * @param title
	 * @param message
	 * @param model
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public String modalError(String title, String message, Model model) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_ERROR, null, model);
		return "/yada/modalNotify";
	}

	/**
	 *
	 * @param title
	 * @param message
	 * @param model
	 * @return
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public String modalInfo(String title, String message, Model model) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_INFO, null, model);
		return "/yada/modalNotify";
	}

	/**
	 *
	 * @param title
	 * @param message
	 * @param model
	 * @return
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public String modalOk(String title, String message, Model model) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_OK, null, model);
		return "/yada/modalNotify";
	}

	/**
	 * Visualizza un errore se viene fatto redirect
	 * @param title
	 * @param message
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void modalError(String title, String message, RedirectAttributes redirectAttributes) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_ERROR, redirectAttributes);
	}

	/**
	 *
	 * @param title
	 * @param message
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void modalInfo(String title, String message, RedirectAttributes redirectAttributes) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_INFO, redirectAttributes);
	}

	/**
	 *
	 * @param title
	 * @param message
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void modalOk(String title, String message, RedirectAttributes redirectAttributes) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_OK, redirectAttributes);
	}

	/**
	 * Set the automatic closing time for the notification - no close button is shown
	 * @param milliseconds
	 * @param model
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void modalAutoclose(long milliseconds, Model model) {
		model.addAttribute(KEY_NOTIFICATION_AUTOCLOSE, milliseconds);
	}

	/**
	 * Set the automatic closing time for the notification - no close button is shown
	 * @param milliseconds
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void modalAutoclose(long milliseconds, RedirectAttributes redirectAttributes) {
		redirectAttributes.addAttribute(KEY_NOTIFICATION_AUTOCLOSE, milliseconds);
	}

	/**
	 * Set the page to reload when the modal is closed
	 * @param model
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void modalReloadOnClose(Model model) {
		model.addAttribute(KEY_NOTIFICATION_RELOADONCLOSE, KEY_NOTIFICATION_RELOADONCLOSE);
	}

	/**
	 * Set the page to reload when the modal is closed
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void modalReloadOnClose(RedirectAttributes redirectAttributes) {
		redirectAttributes.addAttribute(KEY_NOTIFICATION_RELOADONCLOSE, KEY_NOTIFICATION_RELOADONCLOSE);
	}

	@Deprecated
	private void notifyModal(String title, String message, String severity, RedirectAttributes redirectAttributes) {
		Map<String, ?> modelMap = redirectAttributes.getFlashAttributes();
		// Mette nel flash tre array di stringhe che contengono titolo, messaggio e severity.
		if (!modelMap.containsKey(KEY_NOTIFICATION_TITLE)) {
			List<String> titles = new ArrayList<>();
			List<String> bodies = new ArrayList<>();
			List<String> severities = new ArrayList<>();
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_TITLE, titles);
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_BODY, bodies);
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_SEVERITY, severities);
		}
		// Aggiunge i nuovi valori
		((List<String>)modelMap.get(KEY_NOTIFICATION_TITLE)).add(title);
		((List<String>)modelMap.get(KEY_NOTIFICATION_BODY)).add(message);
		((List<String>)modelMap.get(KEY_NOTIFICATION_SEVERITY)).add(severity);
		String newTotalSeverity = calcTotalSeverity(modelMap, severity);
		redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_TOTALSEVERITY, newTotalSeverity);
	}

	/**
	 * Test if a modal is going to be opened when back to the view (usually after a redirect)
	 * @param request
	 * @return true if a modal is going to be opened
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public boolean isNotifyModalPending(HttpServletRequest request) {
		Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
		return flashMap!=null && (
			flashMap.containsKey(KEY_NOTIFICATION_TITLE)
			|| flashMap.containsKey(KEY_NOTIFICATION_BODY)
			|| flashMap.containsKey(KEY_NOTIFICATION_SEVERITY)
			);
	}

	/**
	 * Da usare direttamente solo quando si vuole fare un redirect dopo aver mostrato un messaggio.
	 * Se chiamato tante volte, i messaggi si sommano e vengono mostrati tutti all'utente.
	 * @param title
	 * @param message
	 * @param severity a string like YadaConstants.VAL_NOTIFICATION_SEVERITY_OK
	 * @param redirectSemiurl e.g. "/user/profile"
	 * @param model
	 * @see YadaConstants
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void notifyModal(String title, String message, String severity, String redirectSemiurl, Model model) {
		if (severity==VAL_NOTIFICATION_SEVERITY_ERROR) {
			// Tutte le notifiche di errore vengono loggate a warn (potrebbero non essere degli errori del programma)
			log.warn("notifyModal: {} - {}", title, message);
		}
		// Mette nel model tre array di stringhe che contengono titolo, messaggio e severity.
		if (!model.containsAttribute(KEY_NOTIFICATION_TITLE)) {
			List<String> titles = new ArrayList<>();
			List<String> bodies = new ArrayList<>();
			List<String> severities = new ArrayList<>();
			model.addAttribute(KEY_NOTIFICATION_TITLE, titles);
			model.addAttribute(KEY_NOTIFICATION_BODY, bodies);
			model.addAttribute(KEY_NOTIFICATION_SEVERITY, severities);
		}
		// Aggiunge i nuovi valori
		Map<String, Object> modelMap = model.asMap();
		((List<String>)modelMap.get(KEY_NOTIFICATION_TITLE)).add(title);
		((List<String>)modelMap.get(KEY_NOTIFICATION_BODY)).add(message);
		((List<String>)modelMap.get(KEY_NOTIFICATION_SEVERITY)).add(severity);
		// Il redirect è sempre uno solo: prevale l'ultimo
		if (redirectSemiurl!=null) {
			model.addAttribute(KEY_NOTIFICATION_REDIRECT, redirectSemiurl);
		}
		String newTotalSeverity = calcTotalSeverity(modelMap, severity);
		model.addAttribute(KEY_NOTIFICATION_TOTALSEVERITY, newTotalSeverity);
	}

	/**
	 * Return true if modalError has been called before
	 * @param model
	 * @return
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public boolean isModalError(Model model) {
		return model.asMap().containsValue(VAL_NOTIFICATION_SEVERITY_ERROR);
	}

	/**
	 * Return true if for this thread the notifyModal (or a variant) has been called
	 * @param model
	 * @return
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public boolean isNotifyModalRequested(Model model) {
		return model.containsAttribute(KEY_NOTIFICATION_TITLE);
	}

	/**
	 * Calcola la severity totale del dialog dalla severity di tutti i messaggi esistenti (è la più alta tra tutti)
	 * @param modelMap
	 * @param lastSeverity
	 * @return
	 */
	@Deprecated
	private String calcTotalSeverity(Map<String, ?> modelMap, String lastSeverity) {
		// Algoritmo:
		// - se la total è error, resta error;
		// - se lastSeverity è ok e quella total è info, resta info;
		// - per cui
		// - se lastSeverity è error, diventa o resta error;
		// - se lastSeverity è info, diventa o resta info;
		// - se lastSeverity è ok, diventa ok visto che total non è nè error nè info;
		String newTotalSeverity = lastSeverity;
		String currentTotalSeverity = (String) modelMap.get(KEY_NOTIFICATION_TOTALSEVERITY);
		if (VAL_NOTIFICATION_SEVERITY_ERROR.equals(currentTotalSeverity)) {
			return currentTotalSeverity; // ERROR wins always
		}
		if (VAL_NOTIFICATION_SEVERITY_OK.equals(lastSeverity) && VAL_NOTIFICATION_SEVERITY_INFO.equals(currentTotalSeverity)) {
			newTotalSeverity = currentTotalSeverity; // INFO wins over OK
		}
		return newTotalSeverity;
	}

//	private void notifyModalSetValue(String title, String message, String severity, String redirectSemiurl, Model model) {
//		model.addAttribute(KEY_NOTIFICATION_TITLE, title);
//		model.addAttribute(KEY_NOTIFICATION_BODY, message);
//		model.addAttribute(KEY_NOTIFICATION_SEVERITY, severity);
//		if (redirectSemiurl!=null) {
//			model.addAttribute(KEY_NOTIFICATION_REDIRECT, redirectSemiurl);
//		}
//	}

	/**
	 * Returns the browser's remote ip address.
	 * If the connection uses a proxy that sets the "X-Forwarded-For" header, the result is taken from that header.
	 * @param request
	 * @return The client IP address, ignoring any proxy address when possible
	 */
	public String getClientAddress(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For"); // X-Forwarded-For: 203.0.113.195, 70.41.3.18, 150.172.238.178
		if (!StringUtils.isBlank(forwardedFor)) {
			return forwardedFor.split(",", 2)[0].trim();
		}
		return request.getRemoteAddr();
	}

	@Deprecated // Quite useless because of the format of the result
	public String getClientIp(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		String forwardedFor = request.getHeader("X-Forwarded-For");
		String remoteIp = "?";
		if (!StringUtils.isBlank(remoteAddr)) {
			remoteIp = remoteAddr;
		}
		if (!StringUtils.isBlank(forwardedFor)) {
			remoteIp = "[for " + forwardedFor + "]";
		}
		return remoteIp;
	}

	/**
	 *
	 * @param message text to be displayed (can be null for default value)
	 * @param confirmButton text of the confirm button (can be null for default value)
	 * @param cancelButton text of the cancel button (can be null for default value)
	 * @param model
	 */
	public String modalConfirm(String message, String confirmButton, String cancelButton, Model model) {
		return modalConfirm(message, confirmButton, cancelButton, model, false, false);
	}

	/**
	 * Show the confirm modal and reloads when the confirm button is pressed, adding the confirmation parameter to the url.
	 * The modal will be opened on load.
	 * Usually used by non-ajax methods.
	 * @param message text to be displayed (can be null for default value)
	 * @param confirmButton text of the confirm button (can be null for default value)
	 * @param cancelButton text of the cancel button (can be null for default value)
	 * @param model
	 */
	public String modalConfirmAndReload(String message, String confirmButton, String cancelButton, Model model) {
		return modalConfirm(message, confirmButton, cancelButton, model, true, true);
	}

	/**
	 * Show the confirm modal and optionally reloads when the confirm button is pressed
	 * @param message text to be displayed (can be null for default value)
	 * @param confirmButton text of the confirm button (can be null for default value)
	 * @param cancelButton text of the cancel button (can be null for default value)
	 * @param model
	 * @param reloadOnConfirm (optional) when true, the browser will reload on confirm, adding the confirmation parameter to the url
	 * @param openModal (optional) when true, the modal will be opened. To be used when the call is not ajax
	 */
	public String modalConfirm(String message, String confirmButton, String cancelButton, Model model, Boolean reloadOnConfirm, Boolean openModal) {
		model.addAttribute("message", message);
		model.addAttribute("confirmButton", confirmButton);
		model.addAttribute("cancelButton", cancelButton);
		if (openModal) {
			model.addAttribute("openModal", true);
		}
		if (reloadOnConfirm) {
			model.addAttribute("reloadOnConfirm", true);
		}
		return "/yada/modalConfirm";
	}

	/**
	 * Add a script id to call when opening the notification modal
	 * @param scriptId
	 * @param model
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated
	public void callScriptOnModal(String scriptId, Model model) {
		if (!model.containsAttribute(KEY_NOTIFICATION_CALLSCRIPT)) {
			List<String> scriptIds = new ArrayList<>();
			model.addAttribute(KEY_NOTIFICATION_CALLSCRIPT, scriptIds);
		}
		Map<String, Object> modelMap = model.asMap();
		((List<String>)modelMap.get(KEY_NOTIFICATION_CALLSCRIPT)).add(scriptId);
	}

}
