package net.yadaframework.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class YadaAjaxInterceptor implements HandlerInterceptor {
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
		if (request.getHeader("X-Requested-With")!=null && modelAndView!=null) {
			// Ajax request
			// Using "yadaIsAjaxResponse" as a value instead of "true" so that ${yadaIsAjaxResponse} can be used as a class directly:
			// <div th:classappend="${yadaIsAjaxResponse}"
			modelAndView.addObject("yadaIsAjaxResponse", "yadaIsAjaxResponse");
		}
	}
}