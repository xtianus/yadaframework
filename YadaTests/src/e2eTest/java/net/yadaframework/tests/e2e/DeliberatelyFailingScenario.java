package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A browser test that always fails. It is excluded from the e2eTest task by its tag and run only by
 * {@link CleanupAfterFailureE2eTest}, which checks what a failure leaves behind.
 */
@Tag(DeliberatelyFailingScenario.TAG)
class DeliberatelyFailingScenario extends BrowserTestSupport {
	static final String TAG = "selfcheck";
	/** The database port of the last run, for the checks done after the class has finished */
	static volatile int lastDatabasePort = -1;

	@Test
	void failsOnPurpose() {
		lastDatabasePort = session.run().databasePort();
		page.navigate("/test/home");
		assertThat(page.getByTestId("title")).hasText("Not the real title", new com.microsoft.playwright.assertions.LocatorAssertions.HasTextOptions().setTimeout(1000));
	}
}
