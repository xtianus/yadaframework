package net.yadaframework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;

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
