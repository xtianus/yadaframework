package net.yadaframework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.tests.support.LogCapture;

/**
 * The global exception handler never sends exception text to the client and never renders a view on a committed response.
 */
class YadaGlobalExceptionHandlerTest {
	private static final String HOSTILE = "<script>alert(1)</script>secret-sql-detail";

	private YadaGlobalExceptionHandler handler(boolean ajax) {
		YadaConfiguration configuration = mock(YadaConfiguration.class);
		YadaWebUtil yadaWebUtil = mock(YadaWebUtil.class);
		when(configuration.getErrorPageForward()).thenReturn("/errorPage");
		when(yadaWebUtil.isAjaxRequest(any())).thenReturn(ajax);
		YadaGlobalExceptionHandler handler = new YadaGlobalExceptionHandler();
		ReflectionTestUtils.setField(handler, "config", configuration);
		ReflectionTestUtils.setField(handler, "yadaWebUtil", yadaWebUtil);
		return handler;
	}

	@Test
	void ajaxErrorDescriptionIsGeneric() throws Exception {
		HttpServletResponse response = mock(HttpServletResponse.class);
		ModelAndView mav = handler(true).globalErrorHandler(new MockHttpServletRequest(), response, new IllegalStateException(HOSTILE));
		assertEquals("/yada/ajaxError", mav.getViewName());
		assertEquals("Internal Server Error", mav.getModel().get("errorDescription"));
		assertFalse(mav.getModel().toString().contains("secret-sql-detail"), mav.getModel().toString());
	}

	@Test
	void pageErrorMessageIsGeneric() throws Exception {
		HttpServletResponse response = mock(HttpServletResponse.class);
		ModelAndView mav = handler(false).globalErrorHandler(new MockHttpServletRequest(), response, new IllegalStateException(HOSTILE));
		assertEquals("forward:/errorPage", mav.getViewName());
		assertEquals("Internal Server Error", mav.getModel().get("yadaExceptionMessage"));
		assertFalse(mav.getModel().get("yadaExceptionMessage").toString().contains("secret-sql-detail"));
	}

	/**
	 * The text of a rethrown Spring exception can carry the url of a token link: the logged text is redacted
	 */
	@Test
	void rethrownExceptionTextIsRedacted() throws Exception {
		String secret = "52a3d4f399ee681d15726d58b0efa0f0552a3d4f399ee681d15726d58b0efa0f";
		Exception exception = new NoResourceFoundException(HttpMethod.GET, "autologin/12-" + secret + "/");
		try (LogCapture logs = LogCapture.start()) {
			assertThrows(NoResourceFoundException.class, () -> handler(false).globalErrorHandler(new MockHttpServletRequest(), mock(HttpServletResponse.class), exception));
			String text = logs.text();
			assertTrue(text.contains("Rethrowing ErrorResponse exception"), text);
			assertTrue(text.contains("12-[hidden]"), text);
			assertFalse(text.contains(secret), text);
		}
	}

	@Test
	void committedResponseRendersNoView() throws Exception {
		for (boolean ajax : new boolean[] { false, true }) {
			HttpServletResponse response = mock(HttpServletResponse.class);
			when(response.isCommitted()).thenReturn(true);
			ModelAndView mav = handler(ajax).globalErrorHandler(new MockHttpServletRequest(), response, new IllegalStateException(HOSTILE));
			assertNull(mav);
			verify(response, never()).getWriter();
			verify(response, never()).getOutputStream();
			verify(response, never()).setStatus(org.mockito.ArgumentMatchers.anyInt());
		}
	}
}
