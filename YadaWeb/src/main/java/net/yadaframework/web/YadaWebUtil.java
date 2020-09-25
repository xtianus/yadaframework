package net.yadaframework.web;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaConstants;
import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.exceptions.YadaInvalidUsageException;

@Service
public class YadaWebUtil {

	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaConfiguration config;
	@Autowired private YadaUtil yadaUtil;
	@Autowired private MessageSource messageSource;

	public final Pageable FIND_ONE = new PageRequest(0, 1);

	// Characters that should never be found or placed in a slug
	private static final String PATTERN_INVALID_SLUG = "[:,;=&!+~()@*$'\"\\s]";

	private Map<String, List<?>> sortedLocalEnumCache = new HashMap<>();

	/**
	 * Copies the content of a file to the Response then deletes the file.
	 * To be used when the client needs to download a previously-created temporary file that you don't want to keep on server.
	 * Remember to set the "produces" attribute on the @RequestMapping with the appropriate content-type.
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
	 * Copies the content of a file to the Response then deletes the file.
	 * To be used when the client needs to download a previously-created file that you don't want to keep on server.
	 * Remember to set the "produces" attribute on the @RequestMapping with the appropriate content-type.
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
		String slug = source.trim().toLowerCase().replace('à', 'a').replace('è', 'e').replace('é', 'e').replace('ì', 'i').replace('ò', 'o').replace('ù', 'u').replace('.', '-');
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
	 * @return
	 */
	public String urlEncode(String source) {
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
	 * Ritorna l'indirizzo completo della webapp, tipo http://www.yodadog.net:8080/site, senza slash finale
	 * Da thymeleaf si usa con ${@yadaWebUtil.getWebappAddress(#httpServletRequest)}
	 * @param request
	 * @return
	 */
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
