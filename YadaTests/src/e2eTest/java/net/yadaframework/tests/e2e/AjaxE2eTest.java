package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * yada.ajax.js links: fragment replacement without page reload, and the notification modal returned by YadaNotify
 */
class AjaxE2eTest extends BrowserTestSupport {

	@Test
	void ajaxLinkReplacesTheTargetFragment() {
		page.navigate("/test/ajax");
		assertThat(page.getByTestId("refreshCount")).hasText("0");
		// A marker that a full page load would lose
		page.evaluate("window.ytsMarker = 'kept'");
		page.getByTestId("refreshLink").click();
		assertThat(page.getByTestId("refreshCount")).not().hasText("0");
		String first = page.getByTestId("refreshCount").textContent();
		page.getByTestId("refreshLink").click();
		assertThat(page.getByTestId("refreshCount")).hasText(String.valueOf(Integer.parseInt(first) + 1));
		assertEquals("kept", page.evaluate("window.ytsMarker"));
		assertThat(page.getByTestId("ajaxTarget")).hasCount(1);
	}

	@Test
	void ajaxNotificationIsShownInAModal() {
		page.navigate("/test/ajax");
		page.evaluate("window.ytsMarker = 'kept'");
		page.getByTestId("notifyLink").click();
		assertThat(page.locator(".modal.show .modal-title")).hasText("Ajax notification");
		assertThat(page.locator(".modal.show .modal-body")).containsText("Hello from the server");
		assertEquals("kept", page.evaluate("window.ytsMarker"));
	}
}
