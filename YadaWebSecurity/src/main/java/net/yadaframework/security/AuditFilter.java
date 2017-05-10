package net.yadaframework.security;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.filter.OncePerRequestFilter;

import net.yadaframework.core.YadaWebConfig;


/**
 * Inietta il session ID nell'MDC di logback, chiamandolo "session". Si usa con '%X{session}' nel pattern.
 * Inietta anche "username" e "remoteIp"
 * Stampa i parametri di get e post se il debug è attivo.
 * Scrive il tempo di risposta della action sul logger chiamato "actionDuration"
 */
public class AuditFilter extends OncePerRequestFilter {
	private final static Logger log = LoggerFactory.getLogger(AuditFilter.class);
//	private final static Logger actionLog = LoggerFactory.getLogger("actionDuration");
	private static final String MDC_USERNAME = "username";
	private static final String MDC_SESSION = "session";
	private static final String MDC_REMOTEIP = "remoteIp";
	
	private String resourceUrlStart = null;
	
	/**
	 * Forwards the request to the next filter in the chain and delegates down to the subclasses to perform the actual
	 * request logging both before and after the request is processed.
	 *
	 * @see #beforeRequest
	 * @see #afterRequest
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		if (resourceUrlStart==null) {
			resourceUrlStart = request.getContextPath() + YadaWebConfig.getResourceFolder() + "-"; // /site/res-
		}
		boolean isFirstRequest = !isAsyncDispatch(request);
		long startTime = -1;
		String sessionId = "";
		String username = "";
		// Questo codice per il calcolo dell'IP è stato messo in YadaWebUtil ma accedere quello da qui non viene facile, per cui duplico qui
		String remoteAddr = request.getRemoteAddr();
		String forwardedFor = request.getHeader("X-Forwarded-For");
		String remoteIp = "?";
		if (!StringUtils.isBlank(remoteAddr)) {
			remoteIp = remoteAddr;
		}
		if (!StringUtils.isBlank(forwardedFor)) {
			remoteIp = "[for " + forwardedFor + "]";
		}
		//
		String requestUri = request.getRequestURI();
		HttpSession session = request.getSession(false);
		SecurityContext securityContext = null;
		if (session!=null) {
			sessionId = StringUtils.trimToEmpty(session.getId());
			securityContext = ((SecurityContext)session.getAttribute("SPRING_SECURITY_CONTEXT"));
		}
		if (sessionId.length()==0) {
			sessionId = "req-"+Integer.toString(request.hashCode());
		}
		if (securityContext!=null) {
			try {
				username = securityContext.getAuthentication().getName();
			} catch (Exception e) {
				log.debug("No username in securityContext");
			}
		}
		MDC.put(MDC_SESSION, sessionId); // Session viene messo sull'MDC. Usarlo con %X{session} nel pattern
		MDC.put(MDC_USERNAME, username); // username viene messo sull'MDC. Usarlo con %X{username} nel pattern		
		MDC.put(MDC_REMOTEIP, remoteIp); // remoteIp viene messo sull'MDC. Usarlo con %X{remoteIp} nel pattern		

		if (isFirstRequest) {
			beforeRequest(request);
			startTime = System.currentTimeMillis();
		}
		try {
			filterChain.doFilter(request, response);
			int status = response.getStatus();
			if (!skipDuration(requestUri) && !isAsyncStarted(request)) {
				if (startTime>-1) {
					long timetaken = System.currentTimeMillis()-startTime;
					log.info("{}: {} ms (HTTP {})", requestUri, timetaken, status);
				}
			} else if (status>399) {
				log.error("{} HTTP {}", requestUri, status);
			}
		} finally {
			MDC.remove(MDC_USERNAME);
			MDC.remove(MDC_SESSION);
			MDC.remove(MDC_REMOTEIP);
		}
	}
	
	private boolean skipDuration(String requestUri) {
		return requestUri.startsWith(resourceUrlStart) // Skippo tutte le res
			// Solo che i /contents sono configurati e non ho accesso alla configurazione, quindi skippo in base all'estensione
			|| requestUri.endsWith(".jpg")
			|| requestUri.endsWith(".png")
			|| requestUri.endsWith(".gif");
//		return requestUri.endsWith(".css")
//			|| requestUri.endsWith(".js")
	}
	
	// Stampo i parametri di request (get e post)
	protected void beforeRequest(HttpServletRequest request) {
		if (log.isInfoEnabled()) {
			try {
				String requestUri = request.getRequestURI();
				String queryString = request.getQueryString();
				String ajaxFlag = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))?" (ajax)":"";
				log.info("requestUri:{}" + ajaxFlag, requestUri);
				if (queryString!=null) {
					log.info("queryString:{}", queryString);
				}
				Map<String, String[]> postDataMap = request.getParameterMap();
				for (String paramName : postDataMap.keySet()) {
					String[] paramValue = postDataMap.get(paramName);
					StringBuffer paramString = new StringBuffer();
					for (int i = 0; i < paramValue.length; i++) {
						if (i>0) {
							paramString.append(" & ");
						}
						paramString.append(paramValue[i]);
					}
					if ("password".equals(paramName)) {
						paramString=new StringBuffer("[value hidden from log]");
					}
					log.info("** {} = {} **", paramName, paramString);
				}
				if (postDataMap.isEmpty()) {
					if (org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent(request)) {
						log.info("** multipart request");
					}
				}
			} catch (Throwable e) {
				// Ignoro
			}
		}
	}

}
