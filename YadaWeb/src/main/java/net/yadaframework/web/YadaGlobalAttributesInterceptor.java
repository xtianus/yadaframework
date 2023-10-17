//TODO: package net.yadaframework.web;
//
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.servlet.ModelAndView;
//import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
//import org.springframework.web.servlet.view.RedirectView;
//import org.springframework.web.servlet.view.UrlBasedViewResolver;
//
//import net.yadaframework.components.YadaUtil;
//import net.yadaframework.core.YadaConfiguration;
//
//@Deprecated
//// E' inutile aggiungere oggetti al Model visto che tutti i bean del context sono accessibili con ${@miobean}
//// Magari può tornare utile per accedere al Controller corrente? Lo trovo già nel context? Ce lo posso mettere?
//
//public class YadaGlobalAttributesInterceptor extends HandlerInterceptorAdapter {
//	@Autowired YadaConfiguration config;
//	@Autowired YadaUtil yadaUtil;
//
//	@Override
//	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//		boolean isRedirectView = modelAndView!=null && modelAndView.getView() instanceof RedirectView;
////		boolean isViewObject = modelAndView.getView() == null;
//		// if the view name is null then set a default value of true
//		boolean viewNameStartsWithRedirect = (modelAndView!=null && modelAndView.getViewName() == null ? true
//				: modelAndView!=null && modelAndView.getViewName().startsWith(UrlBasedViewResolver.REDIRECT_URL_PREFIX));
//		if (modelAndView!=null && modelAndView.hasView() && !isRedirectView && !viewNameStartsWithRedirect) {
//			addCommonModelData(request, response, modelAndView);
//		}
//	}
//
//	public void addCommonModelData(HttpServletRequest request, HttpServletResponse response, ModelAndView modelAndView){
//		modelAndView.addObject("yadaUtil", yadaUtil); // TODO Rimnuovere: meglio non usare ${yadaUtil} ma preferire ${@yadaUtil}
//	}
//
////	@Override
////	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
////	}
//
//}
//
