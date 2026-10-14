package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import net.yadaframework.tests.support.LogCapture;

/**
 * The DEBUG line of an oversized multipart request rejected with 413 hides the secret of a token url
 */
class YadaUploadLimitAccessDeniedHandlerLogTest {
	private static final String SECRET = "52a3d4f399ee681d15726d58b0efa0f0552a3d4f399ee681d15726d58b0efa0f";

	@Test
	void rejectedUploadUrlIsRedacted() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/autologin/12-" + SECRET + "/upload");
		request.setAttribute(YadaCommonsMultipartResolver.MAX_UPLOAD_SIZE_EXCEEDED_KEY, Boolean.TRUE);
		MockHttpServletResponse response = new MockHttpServletResponse();
		try (LogCapture logs = LogCapture.start()) {
			new YadaUploadLimitAccessDeniedHandler(mock(AccessDeniedHandler.class)).handle(request, response, new MissingCsrfTokenException(null));
			String text = logs.text();
			assertEquals(413, response.getStatus());
			assertTrue(text.contains("Rejecting oversized multipart request"), text);
			assertFalse(text.contains(SECRET), text);
		}
	}
}
