package net.yadaframework.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaConstants;

/**
 * Handles all exceptions exiting a @Controller that have not been annotated with @ResponseStatus
 * http://ankursinghal86.blogspot.it/2014/07/exception-handling-in-spring-mvc.html
 */
@ControllerAdvice
public class YadaGlobalExceptionHandler {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaConfiguration config;
	@Autowired private YadaWebUtil yadaWebUtil;

	private final static String LOOPCOUNTER_KEY="yada-loop-counter";
	private final static int LOOPCOUNTER_MAX=2;

	@ExceptionHandler(value = Exception.class)
	public ModelAndView globalErrorHandler(HttpServletRequest request, Exception e) throws Exception {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject(YadaConstants.REQUEST_HASERROR_FLAG, "true");
		// Count the times we get here, to avoid looping in case of repeating errors on forward
		Integer loops = (Integer) request.getAttribute(LOOPCOUNTER_KEY);
		if (loops==null) {
			loops = 1;
		} else {
			loops = loops+1;
		}
		request.setAttribute(LOOPCOUNTER_KEY, loops);
		if (loops>=LOOPCOUNTER_MAX) {
			log.error("Emergency exit from request after {} loops - redirecting to home", loops);
			modelAndView.setViewName("redirect:/");
			return modelAndView;
		}

		// If the exception is annotated with @ResponseStatus rethrow it and let the framework handle it.
        if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null) {
        	log.info("Rethrowing @ResponseStatus exception: {}", e.toString());
            throw e;
        }
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
