package net.yadaframework.example.web.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import net.yadaframework.example.support.ExampleWebTestBase;
import net.yadaframework.example.web.UserSession;

/**
 * Impersonation in the example starts and ends only with a CSRF-protected POST, is still limited by
 * userCanImpersonate, and returns only to a local path where it was started.
 */
class ImpersonationSecurityTest extends ExampleWebTestBase {
	private static final String DEIMPERSONATE_URL = "/dashboard/user/deimpersonate";

	private MockHttpSession supervisor;
	private long targetId;

	@BeforeEach
	void cleanState() throws Exception {
		resetDatabase();
		supervisor = login(SUPERVISOR_EMAIL, SUPERVISOR_PASSWORD);
		targetId = findUser(TARGET_EMAIL).getId();
	}

	private void assertLoggedInAs(MockHttpSession session, String email) throws Exception {
		mockMvc.perform(get("/login").session(session)).andExpect(authenticated().withUsername(email));
	}

	private UserSession userSession(MockHttpSession session) {
		return (UserSession) session.getAttribute("scopedTarget.userSession");
	}

	private boolean impersonating(MockHttpSession session) {
		UserSession userSession = userSession(session);
		return userSession != null && userSession.isImpersonationActive();
	}

	/**
	 * Starts impersonating the target with a valid CSRF token
	 * @return the response
	 */
	private MockHttpServletResponse impersonate(String currentLocation) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(post("/dashboard/user/impersonate/" + targetId).session(supervisor)
			.param("_csrf", csrfToken(supervisor))
			.param("currentLocation", currentLocation))
			.andExpect(status().is3xxRedirection())
			.andReturn().getResponse();
		assertLoggedInAs(supervisor, TARGET_EMAIL);
		return response;
	}

	/**
	 * Ends the impersonation with a valid CSRF token
	 * @return the redirect target
	 */
	private String deimpersonate() throws Exception {
		String redirect = mockMvc.perform(post(DEIMPERSONATE_URL).session(supervisor).param("_csrf", csrfToken(supervisor)))
			.andExpect(status().is3xxRedirection())
			.andReturn().getResponse().getRedirectedUrl();
		assertLoggedInAs(supervisor, SUPERVISOR_EMAIL);
		return redirect;
	}

	@Test
	void aGetDoesNotStartImpersonation() throws Exception {
		mockMvc.perform(get("/dashboard/user/impersonate/" + targetId).session(supervisor).param("currentLocation", "https://evil.example/"));
		mockMvc.perform(get("/dashboard/user/impersonate").session(supervisor).param("id", Long.toString(targetId)).param("currentLocation", "https://evil.example/"));
		assertLoggedInAs(supervisor, SUPERVISOR_EMAIL);
		assertFalse(impersonating(supervisor));
	}

	@Test
	void aPostWithoutAValidCsrfTokenDoesNotStartImpersonation() throws Exception {
		mockMvc.perform(post("/dashboard/user/impersonate/" + targetId).session(supervisor)).andExpect(status().isForbidden());
		mockMvc.perform(post("/dashboard/user/impersonate").session(supervisor).param("id", Long.toString(targetId)).param("_csrf", "forged")).andExpect(status().isForbidden());
		assertLoggedInAs(supervisor, SUPERVISOR_EMAIL);
		assertFalse(impersonating(supervisor));
	}

	@Test
	void aGetOrAPostWithoutCsrfDoesNotEndImpersonation() throws Exception {
		impersonate("/dashboard/user");
		mockMvc.perform(get(DEIMPERSONATE_URL).session(supervisor));
		mockMvc.perform(post(DEIMPERSONATE_URL).session(supervisor)).andExpect(status().isForbidden());
		assertLoggedInAs(supervisor, TARGET_EMAIL);
		assertTrue(impersonating(supervisor));
	}

	@Test
	void anAuthorizedPostImpersonatesAndReturnsToTheStartingPath() throws Exception {
		String sessionIdBefore = supervisor.getId();
		impersonate("/en/dashboard/user?page=2");
		assertNotEquals(sessionIdBefore, supervisor.getId(), "The session id was not changed when impersonating");
		assertTrue(impersonating(supervisor));
		assertEquals("/en/dashboard/user?page=2", deimpersonate());
		assertFalse(impersonating(supervisor));
	}

	/**
	 * yada.impersonate sends location.pathname, which includes the context path, and "redirect:" adds the context
	 * path again: the saved location must be relative to the context
	 */
	@Test
	void theStartingPathRoundTripsUnderAContextPath() throws Exception {
		String[][] cases = {
			{ "/ctx/en/dashboard/user?page=2", "/ctx/en/dashboard/user?page=2" },
			{ "/ctx", "/ctx/" },
			{ "/ctx?x=1", "/ctx/?x=1" },
			{ "/ctxother/page", "/ctx/ctxother/page" },
			{ "https://evil.example/", "/ctx/" } };
		for (String[] pair : cases) {
			mockMvc.perform(post("/ctx/dashboard/user/impersonate/" + targetId).contextPath("/ctx").session(supervisor)
				.param("_csrf", csrfToken(supervisor))
				.param("currentLocation", pair[0]))
				.andExpect(status().is3xxRedirection());
			assertLoggedInAs(supervisor, TARGET_EMAIL);
			String redirect = mockMvc.perform(post("/ctx" + DEIMPERSONATE_URL).contextPath("/ctx").session(supervisor).param("_csrf", csrfToken(supervisor)))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getRedirectedUrl();
			assertLoggedInAs(supervisor, SUPERVISOR_EMAIL);
			assertEquals(pair[1], redirect, pair[0]);
		}
	}

	@Test
	void theFormWithTheIdParameterWorksToo() throws Exception {
		mockMvc.perform(post("/dashboard/user/impersonate").session(supervisor)
			.param("_csrf", csrfToken(supervisor))
			.param("id", Long.toString(targetId))
			.param("currentLocation", "/dashboard"))
			.andExpect(status().is3xxRedirection());
		assertLoggedInAs(supervisor, TARGET_EMAIL);
		assertEquals("/dashboard", deimpersonate());
	}

	@Test
	void unsafeStartingLocationsAreNotSaved() throws Exception {
		for (String unsafe : new String[] { "https://evil.example/login", "//evil.example/login", "/\\evil.example", "http://localhost/dashboard", "javascript:alert(1)" }) {
			impersonate(unsafe);
			assertEquals("/", ReflectionTestUtils.getField(userSession(supervisor), "impersonationStartingLocation"), unsafe);
			assertEquals("/", deimpersonate(), unsafe);
		}
	}

	@Test
	void anUnsafeSavedLocationIsNotReplayed() throws Exception {
		impersonate("/dashboard");
		// A value saved by older code or by another caller of YadaSession.impersonate()
		ReflectionTestUtils.setField(userSession(supervisor), "impersonationStartingLocation", "https://evil.example/login");
		assertEquals("/", deimpersonate());
	}

	@Test
	void aMissingStartingLocationReturnsHome() throws Exception {
		mockMvc.perform(post("/dashboard/user/impersonate/" + targetId).session(supervisor).param("_csrf", csrfToken(supervisor)))
			.andExpect(status().is3xxRedirection());
		assertEquals("/", deimpersonate());
	}

	@Test
	void userCanImpersonateIsStillEnforced() throws Exception {
		long adminId = findUser(ADMIN_EMAIL).getId();
		mockMvc.perform(post("/dashboard/user/impersonate/" + adminId).session(supervisor).param("_csrf", csrfToken(supervisor)).param("currentLocation", "/dashboard"))
			.andExpect(status().is3xxRedirection());
		assertLoggedInAs(supervisor, SUPERVISOR_EMAIL);
		assertFalse(impersonating(supervisor));
	}

	@Test
	void theImpersonationBannerEndsItWithAPost() throws Exception {
		impersonate("/dashboard");
		// The timeout page renders the home view with the header; "/" only redirects to the locale path
		String page = mockMvc.perform(get("/timeout").session(supervisor)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		Document document = Jsoup.parse(page);
		Element banner = document.selectFirst(".yadaImpersonationBanner");
		assertNotNull(banner, "No impersonation banner");
		Element form = banner.selectFirst("form[action$=" + DEIMPERSONATE_URL + "]");
		assertNotNull(form, "The banner has no deimpersonate form");
		assertEquals("post", form.attr("method").toLowerCase());
		assertNotNull(form.selectFirst("input[name=_csrf]"), "The deimpersonate form has no CSRF token");
		assertTrue(banner.select("a[href$=" + DEIMPERSONATE_URL + "]").isEmpty(), "The banner still links to the deimpersonate url");
	}
}
