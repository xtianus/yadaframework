package net.yadaframework.tests.e2e;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Tells {@link BrowserTestSupport} whether the test that just ran failed, before its @AfterEach methods run,
 * so that failure artifacts are written only when needed.
 */
public class FailureRecorder implements AfterTestExecutionCallback {

	@Override
	public void afterTestExecution(ExtensionContext context) {
		Object testInstance = context.getRequiredTestInstance();
		if (testInstance instanceof BrowserTestSupport browserTest) {
			browserTest.recordOutcome(context.getExecutionException().orElse(null));
		}
	}
}
