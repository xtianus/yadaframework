package net.yadaframework.core;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import net.yadaframework.web.YadaDateFormatter;
import net.yadaframework.web.dialect.YadaDialect;

@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = { "net.yadaframework.web" })
public class YadaWebConfig extends WebMvcConfigurerAdapter {
	//	private final static long MB = 1024*1024;
//	private final static long MAXFILESIZE = 10*MB;
	private final transient Logger log = LoggerFactory.getLogger(getClass());


	private final static String STATIC_RESOURCES_FOLDER = "/res";
	private final static String STATIC_YADARESOURCES_FOLDER = "/yadares";
	private final static String STATIC_FILE_FOLDER = "/static"; // Ci vanno i file per i quali serve una url univoca immutabile
	
	@Autowired protected YadaConfiguration config;
	
	@Autowired protected ApplicationContext applicationContext;
	
	// @Autowired YadaGlobalAttributesInterceptor globalAttributesInterceptor;


	// Questo metodo non viene mai chiamato!!! Quindi rinuncio.
//	@Override
//	public void addFormatters(FormatterRegistry registry) {
//		registry.addConverter(new YadaStringToEntityConverter());
//	}
	
	public static String getResourceFolder() {
		return STATIC_RESOURCES_FOLDER;
	}
	
	// Questo permette di usare Pageable nei Controller (Spring Data)
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	    PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
	    argumentResolvers.add(resolver);	
	}
	
// NON VAAAAAAAAAAAAAAAAAAAAAAAAA!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!	
//	@Bean
//    MultipartConfigElement multipartConfigElement() {
//		return new MultipartConfigElement(null, MAXFILESIZE, MAXFILESIZE, 0);
//    }
//	
//	@Bean StandardServletMultipartResolver standardServletMultipartResolver() {
//		return new StandardServletMultipartResolver();
//	}

//	@Bean(name="filterMultipartResolver")
//	CommonsMultipartResolver filterMultipartResolver() {
//		CommonsMultipartResolver filterMultipartResolver = new CommonsMultipartResolver();
//		filterMultipartResolver.setMaxUploadSize(MAXFILESIZE);
//		return filterMultipartResolver;
//	}
	
//	@Override
//	public void addInterceptors(InterceptorRegistry registry) {
//		//registry.addInterceptor(globalAttributesInterceptor);
//		// registry.addInterceptor(new ThemeInterceptor()).addPathPatterns("/").excludePathPatterns("/admin/");
//	}
	
//	@Autowired Environment env;

	//
	// Locale handling
	// See http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#mvc-localeresolver
	//
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
		localeChangeInterceptor.setParamName("lang");
		return localeChangeInterceptor;
	}

	@Bean(name = "localeResolver")
	public LocaleResolver localeResolver() {
		CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
		cookieLocaleResolver.setCookieMaxAge(Integer.MAX_VALUE);
		// TODO the default locale should be determined using 'accept-language' header or IP localization
		cookieLocaleResolver.setDefaultLocale(Locale.ENGLISH);
		return cookieLocaleResolver;
	}
	
	@Bean
	@Autowired // L'ho spostato qui per risolvere il problema "Requested bean is currently in creation"
	// Questo registra un Date Formatter
	public FormattingConversionService mvcConversionService(YadaDateFormatter yadaDateFormatter) {
		FormattingConversionServiceFactoryBean result = new FormattingConversionServiceFactoryBean();
		Set<Formatter<Date>> formatters = new HashSet<Formatter<Date>>();
		formatters.add(yadaDateFormatter);
		result.setFormatters(formatters);
		result.afterPropertiesSet();
		return result.getObject();
	}

//	@Bean MessageSource messageSource() {
//		// FIXME Tratto la configurazione come se fosse un resource bundle per accedere via thymeleaf (non so come fare altrimenti per ora)
//		ResourceBundleMessageSource result = new ResourceBundleMessageSource();
//		Properties prop = new Properties();
//		prop.setProperty("res.version", env.getProperty("res.version"));
//		result.setCommonMessages(prop);
//		// http://forum.thymeleaf.org/Access-Spring-properties-td4025970.html
//		// Perhaps it's useful to add a prefix to the keys
//		// messageSource.setCommonMessages(furtherProperties); 
//		return result;
//	}
	
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
		String res = STATIC_RESOURCES_FOLDER;
		if (res.endsWith("/")) {
			res = StringUtils.chop(res); // Remove last character
		}
		registry.addResourceHandler(res + "-" + config.getApplicationBuild() + "/**").addResourceLocations(res+"/").setCachePeriod(31556926); // 1 year cache period
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
		registry.addResourceHandler(yadares + "-" + config.getYadaVersion() + "/**").addResourceLocations("classpath:" + YadaConstants.YADA_VIEW_PREFIX+"/yada/").setCachePeriod(31556926); // 1 year cache period
		
		// Se dobbiamo gestire anche i contents su filesystem, lo facciamo
		String contentUrl = config.getContentUrl();
		if (StringUtils.isNotEmpty(contentUrl) && contentUrl.startsWith("/")) {
			registry.addResourceHandler(contentUrl + "/**").addResourceLocations("file:"+config.getContentPath() + "/"); // Niente cache period
		} // Altrimenti le url sono di tipo http e le gestisce qualcun altro
		
		// robots.txt is usually added by the deploy script depending on the environment
		registry.addResourceHandler("/robots.txt").addResourceLocations("/").setCachePeriod(86400); // 1 day cache period
	}
	
	//
	// Thymeleaf
	//

	@Bean
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
		patterns.add("/yada/*"); // Start with "yada"
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
	public ITemplateResolver mailPreviewTemplateResolver() {
//		ServletContextTemplateResolver resolver = new ServletContextTemplateResolver();
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
	    resolver.setApplicationContext(applicationContext);
//		resolver.setPrefix("/WEB-INF/classes/" + YadaConstants.EMAIL_TEMPLATES_PREFIX);
		resolver.setPrefix("/WEB-INF/classes/" + YadaConstants.EMAIL_TEMPLATES_PREFIX + "/");
		/* From the tutorial:
		 When several template resolvers are applied, it is recommended to specify patterns 
		 for each template resolver so that Thymeleaf can quickly discard those template resolvers 
		 that are not meant to resolve the template, enhancing performance. Doing this is not a 
		 requirement, but a recommendation
		 */
		Set<String> patterns = new HashSet<>();
		patterns.add("/email/*"); // Start with "/email/"
		resolver.setResolvablePatterns(patterns);
		resolver.setSuffix(".html");
		resolver.setCharacterEncoding("UTF-8");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(config.isProductionEnvironment());
		resolver.setOrder(20);
		return resolver;
	}
	
	// x213
	
	@Bean
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
	
	@Bean
	public ITemplateResolver xmlTemplateResolver() {
//		ServletContextTemplateResolver resolver = new ServletContextTemplateResolver();
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
	    resolver.setApplicationContext(applicationContext);
		// Relative paths never work, with or without trailing slash, so better to be consistent without and always use "absolute" paths [xtian]
		resolver.setPrefix("/WEB-INF/views");
//		resolver.setPrefix("/WEB-INF/views/");
		Set<String> patterns = new HashSet<>();
		patterns.add("/xml/*"); // Start with "xml"
		resolver.setResolvablePatterns(patterns);
		resolver.setSuffix(".xml");
		resolver.setCharacterEncoding("UTF-8");
		// NB, selecting HTML5 as the template mode.
		resolver.setTemplateMode("XML");
		resolver.setCacheable(config.isProductionEnvironment());
		// resolver.setOrder(30); // Order not needed because resolver on different ViewResolver
		return resolver;
	}
	
	
	public ClassLoaderTemplateResolver emailTemplateResolver() {
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		// Relative paths never work, with or without trailing slash, so better to be consistent without and always use "absolute" paths [xtian]
		resolver.setPrefix(YadaConstants.EMAIL_TEMPLATES_PREFIX); // Attenzione allo slash finale!
//		resolver.setPrefix(YadaConstants.EMAIL_TEMPLATES_PREFIX + "/"); // Attenzione allo slash finale!
		Set<String> patterns = new HashSet<>();
		patterns.add("/email/*"); // Start with "email"
		resolver.setResolvablePatterns(patterns);
		resolver.setSuffix(".html");
		resolver.setCharacterEncoding("UTF-8");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(config.isProductionEnvironment());
		// resolver.setOrder(40); // Order not needed because resolver on different SpringTemplateEngine
		return resolver;
	}
	
	@Bean
	public SpringTemplateEngine templateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setEnableSpringELCompiler(true);
//		engine.addTemplateResolver(xmlTemplateResolver());
//		engine.addTemplateResolver(emailTemplateResolver());
		engine.addTemplateResolver(webTemplateResolver());
		engine.addTemplateResolver(mailPreviewTemplateResolver());
		engine.addTemplateResolver(yadaTemplateResolver());
		// Do this in the subclass
		//		// http://www.thymeleaf.org/layouts.html
		//		engine.addDialect(new LayoutDialect()); // thymeleaf-layout-dialect
		addExtraDialect(engine); // thymeleaf-SpringSecurity-dialect
		addYadaDialect(engine);
		return engine;
	}
	
	@Bean
	public SpringTemplateEngine emailTemplateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setEnableSpringELCompiler(true);
		engine.addTemplateResolver(emailTemplateResolver());
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

	// Ho aggiunto un viewResolver per gestire i file xml. Per usarlo basta che il controller restituisca il nome di un file xml senza estensione che sta in WEB-INF/views/xml
	// prefissandolo con "/xml", per esempio "/xml/sitemap".
	
	// Non need for a @Bean
	public SpringTemplateEngine xmlTemplateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.addTemplateResolver(xmlTemplateResolver());
		// Do this in the subclass
		//		engine.addDialect(new LayoutDialect()); // thymeleaf-layout-dialect
		addExtraDialect(engine); // thymeleaf-SpringSecurity-dialect
		addYadaDialect(engine);
		return engine;
	}

	@Bean
	public ViewResolver xmlViewResolver() {
		SpringTemplateEngine engine = xmlTemplateEngine();
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setTemplateEngine(engine);
		viewResolver.setCharacterEncoding("UTF-8"); // Questo è importante anche se nei tutorial non lo mettono
		viewResolver.setOrder(10);
		// Tutti i template devono stare nel folder /xml. Se non si usa un folder specifico, questo viewResolver non viene usato
		viewResolver.setViewNames(new String[] { "/xml/*" }); // E' giusto mettere "*" e non "*.xml" perchè il suffisso viene attaccato grazie al resolver.setSuffix(".xml") di xmlTemplateResolver()
		viewResolver.setContentType("text/xml");
		// Default is "true": caching is enabled. Disable this only for debugging and development.
		viewResolver.setCache(config.isProductionEnvironment()); 
		return viewResolver;
	}
	
	@Bean
	public ViewResolver viewResolver() {
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setTemplateEngine(templateEngine());
		viewResolver.setCharacterEncoding("UTF-8"); // Questo è importante anche se nei tutorial non lo mettono
		viewResolver.setOrder(20);
		viewResolver.setViewNames(new String[] { "*" });
		// Default is "true": caching is enabled. Disable this only for debugging and development.
		viewResolver.setCache(config.isProductionEnvironment()); 
		return viewResolver;
	}
}
