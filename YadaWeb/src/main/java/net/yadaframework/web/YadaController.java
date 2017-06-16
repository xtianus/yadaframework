package net.yadaframework.web;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.core.YadaConfiguration;

@Controller
public class YadaController {
	private final Logger log = LoggerFactory.getLogger(YadaController.class);

	@Autowired private YadaConfiguration config;

	// Error page for HTTP error codes
    @RequestMapping("/yadaError")
    public String yadaError(HttpServletRequest request, RedirectAttributes redirectAttributes, Model model, Locale locale) {
    	// The original request has been lost already, but the status code is kept
    	int errorCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String errorMessage = (String) request.getAttribute("javax.servlet.error.message");
        log.error("Error code {} '{}' shown to user", errorCode, errorMessage);
        model.addAttribute("yadaHttpStatus", errorCode);
        model.addAttribute("yadaHttpMessage", errorMessage);
        return "forward:" + config.getErrorPageForward();
    }	

}
