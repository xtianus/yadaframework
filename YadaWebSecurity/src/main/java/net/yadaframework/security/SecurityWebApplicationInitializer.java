package net.yadaframework.security;

import javax.servlet.ServletContext;

import org.springframework.core.annotation.Order;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.multipart.support.MultipartFilter;

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
        insertFilters(servletContext, new CheckSessionFilter(), characterEncodingFilter, new AuditFilter(), new MultipartFilter());
    }
}
