package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Server-side Bean Validation of a form, and the notification shown after a successful submission
 */
class FormE2eTest extends BrowserTestSupport {

	private void submit(String name, String email) {
		page.navigate("/test/form");
		page.getByLabel("Name").fill(name);
		page.getByLabel("Email").fill(email);
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Send")).click();
	}

	@Test
	void emptyFieldsAreRejected() {
		submit("", "");
		assertThat(page.getByTestId("nameError")).isVisible();
		assertThat(page.getByTestId("emailError")).isVisible();
		assertThat(page.getByTestId("saved")).hasCount(0);
	}

	@Test
	void invalidEmailIsRejectedAndInputKept() {
		submit("Ann", "not-an-email");
		assertThat(page.getByTestId("emailError")).isVisible();
		assertThat(page.getByTestId("nameError")).hasCount(0);
		assertThat(page.getByLabel("Name")).hasValue("Ann");
	}

	@Test
	void validFormIsAcceptedWithNotification() {
		submit("Ann", "ann@example.com");
		assertThat(page.getByTestId("saved")).hasText("Contact saved for Ann");
		assertThat(page.locator("#yada-notification.show .modal-body")).containsText("Contact saved for Ann");
	}
}
