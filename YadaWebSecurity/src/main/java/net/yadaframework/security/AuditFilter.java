package net.yadaframework.security;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaWebConfig;


/**
 * Inietta il session ID nell'MDC di logback, chiamandolo "session". Si usa con '%X{session}' nel pattern.
 * Inietta anche "username" e "remoteIp"
 * Stampa i parametri di get e post se il debug è attivo.
 * Scrive il tempo di risposta della action sul logger chiamato "actionDuration"
 */
public class AuditFilter extends OncePerRequestFilter implements Filter {
	private final static Logger log = LoggerFactory.getLogger(AuditFilter.class);
	private final static Logger filesLog = LoggerFactory.getLogger(AuditFilter.class.getName() + ".files");
//	private final static Logger actionLog = LoggerFactory.getLogger("actionDuration");
	private static final String MDC_USERNAME = "username";
	private static final String MDC_SESSION = "session";
	private static final String MDC_REMOTEIP = "remoteIp";
	private static final String MDC_TRACEID = "traceId"; // Must sync with YadaTraceStatementInspector
	private static final String MDC_ENDPOINT = "endpoint";

	private String yadaResourceUrlStart = null; // Prefix for resource urls from /src/main/webapp/yadares
	private String resourceUrlStart = null; // Prefix for resource urls from /src/main/webapp/res
	private String staticUrlStart = null; // Prefix for resource urls from /src/main/webapp/static
	private String contentUrlStart = null; // Prefix for contents urls (usually they are served by apache anyway)

	/**
	 * Forwards the request to the next filter in the chain and delegates down to the subclasses to perform the actual
	 * request logging both before and after the request is processed.
	 *
	 * @see #beforeRequest
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		if (resourceUrlStart==null) {
			resourceUrlStart = request.getContextPath() + YadaWebConfig.getResourceFolder() + "-"; // /site/res-
		}
		if (staticUrlStart==null) {
			staticUrlStart = request.getContextPath() + YadaWebConfig.getStaticFileFolder(); // /site/static
		}
		if (yadaResourceUrlStart==null) {
			yadaResourceUrlStart = request.getContextPath() + YadaWebConfig.getYadaResourceFolder(); // /site/yadares
		}
		if (contentUrlStart==null) {
			try {
				YadaConfiguration config = (YadaConfiguration) YadaUtil.getBean("config"); // Throws NoSuchBeanDefinitionException
				contentUrlStart = request.getContextPath() + config.getContentUrl();  // /site/contents
			} catch (Exception e) {
				log.debug("No YadaConfiguration found yet (ignored)");
			}
		}
		if (!filesLog.isInfoEnabled() && isFile(request)) {
			// If the logger is configured to skip files, continue and return.
			// Example: <logger name="net.yadaframework.security.AuditFilter.files" level="ERROR"/>
			try {
				filterChain.doFilter(request, response);
			} catch (Exception e) {
				// Log the file anyway in case of exception
				String requestUri = request.getRequestURI();
				log.error("Error loading {}: {}", requestUri, e.getMessage());
				throw e;
			}
			return;
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
		String traceId = YadaUtil.INSTANCE.getRandomString(8); // UUID.randomUUID().toString();
		
		MDC.put(MDC_SESSION, sessionId); // Session viene messo sull'MDC. Usarlo con %X{session} nel pattern
		MDC.put(MDC_USERNAME, username); // username viene messo sull'MDC. Usarlo con %X{username} nel pattern
		MDC.put(MDC_REMOTEIP, remoteIp); // remoteIp viene messo sull'MDC. Usarlo con %X{remoteIp} nel pattern
		MDC.put(MDC_TRACEID, traceId); // traceId viene messo sull'MDC. Usarlo con %X{traceId} nel pattern
		MDC.put(MDC_ENDPOINT, requestUri); // requestUri viene messo sull'MDC. Usarlo con %X{endpoint} nel pattern

		if (isFirstRequest) {
			beforeRequest(request);
			startTime = System.currentTimeMillis();
		}
		try {
			filterChain.doFilter(request, response);
			int status = response.getStatus();
			if (!isFile(request) && !isAsyncStarted(request)) {
				// The time taken is for non-files only
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
			MDC.remove(MDC_TRACEID);
			MDC.remove(MDC_ENDPOINT);
			// Return the generated traceId in the response header for debugging
			response.setHeader("X-Trace-Id", traceId);
		}
	}

	/**
	 * Returns true if the request is for a file, not for a controller
	 * @return
	 */
	private boolean isFile(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		return (resourceUrlStart!=null && requestUri.startsWith(resourceUrlStart))
			|| (yadaResourceUrlStart!=null && requestUri.startsWith(yadaResourceUrlStart))
			|| (staticUrlStart!=null && requestUri.startsWith(staticUrlStart))
			|| (contentUrlStart!=null && requestUri.startsWith(contentUrlStart));
		}

	// Stampo i parametri di request (get e post)
	protected void beforeRequest(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String queryString = request.getQueryString();
		boolean maliciousString = (requestUri!=null && requestUri.contains(";")) || (queryString!=null && queryString.contains(";"));
		if (log.isInfoEnabled() || maliciousString) {
			try {
				String ajaxFlag = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))?" (ajax)":"";
				if (maliciousString) {
					log.warn("requestUri:{}" + ajaxFlag, requestUri);
					if (queryString!=null) {
						log.warn("queryString:{}", queryString);
					}
				} else {
					log.info("requestUri:{}" + ajaxFlag, requestUri);
					if (queryString!=null) {
						log.info("queryString:{}", queryString);
					}
				}
				if (log.isDebugEnabled()) {
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
						if ("password".equals(paramName) || "confirmPassword".equals(paramName)) {
							paramString=new StringBuffer("[value hidden from log]");
						}
						log.debug("** {} = {} **", paramName, paramString);
					}
					if (postDataMap.isEmpty()) {
						if (new StandardServletMultipartResolver().isMultipart(request)) {
							log.debug("** multipart request");
						} else if (request.getContentType().equals("application/json;charset=UTF-8")) {
							log.debug("** json object");
						}
					}
				}
			} catch (Throwable e) {
				// Ignoro
			}
		}
	}

}
