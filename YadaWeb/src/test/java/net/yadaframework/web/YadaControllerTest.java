package net.yadaframework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.components.YadaSecurityUtilStub;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;

/**
 * Verifies that servlet error dispatches preserve their original HTTP status.
 */
class YadaControllerTest {

	/**
	 * Keeps a not-found dispatch at HTTP 404 while rendering the configured error page.
	 */
	@Test
	void yadaError_NotFoundDispatchPreserves404() {
		assertErrorStatusPreserved(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Keeps an internal-error dispatch at HTTP 500 while rendering the configured error page.
	 */
	@Test
	void yadaError_InternalErrorDispatchPreserves500() {
		assertErrorStatusPreserved(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	/**
	 * Invokes the real error controller with one servlet error status and verifies the response.
	 *
	 * @param errorStatus original servlet error status
	 */
	private void assertErrorStatusPreserved(int errorStatus) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
		YadaConfiguration configuration = mock(YadaConfiguration.class);
		YadaWebUtil yadaWebUtil = mock(YadaWebUtil.class);
		YadaSecurityUtilStub yadaSecurityUtil = mock(YadaSecurityUtilStub.class);
		when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(errorStatus);
		when(request.getAttribute("jakarta.servlet.error.message")).thenReturn("");
		when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn("/missing");
		when(configuration.getErrorPageForward()).thenReturn("/errorPage");
		when(yadaSecurityUtil.getUsername()).thenReturn("test-user");

		YadaController controller = new YadaController();
		ReflectionTestUtils.setField(controller, "config", configuration);
		ReflectionTestUtils.setField(controller, "yadaWebUtil", yadaWebUtil);
		ReflectionTestUtils.setField(controller, "yadaSecurityUtil", yadaSecurityUtil);

		String viewName = controller.yadaError(
			request,
			response,
			redirectAttributes,
			new ExtendedModelMap(),
			Locale.ENGLISH
		);

		assertEquals("forward:/errorPage", viewName);
		verify(response).resetBuffer();
		verify(response).setStatus(errorStatus);
	}
}
