package net.yadaframework.web;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.raw.YadaHttpUtil;

@Controller
public class YadaController {
	private final Logger log = LoggerFactory.getLogger(YadaController.class);

	@Autowired private YadaUtil yadaUtil;
	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private MessageSource messageSource;
	private YadaHttpUtil yadaHttpUtil = new YadaHttpUtil();

	// Error page
    @RequestMapping("/error")
    public String error(HttpServletRequest request, RedirectAttributes redirectAttributes, Model model, Locale locale) {
    	int errorCode = (int) request.getAttribute("javax.servlet.error.status_code");
    	// If not a document, return a short error page
    	int docType = yadaHttpUtil.getDocumentType(request.getContentType());
    	if (docType!=YadaHttpUtil.CONTENT_DOCUMENT && docType!=YadaHttpUtil.CONTENT_UNKNOWN) {
    		model.addAttribute("statusCode", errorCode);
    		return "/yada/httpError";
    	}
    	// If a document was requested, go home and open a modal
    	String errorMessage = messageSource.getMessage("yada.error.http.other", null, locale);
    	switch (errorCode) {
    	case 400: errorMessage = messageSource.getMessage("yada.error.http.400", null, locale); // BAD REQUEST
    	break;
    	case 403: errorMessage = messageSource.getMessage("yada.error.http.403", null, locale);
    	break;
    	case 404: errorMessage = messageSource.getMessage("yada.error.http.404", null, locale);
    	break;
    	}
    	
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
        if (throwable != null) {
        	log.error("Error code " +errorCode+ " shown to user: " + errorMessage, throwable);
        	errorMessage = throwable.getMessage()!=null?throwable.getMessage():"";
        	Throwable root = yadaUtil.getRootException(throwable);
        	String rootMessage = root.getMessage();
        	if (errorMessage!=null && rootMessage!=null && errorMessage.toLowerCase().indexOf(rootMessage.toLowerCase())<0) {
        		errorMessage += " - " + rootMessage;
        	}
        } else {
        	log.error("Error code " +errorCode+ " shown to user (no details): " + errorMessage);
        }
        String title = messageSource.getMessage("yada.error.http.title", new Object[] {errorCode}, locale);
        YadaNotify.instance(redirectAttributes).yadaError().yadaTitle(title).yadaMessage(errorMessage).yadaSave();
//        yadaWebUtil.modalError(title, errorMessage, redirectAttributes);
        return "redirect:/";
    }	

}
