package net.yadaframework.core;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.format.Formatter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.web.dialect.YadaDialect;



//@Configuration not needed when using WebApplicationInitializer.java
@EnableWebMvc
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = { "net.yadaframework.web" })
public class YadaWebConfig implements WebMvcConfigurer {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	//	private final static long MB = 1024*1024;
//	private final static long MAXFILESIZE = 10*MB;
	public final static int MILLIS_IN_SECOND = 1000;
	public final static int SECONDS_IN_MINUTE = 60;
	public final static int MILLIS_IN_MINUTE = SECONDS_IN_MINUTE*MILLIS_IN_SECOND;

	// TODO put in configuration file
	protected final static String STATIC_RESOURCES_FOLDER = "/res";
	protected final static String STATIC_YADARESOURCES_FOLDER = "/yadares";
	protected final static String STATIC_FILE_FOLDER = "/static";

	@Autowired protected YadaConfiguration config;

	@Autowired protected ApplicationContext applicationContext;

    @Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @PostConstruct
    public void init() {
    	// the content of the default Model should never be used if a controller method redirects
    	// http://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/redirect-attributes/
       requestMappingHandlerAdapter.setIgnoreDefaultModelOnRedirect(true);
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		int minutes = config.getAsyncTimeoutMinutes();
		if (minutes>0) {
			configurer.setDefaultTimeout(minutes*MILLIS_IN_MINUTE);
		}
	}

	/**
	 * Return a string pattern to match urls that should not be localised when using a language path variable
	 * i.e. the language code will not be added when using @{} in thymeleaf
	 * @return "(?:/res|/yadares|/static|/contents)"
	 */
	public String getNotLocalizedResourcePattern() {
		String contentUrl = config.getContentUrl();
		StringBuilder result = new StringBuilder();
		result.append("(?:");
		result.append(STATIC_RESOURCES_FOLDER+"/");
		result.append("|");
		result.append(STATIC_YADARESOURCES_FOLDER+"/");
		result.append("|");
		result.append(STATIC_FILE_FOLDER+"/");
		// result.append("|/loginPost");
		if (config.isContentUrlLocal()) {
			result.append("|");
			result.append(contentUrl+"/");
		}
		result.append(")");
		return result.toString();
	}

	/**
	 * Name of the folder where versioned static yada files are to be found, starting with /
	 * @return
	 */
	public static String getYadaResourceFolder() {
		return STATIC_YADARESOURCES_FOLDER;
	}

	/**
	 * Name of the folder where versioned static files are to be found, starting with /
	 * @return
	 */
	public static String getResourceFolder() {
		return STATIC_RESOURCES_FOLDER;
	}

	/**
	 * Name of the folder where non-versioned static files are to be found, starting with /
	 * @return
	 */
	public static String getStaticFileFolder() {
		return STATIC_FILE_FOLDER;
	}

	// Needed for Spring Data
	// Lets you use Pageable in @Controller request parameters
	//	@Override
	//	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	//	    PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
	//	    argumentResolvers.add(resolver);
	//	}

	//
	// Locale handling
	//
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
		registry.addInterceptor(yadaLocalePathChangeInterceptor());
		registry.addInterceptor(new YadaAjaxInterceptor());
	}
	//
	// This is the standard locale implementation using request parameters
	// See http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#mvc-localeresolver
	//
	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
		localeChangeInterceptor.setParamName("lang");
		return localeChangeInterceptor;
	}
	//
	// Setting the locale based on a path variable like /en/home
	// See https://stackoverflow.com/a/23847484/587641
	//
	@Bean
	public YadaLocalePathChangeInterceptor yadaLocalePathChangeInterceptor() {
		YadaLocalePathChangeInterceptor localeChangeInterceptor = new YadaLocalePathChangeInterceptor();
		return localeChangeInterceptor;
	}

	//
	// This is setting the locale using cookies.
	// The first time, when no cookies are set, the locale is taken from the "accept-language" request header
	// but only if it is an accepted (configured) locale, otherwise the configured default locale is used.
	//
	@Bean(name = "localeResolver")
	public LocaleResolver localeResolver() {
		CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver() {
			@Override
			protected Locale determineDefaultLocale(HttpServletRequest request) {
				Set<Locale> acceptedLocales = config.getLocaleSet();
				Enumeration<Locale> preferredLocales = request.getLocales();
				// Find the first locale that is configured
		        while (preferredLocales.hasMoreElements()) {
		            Locale preferredLocale = preferredLocales.nextElement();
		            if (acceptedLocales.contains(preferredLocale)) {
		            	log.debug("Locale chosen from accept-language header: {}", preferredLocale);
		            	return preferredLocale;
		            }
		        }
		        // When none of the request locales are accepted, return the platform default locale
		        Locale defaultLocale = config.getDefaultLocale();
		        if (defaultLocale==null) {
		        	// This is needed to prevent Thymeleaf errors when no default locale has been configured
		        	defaultLocale = Locale.getDefault();
		        }
		        return defaultLocale;
			}
		};
		cookieLocaleResolver.setCookieMaxAge(Integer.MAX_VALUE);
		// The default is taken from the request header
		// NO: cookieLocaleResolver.setDefaultLocale(Locale.ENGLISH);
		return cookieLocaleResolver;
	}

	/**
	 * Register the configured DateFormatter or the default DefaultFormattingConversionService
	 * @return
	 */
	@Bean
	@Autowired // L'ho spostato qui per risolvere il problema "Requested bean is currently in creation"
	public FormattingConversionService mvcConversionService() {
		Formatter<Date> formatter = config.getDateFormatter();
		if (formatter!=null) {
			// Configure like <dateFormatter>net.yadaframework.components.YadaDateFormatter</dateFormatter> 
			FormattingConversionServiceFactoryBean result = new FormattingConversionServiceFactoryBean();
			Set<Formatter<Date>> formatters = new HashSet<Formatter<Date>>();
			formatters.add(formatter);
			result.setFormatters(formatters);
			result.afterPropertiesSet();
			return result.getObject();
		}
		return new DefaultFormattingConversionService();
	}
	
	/**
	 * Tutti i file dentro a /res vengono indicati come cacheabili lato browser per 1 anno (tramite l'header expires).
	 * Per evitare che nuove versioni non vengano mai prese, si usa il "trucco" di indicare il numero di build nell'url, così cambiando
	 * la build cambia l'url e la cache del browser va in miss la prima volta.
	 * Per sfruttare questo meccanismo bisogna usare lo YadaDialect con l'attributo yada:href, che si comporta come il th:href ma inserisce
	 * il numero di build nell'url calcolata. Per esempio: yada:href="@{/res/img/favicon.ico}"
	 * Stessa cosa per yada:src
	 * I file dentro a /static, invece, non cambiano mai nemmeno alle nuove release (anche se in cache stanno solo 100 giorni). Però non è per questo che si usa static, ma per il fatto che dentro ai commenti condizionali
	 * non si possono usare i tag thymeleaf, per cui ad esempio html5shiv.js viene messo in /static
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		// The official versioning code doesn't seem to work properly: even when adding a ResourceUrlEncodingFilter to rewrite links
		// See:
		// https://spring.io/blog/2014/07/24/spring-framework-4-1-handling-static-web-resources
		// http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#mvc-config-static-resources
		//	registry.addResourceHandler("/resources/**").addResourceLocations("/META-INF/")
		//		.setCachePeriod(8640000) // 100 days cache period
		//		.resourceChain(false).addResolver(new VersionResourceResolver().addFixedVersionStrategy(config.getApplicationBuild(), "/**/"));

		String res = STATIC_RESOURCES_FOLDER;
		if (res.endsWith("/")) {
			res = StringUtils.chop(res); // Remove last character
		}
		registry.addResourceHandler(res + "-" + config.getApplicationBuild() + "/**").addResourceLocations(res+"/").setCachePeriod(8640000); // 100 days cache period
		// Uso "-?*/**" per matchare anche eventuali versioni vecchie che qualcuno potrebbe avere in cache
		// Non si può fare perché non matcha
		// registry.addResourceHandler(res + "-*/**").addResourceLocations(res+"/").setCachePeriod(31556926); // 1 year cache period

		String s = STATIC_FILE_FOLDER;
		if (s.endsWith("/")) {
			s = StringUtils.chop(s); // Remove last character
		}
		registry.addResourceHandler(s + "/**").addResourceLocations(s+"/").setCachePeriod(8640000); // 100 days cache period

		// yadares prende le risorse dal classpath
		String yadares = STATIC_YADARESOURCES_FOLDER;
		if (yadares.endsWith("/")) {
			yadares = StringUtils.chop(res); // Remove last character
		}
		registry.addResourceHandler(yadares + "-" + config.getYadaVersion() + "/**").addResourceLocations("classpath:" + YadaConstants.YADA_VIEW_PREFIX+"/yada/").setCachePeriod(8640000); // 100 days cache period

		// Handling the "contents" uploaded locally
		// NOTE: if you don't need versioning but are happy with the apache file handling, just let apache serve the contents
		if (config.isContentUrlLocal()) {
			String contentUrl = config.getContentUrl();
			// TODO The problem with contents is that the version should be taken from the file timestamp so here it should accept any value but I don't know how to make it work with any version value
			registry.addResourceHandler(contentUrl + "/**").addResourceLocations("file:"+config.getContentPath() + "/").setCachePeriod(8640000); // 100 days cache period
		}

		// robots.txt is usually added by the deploy script depending on the environment
		registry.addResourceHandler("/robots.txt").addResourceLocations("/").setCachePeriod(86400); // 1 day cache period
	}

	//
	// Thymeleaf
	//

	// @Bean // No need for a @Bean?
	public ClassLoaderTemplateResolver yadaTemplateResolver() {
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		// Relative paths never work, with or without trailing slash, so better to be consistent without and always use "absolute" paths [xtian]
		resolver.setPrefix(YadaConstants.YADA_VIEW_PREFIX); // Attenzione allo slash finale!
//		resolver.setPrefix(YadaConstants.YADA_VIEW_PREFIX + "/"); // Attenzione allo slash finale!
		/* From the tutorial:
		 When several template resolvers are applied, it is recommended to specify patterns
		 for each template resolver so that Thymeleaf can quickly discard those template resolvers
		 that are not meant to resolve the template, enhancing performance. Doing this is not a
		 requirement, but a recommendation
		 */
		Set<String> patterns = new HashSet<>();
		patterns.add("/yada/*"); // Start with "yada". Patterns are NOT regexp
		patterns.add("/yadacms/*"); // Start with "yadacms". Patterns are NOT regexp
		resolver.setResolvablePatterns(patterns);
		resolver.setSuffix(".html");
		resolver.setCharacterEncoding("UTF-8");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(config.isProductionEnvironment());
		resolver.setOrder(10);
		return resolver;
	}


	/**
	 * Template resolver for mail preview pages that include email templates (maybe I could just use emailTemplateResolver())
	 * @return
	 */
	// @Bean // No need for a @Bean?
	public ITemplateResolver mailPreviewTemplateResolver() {
//		ServletContextTemplateResolver resolver = new ServletContextTemplateResolver();
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
	    resolver.setApplicationContext(applicationContext);
		resolver.setPrefix("/WEB-INF/classes/" + YadaConstants.EMAIL_TEMPLATES_PREFIX);
// The final slash is not needed because all email template paths must start with "/email/" as specified in the "patterns.add()" statement below
//		resolver.setPrefix("/WEB-INF/classes/" + YadaConstants.EMAIL_TEMPLATES_PREFIX + "/");
		/* From the tutorial:
		 When several template resolvers are applied, it is recommended to specify patterns
		 for each template resolver so that Thymeleaf can quickly discard those template resolvers
		 that are not meant to resolve the template, enhancing performance. Doing this is not a
		 requirement, but a recommendation
		 */
		Set<String> patterns = new HashSet<>();
		patterns.add("/email/*"); // Start with "/email/". Patterns are NOT regexp
		resolver.setResolvablePatterns(patterns);
		resolver.setSuffix(".html");
		resolver.setCharacterEncoding("UTF-8");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(config.isProductionEnvironment());
		resolver.setOrder(20);
		return resolver;
	}

	// x213

	// @Bean // No need for a @Bean?
	public ITemplateResolver webTemplateResolver() {
//		ServletContextTemplateResolver resolver = new ServletContextTemplateResolver();
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
	    resolver.setApplicationContext(applicationContext);
		// Relative paths never work, with or without trailing slash, so better to be consistent without and always use "absolute" paths [xtian]
		resolver.setPrefix("/WEB-INF/views");
//		resolver.setPrefix("/WEB-INF/views/");
		resolver.setSuffix(".html");
//		Set<String> patterns = new HashSet<>();
//		patterns.add("^(?!/yada/).*"); // Do not start with "yada" - DOES NOT WORK because it doesn't accept a regexp
//		resolver.setResolvablePatterns(patterns);
		resolver.setCharacterEncoding("UTF-8");
		// NB, selecting HTML5 as the template mode.
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(config.isProductionEnvironment());
		resolver.setOrder(30); // The last one, because it doesn't have any ResolvablePatterns
		return resolver;
	}

	// @Bean // No need for a @Bean?
	public ITemplateResolver javascriptTemplateResolver() {
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
	    resolver.setApplicationContext(applicationContext);
		// Relative paths never work, with or without trailing slash, so better to be consistent without and always use "absolute" paths [xtian]
		resolver.setPrefix("/WEB-INF/views");
		Set<String> patterns = new HashSet<>();
		patterns.add("/*.js"); // Ends with ".js"
		resolver.setResolvablePatterns(patterns);
		resolver.setSuffix(".js");
		resolver.setCharacterEncoding("UTF-8");
		resolver.setTemplateMode(TemplateMode.JAVASCRIPT);
		resolver.setCacheable(config.isProductionEnvironment());
		// resolver.setOrder(30); // Order not needed because resolver on different ViewResolver
		return resolver;
	}

	// @Bean // No need for a @Bean?
	public ITemplateResolver xmlTemplateResolver() {
//		ServletContextTemplateResolver resolver = new ServletContextTemplateResolver();
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
	    resolver.setApplicationContext(applicationContext);
		// Relative paths never work, with or without trailing slash, so better to be consistent without and always use "absolute" paths [xtian]
		resolver.setPrefix("/WEB-INF/views");
//		resolver.setPrefix("/WEB-INF/views/");
		Set<String> patterns = new HashSet<>();
		patterns.add("/xml/*"); // Start with "xml". Patterns are NOT regexp
		resolver.setResolvablePatterns(patterns);
		resolver.setSuffix(".xml");
		resolver.setCharacterEncoding("UTF-8");
		resolver.setTemplateMode(TemplateMode.XML);
		resolver.setCacheable(config.isProductionEnvironment());
		// resolver.setOrder(30); // Order not needed because resolver on different ViewResolver
		return resolver;
	}

	@Bean 	// WARNING: @Bean annotation needed to make message.properties work properly - DO NOT REMOVE
			//          because the SpringTemplateEngine has to be injected with the MessageSource bean.
	public SpringTemplateEngine templateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setEnableSpringELCompiler(true);
//		engine.addTemplateResolver(xmlTemplateResolver());
//		engine.addTemplateResolver(emailTemplateResolver());
		engine.addTemplateResolver(webTemplateResolver());
		engine.addTemplateResolver(mailPreviewTemplateResolver());
		engine.addTemplateResolver(yadaTemplateResolver());
		// Fixes urls with @{} adding the language path when configured and versioning resource folders
		engine.setLinkBuilder(new YadaLinkBuilder(config, getNotLocalizedResourcePattern()));

		// Do this in the subclass
		//		// http://www.thymeleaf.org/layouts.html
		//		engine.addDialect(new LayoutDialect()); // thymeleaf-layout-dialect
		addExtraDialect(engine); // thymeleaf-SpringSecurity-dialect
		addYadaDialect(engine);
		return engine;
	}

	/**
	 * To be overridden when a new dialect has to be added, e.g. engine.addDialect(new LayoutDialect());
	 * @param engine
	 */
	protected void addExtraDialect(SpringTemplateEngine engine) {
		// Do nothing
	}

	protected void addYadaDialect(SpringTemplateEngine engine) {
		engine.addDialect(new YadaDialect(config));
	}

	@Bean
	public SpringTemplateEngine javascriptTemplateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.addTemplateResolver(javascriptTemplateResolver());
		// Do this in the subclass
		//		engine.addDialect(new LayoutDialect()); // thymeleaf-layout-dialect
		addExtraDialect(engine); // thymeleaf-SpringSecurity-dialect
		addYadaDialect(engine);
		return engine;
	}

	// Ho aggiunto un viewResolver per gestire i file xml. Per usarlo basta che il controller restituisca il nome di un file xml senza estensione che sta in WEB-INF/views/xml
	// prefissandolo con "/xml", per esempio "/xml/sitemap".
	@Bean
	public SpringTemplateEngine xmlTemplateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.addTemplateResolver(xmlTemplateResolver());
		// Do this in the subclass
		//		engine.addDialect(new LayoutDialect()); // thymeleaf-layout-dialect
		addExtraDialect(engine); // thymeleaf-SpringSecurity-dialect
		addYadaDialect(engine);
		return engine;
	}

	/**
	 * View resolver for js files that can be anywhere in the views folder.
	 * Contrary to usual practice, the view name MUST include the .js extension: return "/some/path/myfile.js".
	 * @return
	 */
	@Bean
	public ViewResolver javascriptViewResolver(@Qualifier("javascriptTemplateEngine") SpringTemplateEngine javascriptTemplateEngine) {
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setTemplateEngine(javascriptTemplateEngine);
		viewResolver.setCharacterEncoding("UTF-8"); // Questo è importante anche se nei tutorial non lo mettono
		viewResolver.setOrder(5);
		// This is needed to skip this resolver for all html files but it forces the use of .js in the view name
		viewResolver.setViewNames(new String[] { "*.js" });
		viewResolver.setContentType("application/javascript");
		// Default is "true": caching is enabled. Disable this only for debugging and development.
		viewResolver.setCache(config.isProductionEnvironment());
		return viewResolver;
	}

	/**
	 * View resolver for xml files that are inside the /xml folder.
	 * @return
	 */
	@Bean
	public ViewResolver xmlViewResolver(@Qualifier("xmlTemplateEngine") SpringTemplateEngine xmlTemplateEngine) {
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setTemplateEngine(xmlTemplateEngine);
		viewResolver.setCharacterEncoding("UTF-8"); // Questo è importante anche se nei tutorial non lo mettono
		viewResolver.setOrder(10);
		// Tutti i template devono stare nel folder /xml. Se non si usa un folder specifico, questo viewResolver non viene usato
		viewResolver.setViewNames(new String[] { "/xml/*" }); // E' giusto mettere "*" e non "*.xml" perchè il suffisso viene attaccato grazie al resolver.setSuffix(".xml") di xmlTemplateResolver()
		viewResolver.setContentType("text/xml");
		// Default is "true": caching is enabled. Disable this only for debugging and development.
		viewResolver.setCache(config.isProductionEnvironment());
		return viewResolver;
	}

	/**
	 * View resolver for html pages. It handles web pages, mail preview pages from classpath, yada snippets from classpath
	 * @return
	 */
	@Bean
	public ViewResolver viewResolver(@Qualifier("templateEngine") SpringTemplateEngine templateEngine) {
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		// ATTENTION: do not use templateEngine() here otherwise i18n won't work because the MessageSource is not going to be injected
		viewResolver.setTemplateEngine(templateEngine);
		// viewResolver.setTemplateEngine(templateEngine());
		viewResolver.setCharacterEncoding("UTF-8");
		viewResolver.setContentType("text/html; charset=UTF-8");
		viewResolver.setOrder(20);
		viewResolver.setViewNames(new String[] { "*" });
		// Default is "true": caching is enabled. Disable this only for debugging and development.
		viewResolver.setCache(config.isProductionEnvironment());
		return viewResolver;
	}
}
