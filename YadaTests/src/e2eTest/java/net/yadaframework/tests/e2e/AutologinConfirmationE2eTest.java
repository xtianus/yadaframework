package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import net.yadaframework.tests.components.TestDataService;

/**
 * The real autologin confirmation page in a browser: opening a link neither logs in nor uses the token and nothing is
 * submitted automatically, the explicit confirmation logs in once and keeps the link fragment, and the session of
 * another logged-in user is preserved.
 */
class AutologinConfirmationE2eTest extends BrowserTestSupport {
	private static final String TARGET = "/test/user/area";

	private String issueUserLinkPathAndQuery() {
		String link = session.application().getBean(TestDataService.class).issueAutologinLink(TestDataService.USER_EMAIL, TARGET);
		URI uri = URI.create(link);
		return uri.getRawPath() + "?" + uri.getRawQuery();
	}

	private long tokenRows() {
		return session.fixtures().countRows("YadaAutoLoginToken");
	}

	private void assertNotLoggedIn() {
		page.navigate("/test/home");
		assertThat(page.getByTestId("loginLink")).isVisible();
	}

	@Test
	void openingTheLinkNeitherLogsInNorUsesTheToken() {
		String link = issueUserLinkPathAndQuery();
		page.navigate(link);
		assertThat(page.getByTestId("autologinAccount")).hasText(TestDataService.USER_EMAIL);
		// Nothing is submitted automatically
		page.waitForTimeout(1500);
		assertThat(page).hasURL(Pattern.compile(".*/autologin/.*"));
		assertEquals(1, tokenRows());
		assertNotLoggedIn();
		assertEquals(1, tokenRows());
	}

	@Test
	void theExplicitConfirmationLogsInOnceAndKeepsTheFragment() {
		String link = issueUserLinkPathAndQuery();
		page.navigate(link + "#details");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
		page.waitForURL(Pattern.compile(".*" + Pattern.quote(TARGET) + "#details$"));
		assertEquals(0, tokenRows());
		page.navigate("/test/home");
		assertThat(page.getByTestId("loggedAs")).hasText("Logged in as " + TestDataService.USER_EMAIL);

		// The used link doesn't offer the confirmation any more, also in a new browser session
		context.clearCookies();
		page.navigate(link);
		assertThat(page.getByTestId("autologinAccount")).hasCount(0);
		assertNotLoggedIn();
	}

	@Test
	void anotherUsersSessionIsPreserved() {
		login(TestDataService.ADMIN_EMAIL, TestDataService.ADMIN_PASSWORD);
		String link = issueUserLinkPathAndQuery();
		page.navigate(link);
		assertThat(page.getByTestId("autologinAccount")).hasCount(0);
		assertTrue(page.url().contains("/test/"), page.url());
		page.navigate("/test/home");
		assertThat(page.getByTestId("loggedAs")).hasText("Logged in as " + TestDataService.ADMIN_EMAIL);
		assertEquals(1, tokenRows());
	}
}
