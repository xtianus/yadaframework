package net.yadaframework.web;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.yadaframework.components.YadaDataTableFactory;
import net.yadaframework.components.YadaLogRedaction;
import net.yadaframework.components.YadaSecurityUtilStub;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaConstants;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.YadaDataTableDao;
import net.yadaframework.web.datatables.proxy.YadaDataTableProxy;

@Controller
public class YadaController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired private YadaConfiguration config;
	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaDataTableDao yadaDataTableDao;
	@Autowired private YadaDataTableFactory yadaDataTableFactory;
	// Need to use the stub to avoid circular dependency with YadaWebSecurity
	@Autowired private YadaSecurityUtilStub yadaSecurityUtil;

	/**
	 * Get the data for a datatable.
	 * This method is called automatically when a YadaDataTable doesn't have an ajaxUrl set.
	 * @param yadaDatatablesRequest the request parameters sent by DataTables
	 * @param locale the locale to use for localized values
	 * @return one page of data
	 * @throws AccessDeniedException when there is a security violation
	 */
	// The mapping must match YadaDataTable.prepareConfiguration()
	@RequestMapping(value ="/yadaDataTableData", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public Map<String, Object> yadaDataTableData(YadaDatatablesRequest yadaDatatablesRequest, HttpServletRequest request, Locale locale) throws AccessDeniedException {
		String dataTableId = yadaDatatablesRequest.getDataTableId();
		if (dataTableId == null) {
			throw new YadaInvalidUsageException("Missing dataTableId in request");
		}
		YadaDataTableProxy yadaDataTable = yadaDataTableFactory.getSingleton(dataTableId, locale); // throws YadaInvalidUsageException if not found
		String securityAsPath = yadaDataTable.getSecurityAsPath();
		Class entityClass = yadaDataTable.getEntityClass();
		if (entityClass == null) {
			log.error("Entity class not found for dataTableId '{}'", dataTableId);
			throw new YadaInvalidUsageException("Entity class must be set in dataTableId '{}'", dataTableId);
		}
		if (securityAsPath!=null) {
			boolean allowed = yadaSecurityUtil.checkUrlAccess(request, securityAsPath);
			if (!allowed) {
				throw new AccessDeniedException("Access is denied to data for table '"+dataTableId+"' because path '"+ securityAsPath + "' is forbidden to current user by configuration");
			}
		}
		// Columns, search, sort and output come from the table configuration, never from the request
		return yadaDataTableDao.getConvertedJsonPage(yadaDatatablesRequest, entityClass, locale, yadaDataTable.getQueryPolicy());
	}

	/**
	 * Set the user timezone. Called by yada.js at each new browser session
	 * @param timezone the timezone from the browser
	 * @return
	 */
	@RequestMapping("/yadaTimezone")
	public String yadaTimezone(String timezone, HttpSession httpSession, Model model, Locale locale) {
//		int offsetHours =  timezoneOffset/60;
//		int offsetMinues = timezoneOffset - offsetHours*60;
//		ZoneOffset userZoneOffset = ZoneOffset.ofHoursMinutes(-offsetHours, -offsetMinues);
		TimeZone userTimeZone = TimeZone.getTimeZone(StringUtils.trimToEmpty(timezone));
		//  Can't use YadaSession here
		httpSession.setAttribute(YadaConstants.SESSION_USER_TIMEZONE, userTimeZone);
		return YadaViews.AJAX_SUCCESS;
	}

	// Error page for HTTP error codes
	// NOTE: when there is a thymeleaf exception, the output page has already partially been sent to the browser together with
	// 		 the HTTP error code in the header (which will be 200) so there's nothing we can do to show the error page.
    @RequestMapping("/yadaError")
    public String yadaError(HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes, Model model, Locale locale) {
    	// The original request has been lost already, but the status code is kept
    	int errorCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    	String errorMessage = (String) request.getAttribute("jakarta.servlet.error.message");
    	Exception exception = (Exception) request.getAttribute("jakarta.servlet.error.exception");
    	String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        if (StringUtils.isBlank(errorMessage) && exception!=null) {
        	errorMessage = exception.getMessage();
        }
        String isAjaxString = yadaWebUtil.isAjaxRequest(request)?" (ajax)":"";
        String currentUsername = yadaSecurityUtil.getUsername();
        // The detailed message and the stacktrace go to the server log only
        if (exception==null) {
        	log.error("Error (HTTP {} '{}') shown to user '{}' at {}{}", errorCode, YadaLogRedaction.redactTokens(errorMessage), currentUsername, YadaLogRedaction.redactTokens(requestUri), isAjaxString);
        } else {
        	log.error("Error (HTTP {} '{}') shown to user '{}' at {}{}", errorCode, YadaLogRedaction.redactTokens(errorMessage), currentUsername, YadaLogRedaction.redactTokens(requestUri), isAjaxString, exception);
        }
        if (response.isCommitted()) {
        	// The browser has received part of the page (and the original status code) already, so a new view can't be rendered.
        	// Nothing is written either: exception text would be inserted in the middle of an unknown document.
        	// The response is just flushed, which also avoids a second error dispatch. It is deliberately not closed with a
        	// fake "complete" ending: a response that failed after commit must stay visibly truncated or aborted for the client.
        	try {
        		response.flushBuffer();
        	} catch (IOException | RuntimeException ex) {
        		log.debug("Error flushing the committed response", ex);
        	}
        	return null;
        }
        // Only a generic text derived from the status code is ever sent to the client
        String safeMessage = getSafeErrorMessage(errorCode);
        model.addAttribute(YadaConstants.REQUEST_HASERROR_FLAG, "true");
        // If it was an ajax request, return an error object
        if (yadaWebUtil.isAjaxRequest(request)) {
        	model.addAttribute("errorDescription", safeMessage);
        	return "/yada/ajaxError";
        }
        // Otherwise forward to the configured error page (defaults to home)
        model.addAttribute("yadaHttpStatus", errorCode);
        model.addAttribute("yadaHttpMessage", safeMessage);
        // Clear any buffered content before sending a new response
        response.resetBuffer();
        // Preserve the original servlet error status while rendering the configured error page
        response.setStatus(errorCode);
        return "forward:" + config.getErrorPageForward();
//        YadaLocalePathVariableFilter.resetCalledFlag(request); // Reset so that the locale is set again and stripped
//        return "forward:" + yadaWebUtil.getLocaleSafeForward(config.getErrorPageForward());
    }

    /**
     * The text that can be shown to the user for an HTTP error status: the standard reason phrase, never exception-derived text.
     * The text is English, like the log messages: localizing it would need a message bundle entry per status.
     * @param errorCode the HTTP status code
     * @return the reason phrase, or "Server Error" for an unknown status
     */
    static String getSafeErrorMessage(int errorCode) {
    	HttpStatus status = HttpStatus.resolve(errorCode);
    	return status==null?"Server Error":status.getReasonPhrase();
    }

}
