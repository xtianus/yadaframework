package net.yadaframework.tests.e2e;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;

import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;
import net.yadaframework.tests.support.TestRun;

/**
 * Base class of the browser tests: one application and one Chromium per test class, a fresh browser context and a
 * database reset for every test.
 * <p>
 * Playwright objects are created and closed on the JUnit thread that owns the test class. When a test fails, its
 * screenshot and trace are written to the "yts.e2eArtifactsDir" folder; the backend and native database output is in
 * the test report of the class. Everything is released even when an assertion fails.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(FailureRecorder.class)
public abstract class BrowserTestSupport {
	private static final Logger log = LoggerFactory.getLogger(BrowserTestSupport.class);
	/** Upper limit for any single wait: assertions retry until the condition holds or this expires */
	protected static final double TIMEOUT_MILLIS = 10000;

	protected ApplicationSession session;
	private Playwright playwright;
	private Browser browser;
	protected BrowserContext context;
	protected Page page;
	private Throwable failure;

	@BeforeAll
	void startApplicationAndBrowser() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
		try {
			playwright = Playwright.create();
			browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
		} catch (RuntimeException e) {
			stopApplicationAndBrowser();
			throw e;
		}
	}

	@BeforeEach
	void openContext() {
		failure = null;
		session.fixtures().reset();
		context = browser.newContext(new Browser.NewContextOptions().setBaseURL(stripTrailingSlash(session.application().baseUri().toString())));
		context.setDefaultTimeout(TIMEOUT_MILLIS);
		context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
		page = context.newPage();
	}

	void recordOutcome(Throwable failure) {
		this.failure = failure;
	}

	@AfterEach
	void closeContext(TestInfo testInfo) {
		if (context == null) {
			return; // The setup failed before the context was created
		}
		try {
			if (failure != null) {
				Path folder = artifactsDirectory().resolve(getClass().getSimpleName() + "-" + safeName(testInfo.getDisplayName()));
				Files.createDirectories(folder);
				page.screenshot(new Page.ScreenshotOptions().setPath(folder.resolve("screenshot.png")).setFullPage(true));
				context.tracing().stop(new Tracing.StopOptions().setPath(folder.resolve("trace.zip")));
				log.error("Browser test failed: screenshot and trace in {}", folder);
			} else {
				context.tracing().stop();
			}
		} catch (Exception e) {
			log.error("Can't save the failure artifacts", e);
		} finally {
			context.close();
			context = null;
			page = null;
		}
	}

	@AfterAll
	void stopApplicationAndBrowser() {
		// Each resource is closed even when closing the previous one failed
		RuntimeException failure = null;
		for (AutoCloseable resource : new AutoCloseable[] { browser, playwright, session }) {
			if (resource == null) {
				continue;
			}
			try {
				resource.close();
			} catch (Exception e) {
				if (failure == null) {
					failure = new IllegalStateException("Browser test cleanup failed");
				}
				failure.addSuppressed(e);
			}
		}
		browser = null;
		playwright = null;
		session = null;
		if (failure != null) {
			throw failure;
		}
	}

	/**
	 * @return the folder for screenshots and traces of failed tests
	 */
	public static Path artifactsDirectory() {
		String configured = System.getProperty("yts.e2eArtifactsDir");
		return configured != null ? Path.of(configured) : TestRun.buildDirectory().resolve("e2e-artifacts");
	}

	private static String stripTrailingSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	private static String safeName(String name) {
		return name.replaceAll("[^A-Za-z0-9_-]+", "_");
	}

	/**
	 * Logs in through the real login form
	 */
	protected void login(String email, String password) {
		page.navigate("/login");
		page.getByLabel("Email").fill(email);
		page.getByLabel("Password").fill(password);
		page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
		page.waitForURL(Pattern.compile(".*/test/home$"));
	}
}
