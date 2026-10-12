package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import net.yadaframework.tests.components.TestDataService;

/**
 * Commerce: YadaMoney prices, a persisted cart and an order with a persistent-enum status
 */
class ShopE2eTest extends BrowserTestSupport {

	private void add(String sku) {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to cart " + sku)).click();
		page.waitForURL(Pattern.compile(".*/test/user/shop$"));
	}

	@Test
	void shopNeedsALoggedInUser() {
		page.navigate("/test/user/shop");
		assertThat(page).hasURL(Pattern.compile(".*/login$"));
	}

	@Test
	void cartTotalAndOrderArePersisted() {
		login(TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD);
		page.navigate("/test/user/shop");
		assertThat(page.getByTestId("shopArticle-SKU-A").getByTestId("price")).hasText("125.00");
		assertThat(page.getByTestId("shopArticle-SKU-C").getByTestId("price")).hasText("89.00");
		add("SKU-A");
		add("SKU-A");
		add("SKU-C");
		assertThat(page.getByTestId("cartItem")).hasText(new String[] { "SKU-A x 2", "SKU-C x 1" });
		assertThat(page.getByTestId("cartTotal")).hasText("339.00");
		assertEquals(1, session.fixtures().countRows("YadaCart"));
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Place order")).click();
		assertThat(page.locator("#yada-notification.show .modal-body")).containsText("Order 1 placed");
		assertThat(page.getByTestId("orderTotal")).hasText("339.00");
		assertThat(page.getByTestId("orderStatus")).hasText("Unpaid");
		assertThat(page.getByTestId("orderItems")).hasText("2");
		assertThat(page.getByTestId("cartItem")).hasCount(0);
		assertEquals(1, session.fixtures().countRows("YadaOrder"));
		assertEquals(2, session.fixtures().countRows("YadaOrderItem"));
		assertEquals(0, session.fixtures().countRows("YadaCart"));
	}
}
