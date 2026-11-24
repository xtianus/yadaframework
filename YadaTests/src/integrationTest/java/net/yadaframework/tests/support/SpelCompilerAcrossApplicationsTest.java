package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Compiled SpEL expressions keep working after an earlier test application has been closed in the same JVM.
 * <p>
 * Thymeleaf's SPELVariableExpressionEvaluator creates its compiling parser once per class, with the context class
 * loader of the thread that first uses it. The test applications load Thymeleaf from the shared test class path,
 * so that loader must not be the web application class loader of an application that is closed afterwards: Spring's
 * SpelCompiler defines the compiled expressions in a child of it, which can't resolve CompiledExpression once Tomcat
 * has stopped it, and the page fails with NoClassDefFoundError.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpelCompilerAcrossApplicationsTest {
	private static final String THYMELEAF_EVALUATOR = "org.thymeleaf.spring6.expression.SPELVariableExpressionEvaluator";
	/** More than the evaluations after which Spring compiles an expression in MIXED mode (100) */
	private static final int REQUESTS_TO_COMPILE = 110;

	@Test
	@Order(1)
	void thymeleafCompilerClassLoaderOutlivesTheApplication() throws Exception {
		try (TestRun run = TestRun.create()) {
			try (TestApplication application = TestApplication.start(run)) {
				assertEquals(200, get(application, "/test/home").statusCode());
			}
		}
		ClassLoader compilerClassLoader = thymeleafCompilerClassLoader();
		assertNotNull(compilerClassLoader);
		// SpelCompiler defines compiled expressions in a new child loader, e.g. after 100 classes or when its cache entry was collected
		try (URLClassLoader newChildLoader = new URLClassLoader(new URL[0], compilerClassLoader)) {
			assertDoesNotThrow(() -> Class.forName(CompiledExpression.class.getName(), false, newChildLoader), "Thymeleaf compiles SpEL against " + describe(compilerClassLoader) + ", which a closed application has stopped: see YadaEmbeddedTomcatTestServer.initializeSharedSpelCompilerLoader");
		}
	}

	@Test
	@Order(2)
	void compiledExpressionsWorkInALaterApplication() throws Exception {
		try (TestRun run = TestRun.create(); TestApplication application = TestApplication.start(run)) {
			for (int count = 1; count <= REQUESTS_TO_COMPILE; count++) {
				HttpResponse<String> response = get(application, "/test/home");
				assertEquals(200, response.statusCode(), "Request " + count + ": " + response.body());
			}
		}
	}

	private static HttpResponse<String> get(TestApplication application, String path) throws Exception {
		// A new client per request: a reused keep-alive connection may have just been closed by Tomcat
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
		return client.send(HttpRequest.newBuilder(application.resolve(path)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private static String describe(ClassLoader classLoader) {
		return classLoader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(classLoader));
	}

	/**
	 * Reads private fields of Thymeleaf and Spring on purpose: no public API exposes the loader, and a library
	 * upgrade that renames them makes this test fail loudly rather than pass silently.
	 * @return the class loader that Thymeleaf's static compiling SpEL parser passes to SpelCompiler
	 */
	private static ClassLoader thymeleafCompilerClassLoader() throws ReflectiveOperationException {
		Class<?> evaluatorClass = Class.forName(THYMELEAF_EVALUATOR, false, SpelCompilerAcrossApplicationsTest.class.getClassLoader());
		Field parserField = evaluatorClass.getDeclaredField("PARSER_WITH_COMPILED_SPEL");
		parserField.setAccessible(true);
		SpelExpressionParser parser = (SpelExpressionParser) parserField.get(null);
		Field configurationField = SpelExpressionParser.class.getDeclaredField("configuration");
		configurationField.setAccessible(true);
		return ((SpelParserConfiguration) configurationField.get(parser)).getCompilerClassLoader();
	}
}
