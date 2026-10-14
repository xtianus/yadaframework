package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Route;

/**
 * yada.impersonate with POST sends the local path of the page as currentLocation, and a yada.dataTableCrud extra
 * button with ajax false and method POST navigates with a POST (see ImpersonationScenarioController).
 */
class ImpersonationE2eTest extends BrowserTestSupport {
	/** DataTables is a vendor library that the test application doesn't serve: the copy in the example is used */
	private static final Path DATATABLES = Path.of(System.getProperty("yts.projectDir", ".")).toAbsolutePath().normalize()
		.resolveSibling("YadaExamples/src/main/webapp/static/datatables-2.1.8/datatables.min.js");

	/** The target page, reached with or without a query string */
	private static final Pattern START_URL = Pattern.compile(".*/test/impersonation/start(\\?.*)?$");

	@BeforeEach
	void serveDataTables() {
		if (!Files.isRegularFile(DATATABLES)) {
			throw new IllegalStateException("DataTables not found at " + DATATABLES);
		}
		page.route("**/test/impersonation/vendor/datatables.min.js", route -> route.fulfill(new Route.FulfillOptions().setPath(DATATABLES).setContentType("application/javascript")));
	}

	@Test
	void impersonateSendsTheLocalPathWithAPost() {
		page.navigate("/test/impersonation?tab=2");
		page.getByTestId("impersonateButton").click();
		page.waitForURL(START_URL);
		assertThat(page.getByTestId("method")).hasText("POST");
		assertThat(page.getByTestId("id")).hasText("7");
		assertThat(page.getByTestId("currentLocation")).hasText("/test/impersonation?tab=2");
	}

	@Test
	void legacyTableButtonWithoutAjaxSubmitsAPost() {
		page.navigate("/test/impersonation");
		page.getByTestId("legacyImpersonate").click();
		page.waitForURL(START_URL);
		assertThat(page.getByTestId("method")).hasText("POST");
		assertThat(page.getByTestId("id")).hasText("7");
	}
}
