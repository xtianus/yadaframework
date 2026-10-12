package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import net.yadaframework.tests.components.TestDataService;

/**
 * Login, logout, role-based access and CSRF protection through Spring Security as configured by YadaSecurityConfig
 */
class AuthenticationE2eTest extends BrowserTestSupport {

	@Test
	void loginAndLogout() {
		login(TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD);
		assertThat(page.getByTestId("loggedAs")).hasText("Logged in as " + TestDataService.USER_EMAIL);
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log out")).click();
		assertThat(page.getByTestId("loginLink")).isVisible();
		assertThat(page.getByTestId("loggedAs")).hasCount(0);
	}

	@Test
	void wrongPasswordShowsTheLoginError() {
		page.navigate("/login");
		page.getByLabel("Email").fill(TestDataService.USER_EMAIL);
		page.getByLabel("Password").fill("not-the-password");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
		assertThat(page.getByTestId("loginError")).isVisible();
		// The username is kept in the form by the failure handler
		assertThat(page.getByLabel("Email")).hasValue(TestDataService.USER_EMAIL);
	}

	@Test
	void anonymousUserIsSentToLogin() {
		page.navigate("/test/user/area");
		assertThat(page).hasURL(Pattern.compile(".*/login$"));
		assertThat(page.getByTestId("loginForm")).isVisible();
	}

	@Test
	void userIsAuthorizedForUserAreaButForbiddenInAdminArea() {
		login(TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD);
		page.navigate("/test/user/area");
		assertThat(page.getByTestId("area")).hasText("User area");
		page.navigate("/test/admin/area");
		assertThat(page.getByTestId("errorStatus")).hasText("403");
		assertThat(page.getByTestId("area")).hasCount(0);
	}

	@Test
	void adminIsAuthorizedForAdminArea() {
		login(TestDataService.ADMIN_EMAIL, TestDataService.ADMIN_PASSWORD);
		page.navigate("/test/admin/area");
		assertThat(page.getByTestId("area")).hasText("Admin area");
	}

	@Test
	void formWithCsrfTokenIsAccepted() {
		page.navigate("/test/home");
		// The token is added to th:action forms by Spring Security
		assertThat(page.locator("[data-testid=csrfForm] input[type=hidden][name=_csrf]")).hasCount(1);
		page.getByTestId("csrfForm").getByRole(AriaRole.BUTTON).click();
		assertThat(page.locator("body")).hasText("ECHO:hello");
	}

	@Test
	void formWithoutCsrfTokenIsRejected() {
		page.navigate("/test/home");
		page.locator("[data-testid=csrfForm] input[name=_csrf]").evaluate("input => input.remove()");
		page.getByTestId("csrfForm").getByRole(AriaRole.BUTTON).click();
		assertThat(page.getByTestId("errorStatus")).hasText("403");
	}
}
