package net.yadaframework.example.web.dashboard;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import net.yadaframework.example.support.ExampleWebTestBase;

/**
 * Deleting users in the example dashboard needs a CSRF-protected POST: a GET (a link, an image, a prefetch) or a POST
 * without a valid token deletes nothing.
 */
class UserDeleteSecurityTest extends ExampleWebTestBase {
	private static final String DELETE_URL = "/dashboard/userwrite/ajaxDeleteUserProfile";

	private MockHttpSession admin;
	private String targetId;

	@BeforeEach
	void cleanState() throws Exception {
		resetDatabase();
		admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
		targetId = findUser(TARGET_EMAIL).getId().toString();
	}

	@Test
	void aGetDoesNotDelete() throws Exception {
		mockMvc.perform(get(DELETE_URL).session(admin).param("id", targetId)).andExpect(status().isMethodNotAllowed());
		assertNotNull(findUser(TARGET_EMAIL), "A GET deleted the user");
	}

	@Test
	void aPostWithoutAValidCsrfTokenDoesNotDelete() throws Exception {
		mockMvc.perform(post(DELETE_URL).session(admin).param("id", targetId)).andExpect(status().isForbidden());
		mockMvc.perform(post(DELETE_URL).session(admin).param("id", targetId).param("_csrf", "forged")).andExpect(status().isForbidden());
		assertNotNull(findUser(TARGET_EMAIL), "A POST without a valid CSRF token deleted the user");
	}

	@Test
	void aPostWithTheCsrfTokenDeletes() throws Exception {
		mockMvc.perform(post(DELETE_URL).session(admin).param("id", targetId).param("_csrf", csrfToken(admin)).header("X-Requested-With", "XMLHttpRequest"))
			.andExpect(status().isOk());
		assertNull(findUser(TARGET_EMAIL), "The user was not deleted");
	}

	@Test
	void theDeleteButtonsSendAPost() throws Exception {
		String users = mockMvc.perform(get("/dashboard/user").session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		// The button configuration of the "userTable" is serialized as json in the page
		assertTrue(Pattern.compile("ajaxDeleteUserProfile\\}\"[^{]*\"method\":\"POST\"").matcher(users).find(), "The delete button of the user table doesn't send a POST");
		String legacy = mockMvc.perform(get("/dashboard/user/legacy").session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString().replace("\\/", "/"); // Javascript inlining escapes the slashes
		assertTrue(legacy.contains("/dashboard/userwrite/ajaxDeleteUserProfile\";"), "The legacy table doesn't use the delete url");
		assertTrue(Pattern.compile("\"url\": userDeleteUrl,[^}]*\"method\": \"POST\"").matcher(legacy).find(), "The delete button of the legacy table doesn't send a POST");
	}
}
