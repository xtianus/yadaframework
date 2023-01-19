package net.yadaframework.web;

import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaConstants;

@Controller
public class YadaController {
	private final Logger log = LoggerFactory.getLogger(YadaController.class);

	@Autowired private YadaConfiguration config;
	@Autowired private YadaWebUtil yadaWebUtil;

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
    @RequestMapping("/yadaError")
    public String yadaError(HttpServletRequest request, RedirectAttributes redirectAttributes, Model model, Locale locale) {
    	// The original request has been lost already, but the status code is kept
    	int errorCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String errorMessage = (String) request.getAttribute("javax.servlet.error.message");
        Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");
        if (StringUtils.isBlank(errorMessage) && exception!=null) {
        	errorMessage = exception.getMessage();
        }
        String isAjaxString = yadaWebUtil.isAjaxRequest(request)?" (ajax)":"";
        if (exception==null) {
        	log.error("Error (HTTP {} '{}') shown to user {}", errorCode, errorMessage, isAjaxString);
        } else {
        	log.error("Error (HTTP {} '{}') shown to user {}", errorCode, errorMessage, isAjaxString, exception);
        }
        model.addAttribute(YadaConstants.REQUEST_HASERROR_FLAG, "true");
        // If it was an ajax request, return an error object
        if (yadaWebUtil.isAjaxRequest(request)) {
        	model.addAttribute("errorDescription", errorMessage);
        	return "/yada/ajaxError";
        }
        // Otherwise forward to the configured error page (defaults to home)
        model.addAttribute("yadaHttpStatus", errorCode);
        model.addAttribute("yadaHttpMessage", errorMessage);
        // ATTENTION: if some response has already been sent, the following might result in the error page appended to the current page
        return "forward:" + config.getErrorPageForward();
//        YadaLocalePathVariableFilter.resetCalledFlag(request); // Reset so that the locale is set again and stripped
//        return "forward:" + yadaWebUtil.getLocaleSafeForward(config.getErrorPageForward());
    }

}
