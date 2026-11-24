package net.yadaframework.security.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.util.ReflectionTestUtils;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.TooManyFailedAttemptsException;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.tests.support.LogCapture;

/**
 * A failed login looks the same whether the username exists or not, also when a custom authentication provider
 * passes a UsernameNotFoundException straight to the handler.
 */
class YadaAuthenticationFailureHandlerTest {
	private static final String SUBMITTED_PASSWORD = "Submitted-Secret-4711";

	private YadaAuthenticationFailureHandler handler;
	private YadaUserCredentialsDao yadaUserCredentialsDao;
	private YadaConfiguration yadaConfiguration;

	@BeforeEach
	void createHandler() {
		yadaUserCredentialsDao = mock(YadaUserCredentialsDao.class);
		yadaConfiguration = mock(YadaConfiguration.class);
		handler = new YadaAuthenticationFailureHandler();
		ReflectionTestUtils.setField(handler, "yadaUserCredentialsDao", yadaUserCredentialsDao);
		ReflectionTestUtils.setField(handler, "yadaConfiguration", yadaConfiguration);
		handler.setFailureUrlNormalRequest("/login");
		handler.setFailureUrlAjaxRequest("/ajaxLogin");
	}

	private MockHttpServletRequest loginRequest(String username, boolean ajax) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/loginPost");
		request.setParameter("username", username);
		request.setParameter("password", SUBMITTED_PASSWORD);
		if (ajax) {
			request.addHeader("X-Requested-With", "XMLHttpRequest");
		}
		return request;
	}

	/**
	 * @return the request attributes set by the handler, without the exception itself
	 */
	private static Map<String, Object> visibleAttributes(MockHttpServletRequest request) {
		Map<String, Object> result = new TreeMap<>();
		for (String name : Collections.list(request.getAttributeNames())) {
			if (!WebAttributes.AUTHENTICATION_EXCEPTION.equals(name)) {
				result.put(name, request.getAttribute(name));
			}
		}
		return result;
	}

	private MockHttpServletRequest fail(String username, boolean ajax, AuthenticationException exception, MockHttpServletResponse response) throws Exception {
		MockHttpServletRequest request = loginRequest(username, ajax);
		handler.onAuthenticationFailure(request, response, exception);
		return request;
	}

	private void assertSameOutcome(boolean ajax) throws Exception {
		MockHttpServletResponse wrongPasswordResponse = new MockHttpServletResponse();
		MockHttpServletRequest wrongPassword = fail("someone@yts.test", ajax, new BadCredentialsException("Bad credentials"), wrongPasswordResponse);
		MockHttpServletResponse unknownUserResponse = new MockHttpServletResponse();
		MockHttpServletRequest unknownUser = fail("someone@yts.test", ajax, new UsernameNotFoundException("Username someone@yts.test not found"), unknownUserResponse);

		assertEquals(visibleAttributes(wrongPassword), visibleAttributes(unknownUser));
		assertEquals(wrongPasswordResponse.getForwardedUrl(), unknownUserResponse.getForwardedUrl());
		assertEquals(wrongPasswordResponse.getStatus(), unknownUserResponse.getStatus());
		assertFalse(visibleAttributes(unknownUser).containsValue(SUBMITTED_PASSWORD), "The submitted password is a request attribute");
		assertNull(unknownUser.getAttribute("usernameNotFound"));
		assertNull(unknownUser.getAttribute(YadaAuthenticationFailureHandler.REQUESTATTR_PASSWORD));
		assertEquals("loginErrorFlag", unknownUser.getAttribute(YadaAuthenticationFailureHandler.REQUESTATTR_LOGINERRORFLAG));
	}

	@Test
	void unknownUserLooksLikeWrongPasswordOnHtmlLogin() throws Exception {
		assertSameOutcome(false);
	}

	@Test
	void unknownUserLooksLikeWrongPasswordOnAjaxLogin() throws Exception {
		assertSameOutcome(true);
	}

	@Test
	void errorWithoutFailureUrlHidesTheExceptionMessage() throws Exception {
		handler.setFailureUrlNormalRequest(null);
		MockHttpServletResponse unknownUserResponse = new MockHttpServletResponse();
		fail("someone@yts.test", false, new UsernameNotFoundException("Username someone@yts.test not found"), unknownUserResponse);
		MockHttpServletResponse wrongPasswordResponse = new MockHttpServletResponse();
		fail("someone@yts.test", false, new BadCredentialsException("Bad credentials"), wrongPasswordResponse);

		assertEquals(401, unknownUserResponse.getStatus());
		assertEquals(wrongPasswordResponse.getErrorMessage(), unknownUserResponse.getErrorMessage());
		assertFalse(unknownUserResponse.getErrorMessage().contains("someone@yts.test"), unknownUserResponse.getErrorMessage());
	}

	@Test
	void lockedAccountReportsTheMinutesLeft() throws Exception {
		YadaUserCredentials credentials = new YadaUserCredentials();
		credentials.setLastFailedAttempt(new Date());
		when(yadaUserCredentialsDao.findFirstByUsername("someone@yts.test")).thenReturn(credentials);
		when(yadaConfiguration.getPasswordFailedAttemptsLockoutMinutes()).thenReturn(10);

		MockHttpServletRequest locked = fail("someone@yts.test", false, new LockedException("User account is locked"), new MockHttpServletResponse());
		MockHttpServletRequest legacyLocked = fail("someone@yts.test", false, new TooManyFailedAttemptsException(), new MockHttpServletResponse());

		assertNotNull(locked.getAttribute(YadaAuthenticationFailureHandler.REQUESTATTR_LOCKOUTMINUTES));
		assertEquals(10L, locked.getAttribute(YadaAuthenticationFailureHandler.REQUESTATTR_LOCKOUTMINUTES));
		assertEquals(10L, legacyLocked.getAttribute(YadaAuthenticationFailureHandler.REQUESTATTR_LOCKOUTMINUTES));
	}

	@Test
	void unknownUserCountsAsAFailedAttemptLikeAWrongPassword() throws Exception {
		fail("someone@yts.test", false, new UsernameNotFoundException("not found"), new MockHttpServletResponse());
		verify(yadaUserCredentialsDao).incrementFailedAttempts("someone@yts.test");
	}

	@Test
	void lockoutWithoutUsernameIsHandledWithoutErrors() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/loginPost");
		MockHttpServletResponse response = new MockHttpServletResponse();
		try (LogCapture logs = LogCapture.start()) {
			handler.onAuthenticationFailure(request, response, new LockedException("User account is locked"));
			assertFalse(logs.text().contains("Failed to handle authentication failure"), logs.text());
		}
		verify(yadaUserCredentialsDao, never()).findFirstByUsername(any());
		assertEquals("loginErrorFlag", request.getAttribute(YadaAuthenticationFailureHandler.REQUESTATTR_LOGINERRORFLAG));
		assertEquals("/login", response.getForwardedUrl());
	}
}
