package net.yadaframework.tests.support;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;

import net.yadaframework.components.YadaMariaDBServer;
import net.yadaframework.core.YadaEmbeddedTomcatTestServer;
import net.yadaframework.core.YadaTestConfigurationScope;

/**
 * The YadaTests application running in an embedded Tomcat on the database of one {@link TestRun}.
 * <p>
 * The application owns its configuration scope, its Tomcat and the embedded database process it started.
 * {@link #close()} releases them in order even after a failure: HTTP connector and contexts (with the
 * EntityManagerFactory and the pool), then the owned database, then the configuration scope, which also
 * restores the static application context. Run files are deleted by the {@link TestRun}, only after the port is free.
 */
public final class TestApplication implements AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(TestApplication.class);
	/** The initializer of the YadaTests application, in the main source set that the harness doesn't compile against */
	public static final String APPLICATION_INITIALIZER = "net.yadaframework.tests.core.WebApplicationInitializer";
	/** Installs the Spring Security filters, as it does when found in the YadaWebSecurity jar of a deployed application */
	public static final String SECURITY_INITIALIZER = "net.yadaframework.security.SecurityWebApplicationInitializer";
	private static final int STARTUP_ATTEMPTS = 3;

	private final TestRun run;
	private final YadaTestConfigurationScope configurationScope;
	private final YadaEmbeddedTomcatTestServer server;
	private boolean closed = false;

	private TestApplication(TestRun run, YadaTestConfigurationScope configurationScope, YadaEmbeddedTomcatTestServer server) {
		this.run = run;
		this.configurationScope = configurationScope;
		this.server = server;
	}

	/**
	 * Starts the YadaTests application on the run database. When the database port has been taken by someone else
	 * in the meantime, the run gets a new port and database folder and the startup is retried.
	 * @param run the run that provides configuration, database port and folders
	 * @return the running application
	 * @throws IllegalStateException when the application can't start; nothing is left running
	 */
	public static TestApplication start(TestRun run) {
		for (int attempt = 1; ; attempt++) {
			try {
				return startOnce(run);
			} catch (DatabasePortTakenException e) {
				if (attempt >= STARTUP_ATTEMPTS) {
					throw e;
				}
				log.warn("Database port {} taken by another process: retrying with a new allocation", run.databasePort());
				run.renewDatabaseAllocation();
			}
		}
	}

	private static TestApplication startOnce(TestRun run) {
		if (!OwnedDatabases.registered().isEmpty()) {
			throw new IllegalStateException("Another test application or database is still registered: close it first");
		}
		assertMariaDB4jPresent();
		YadaTestConfigurationScope configurationScope = new YadaTestConfigurationScope(run.configurationFile());
		YadaEmbeddedTomcatTestServer server = null;
		try {
			server = new YadaEmbeddedTomcatTestServer("", webappDirectory(), applicationClasspathRoots());
			server.setBaseDirectory(run.directory().resolve("tomcat"));
			server.setInitializerClassNames(List.of(SECURITY_INITIALIZER, APPLICATION_INITIALIZER));
			server.setDelegate(true);
			server.addContextParameter("globalInitializerClasses", EmbeddedDatabaseOwnership.class.getName());
			server.start();
			TestApplication application = new TestApplication(run, configurationScope, server);
			application.checkDatabaseCaptured();
			return application;
		} catch (Exception e) {
			// Our database never got to run but something answers on its port: the port was taken in the meantime
			boolean portTaken = OwnedDatabases.registered().isEmpty() && TestRun.isListening(run.databasePort());
			RuntimeException failure = e instanceof RuntimeException runtimeException ? runtimeException : new IllegalStateException("The test application failed to start", e);
			releaseAll(server, configurationScope, failure);
			throw portTaken ? new DatabasePortTakenException(run.databasePort(), failure) : failure;
		}
	}

	/**
	 * The only startup failure that is retried with a new port
	 */
	private static class DatabasePortTakenException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		DatabasePortTakenException(int port, Throwable cause) {
			super("The database port " + port + " was taken by another process", cause);
		}
	}

	/**
	 * Without MariaDB4j YadaJpaConfig would silently connect to the configured url instead of the embedded database
	 */
	private static void assertMariaDB4jPresent() {
		try {
			Class.forName("ch.vorburger.mariadb4j.DB");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("MariaDB4j is not on the classpath: refusing to start without the embedded database", e);
		}
	}

	private void checkDatabaseCaptured() {
		List<YadaMariaDBServer> databases = OwnedDatabases.registered();
		if (databases.size() != 1) {
			throw new IllegalStateException("Expected exactly one embedded database component, found " + databases.size());
		}
		Integer port = databases.get(0).getPort();
		if (port == null || port != run.databasePort()) {
			throw new IllegalStateException("The embedded database runs on port " + port + " instead of the run port " + run.databasePort());
		}
	}

	/**
	 * @return the src/main/webapp folder of YadaTests
	 */
	static Path webappDirectory() {
		Path projectDir = Path.of(System.getProperty("yts.projectDir", ".")).toAbsolutePath().normalize();
		Path webapp = projectDir.resolve("src/main/webapp");
		if (!Files.isDirectory(webapp.resolve("WEB-INF"))) {
			throw new IllegalStateException("No webapp found in " + webapp + ": set the yts.projectDir system property to the YadaTests folder");
		}
		return webapp;
	}

	/**
	 * The compiled classes and resources of the application, mounted on WEB-INF/classes.
	 * The webapp folder has no "build" ancestor, so they can't be inferred and are passed explicitly.
	 */
	static List<Path> applicationClasspathRoots() {
		Path buildDir = TestRun.buildDirectory();
		List<Path> roots = new ArrayList<>();
		for (Path root : List.of(buildDir.resolve("classes/java/main"), buildDir.resolve("resources/main"))) {
			if (!Files.isDirectory(root)) {
				throw new IllegalStateException("Application output " + root + " not found: build the YadaTests main source set first");
			}
			roots.add(root);
		}
		return roots;
	}

	/**
	 * @return the base URI of the application, with a trailing slash
	 */
	public URI baseUri() {
		return server.getBaseUri();
	}

	/**
	 * @param path an application path, e.g. "/test/home"
	 * @return the absolute URI of the path
	 */
	public URI resolve(String path) {
		return server.resolve(path);
	}

	/**
	 * @return the root application context, for in-process access to application services
	 */
	public WebApplicationContext rootContext() {
		return server.getRootApplicationContext();
	}

	/**
	 * @param type the bean type
	 * @return the bean of the given type from the root application context
	 */
	public <T> T getBean(Class<T> type) {
		return rootContext().getBean(type);
	}

	/** @return the run this application uses */
	public TestRun run() {
		return run;
	}

	/**
	 * Stops the application and its owned database, then restores the static configuration.
	 * Continues past individual failures and reports them together. After a failure the application
	 * is not marked as closed, so that close() can be called again.
	 * @throws IllegalStateException when some resource could not be released
	 */
	@Override
	public synchronized void close() {
		if (closed) {
			return;
		}
		IllegalStateException failure = new IllegalStateException("The test application did not shut down cleanly");
		releaseAll(server, configurationScope, failure);
		if (TestRun.isListening(run.databasePort())) {
			failure.addSuppressed(new IllegalStateException("The database port " + run.databasePort() + " is still in use"));
		}
		if (failure.getSuppressed().length > 0) {
			throw failure;
		}
		closed = true;
	}

	private static void releaseAll(YadaEmbeddedTomcatTestServer server, YadaTestConfigurationScope configurationScope, Throwable failure) {
		if (server != null) {
			try {
				server.close();
			} catch (Exception e) {
				failure.addSuppressed(e);
			}
		}
		try {
			OwnedDatabases.stopAll();
		} catch (RuntimeException e) {
			failure.addSuppressed(e);
		}
		try {
			configurationScope.close();
		} catch (RuntimeException e) {
			failure.addSuppressed(e);
		}
	}

	@Override
	public String toString() {
		return "TestApplication at " + (closed ? "(closed)" : server.getBaseUri()) + " on " + run;
	}
}
