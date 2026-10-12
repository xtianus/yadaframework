package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.FilePayload;

/**
 * Multipart upload through the framework multipart handling, below and above maxFileUploadSizeBytes (100 KiB)
 */
class UploadE2eTest extends BrowserTestSupport {

	private void upload(String fileName, int size) {
		page.navigate("/test/upload");
		page.getByLabel("File").setInputFiles(new FilePayload(fileName, "application/octet-stream", new byte[size]));
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Upload")).click();
	}

	@Test
	void fileWithinTheLimitIsReceived() {
		upload("small.bin", 1000);
		assertThat(page.getByTestId("uploaded")).hasText("Received small.bin (1,000 bytes)"); // Formatted by the message bundle
	}

	@Test
	void fileOverTheLimitIsReportedAndNotRejectedAsForbidden() {
		upload("large.bin", 200 * 1024);
		assertThat(page.getByTestId("tooLarge")).isVisible();
		assertThat(page.getByTestId("uploaded")).hasCount(0);
		assertThat(page.getByTestId("errorStatus")).hasCount(0);
	}
}
