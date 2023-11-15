package net.yadaframework.security;
import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Questo filtro viene eseguito prima di qualunque cosa e, se la richiesta è COMMAND, ritorna "active" o "expired" riguardo la session.
 * Attenzione perchè allo stato attuale fa un touch sulla session ogni volta che viene chiamato e non so come evitarlo.
 *
 */
public class CheckSessionFilter extends GenericFilterBean implements Filter {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	private final static String COMMAND = "/ajaxCheckSessionActive";

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
		// FIXME per qualche strana ragione la sessione non scade se questa url viene ripetutamente chiamata!
		// Per ovviare al problema, faccio il controllo via js con un timeout pari a quello di sessione, in modo che quando arriva è già scaduta,
		// e male che vada la sessione dura il doppio del session timeout impostato (metti che un ajax rinfresca subito dopo il page load per cui il js che entra qui si trova la sessione ancora attiva e la rinfresca)
		if ((servletRequest instanceof HttpServletRequest) && (servletResponse instanceof HttpServletResponse)) {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			String requestUri = request.getRequestURI();
			if (requestUri.endsWith(COMMAND)) {
				HttpSession session = request.getSession(false);
				String result = "expired";
				if (session!=null) {
					result = "active";
				}
				if (log.isDebugEnabled()) {
					log.debug(COMMAND + " returned " + result + (session!=null?" "+session.getId():""));
				}
				Writer out = response.getWriter();
				out.write(result);
				out.close();
				return;
			} 
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}


}
