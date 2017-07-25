package net.yadaframework.web;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import net.yadaframework.core.YadaConfiguration;

/**
 * Handles all exceptions exiting a @Controller that have not been annotated with @ResponseStatus
 * http://ankursinghal86.blogspot.it/2014/07/exception-handling-in-spring-mvc.html
 */
@ControllerAdvice
public class YadaGlobalExceptionHandler {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired private YadaConfiguration config;
	@Autowired private YadaWebUtil yadaWebUtil;
	
	@ExceptionHandler(value = Exception.class)
	public ModelAndView globalErrorHandler(HttpServletRequest request, Exception e) throws Exception {
		// If the exception is annotated with @ResponseStatus rethrow it and let the framework handle it.
        if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null) {
        	log.info("Rethrowing @ResponseStatus exception: {}", e.toString());
            throw e;
        }
        ModelAndView modelAndView = new ModelAndView();
        log.error("Unhandled exception shown to user", e);
        if (yadaWebUtil.isAjaxRequest(request)) {
        	// If it was an ajax request, return an error object
        	modelAndView.setViewName("/yada/ajaxError");
        	modelAndView.addObject("errorDescription", e.getMessage());
        } else {
        	// Otherwise forward to the configured error page (defaults to home)
        	modelAndView.setViewName("forward:" + config.getErrorPageForward());
        	modelAndView.addObject("yadaExceptionObject", e);
        	modelAndView.addObject("yadaExceptionUrl", request.getRequestURL());
        }
        return modelAndView;   
    }
	
}
