package net.yadaframework.security;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.multipart.support.MultipartFilter;

import net.yadaframework.security.components.YadaAuthenticationSuccessFilter;

@Order(1)
public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {
	// Qui non si può usare @Autowired

	@Override
	// Questo serve (anche) per consentire il file upload verso url protette:
	//   The first option is to ensure that the MultipartFilter is specified before the Spring Security filter.
	//   Specifying the MultipartFilter before the Spring Security filter means that there is no authorization
	//   for invoking the MultipartFilter which means anyone can place temporary files on your server.
	//   However, only authorized users will be able to submit a File that is processed by your application.
	//   In general, this is the recommended approach because the temporary file upload should have a
	//   negligble impact on most servers.
    protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
		// Per aggiungere altri filtri basta metterli in fila, in ordine di esecuzione
        // insertFilters(servletContext, new YadaMultipartExceptionHandler(), new MultipartFilter());
		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
		characterEncodingFilter.setEncoding("UTF-8");
		characterEncodingFilter.setForceEncoding(true);
		// L'AuditFilter lo metto prima di tutto almeno viene eseguito prima dell'autorizzazione e riesco a capire il motivo di eventuali 403
		// For some reason the characterEncodingFilter can not be after the MultipartFilter otherwise encoding in forms doesn't work (for non-multipart forms).
        insertFilters(servletContext,
        	new CheckSessionFilter(),
        	characterEncodingFilter,	// TODO Why is this in the YadaWebSecurity project and not in YadaWeb?
        	new AuditFilter(), 			// TODO Why is this in the YadaWebSecurity project and not in YadaWeb?
        	new MultipartFilter(),		// TODO Why is this in the YadaWebSecurity project and not in YadaWeb?
        	new DelegatingFilterProxy("yadaLocalePathVariableFilter"), // TODO Why is this in the YadaWebSecurity project and not in YadaWeb?
	        new YadaAuthenticationSuccessFilter()
	    );
    }

	/**
	 * We need to add FORWARD to the filter mapping otherwise yadaLocalePathVariableFilter will skip security on forward (and all the following filters too)
	 */
	@Override
	protected  EnumSet<DispatcherType> getSecurityDispatcherTypes() {
		return EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC, DispatcherType.FORWARD);
	}

}
