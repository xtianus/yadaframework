package net.yadaframework.example.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.example.support.ExampleWebTestBase;

/**
 * The registration and password recovery endpoints of the example bind only the permitted fields: forged ids, tokens,
 * types, users and timestamps never reach a stored request, an existing request can't be turned into a request for
 * another account, and the example subclass fields (name, surname) still work from form to confirmed user.
 */
class RegistrationControllerAuthBindingTest extends ExampleWebTestBase {
	private static final String NEW_EMAIL = "newcomer@example.test";
	private static final String NEW_PASSWORD = "newcomer-password";
	private static final int TYPE_REGISTRATION = 0;
	private static final int TYPE_PASSWORD_RECOVERY = 1;

	@Autowired private DataSource dataSource;
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanState() throws Exception {
		resetDatabase();
		jdbc = new JdbcTemplate(dataSource);
	}

	/**
	 * Adds the parameters an attacker would forge to a request
	 */
	private static MockHttpServletRequestBuilder withForgedFields(MockHttpServletRequestBuilder request, long existingId) {
		return request.param("id", Long.toString(existingId))
			.param("token", "1234")
			.param("tokenHash", "0".repeat(64))
			.param("timestamp", "2000-01-01")
			.param("expiration", "2100-01-01")
			.param("version", "0")
			.param("registrationType", "PASSWORD_RECOVERY")
			.param("yadaUserCredentials.id", "1")
			.param("trattamentoDati.id", "1");
	}

	/**
	 * @return the path of an emailed link, without the locale segment that MockMvc can't forward
	 */
	private static String localPath(String link) {
		String path = URI.create(link).getRawPath();
		return path.replaceFirst("^/[a-z]{2}/", "/");
	}

	@Test
	void signupStoresOnlyThePermittedFields() throws Exception {
		mockMvc.perform(withForgedFields(post("/signup").with(csrf()), 999).param("email", NEW_EMAIL).param("password", NEW_PASSWORD).param("name", "Ann").param("surname", "Lee"))
			.andExpect(status().is3xxRedirection());

		List<Map<String, Object>> rows = jdbc.queryForList("SELECT r.*, y.name, y.surname FROM YadaRegistrationRequest r JOIN YexRegistrationRequest y ON y.id=r.id");
		assertEquals(1, rows.size());
		Map<String, Object> row = rows.get(0);
		assertNotEquals(999L, ((Number) row.get("id")).longValue());
		assertEquals(NEW_EMAIL, row.get("email"));
		assertEquals(TYPE_REGISTRATION, ((Number) row.get("registrationType")).intValue());
		assertNull(row.get("yadaUserCredentials_id"));
		assertNull(row.get("trattamentoDati_id"));
		assertNotEquals("0".repeat(64), row.get("tokenHash"));
		assertTrue(((Timestamp) row.get("timestamp")).getTime() > System.currentTimeMillis() - 3600_000L, "Forged timestamp stored");
		assertTrue(((Timestamp) row.get("expiration")).getTime() < System.currentTimeMillis() + 25 * 3600_000L, "Forged expiration stored");
		assertEquals("Ann", row.get("name"));
		assertEquals("Lee", row.get("surname"));

		// The subclass fields reach the new user through the confirmation link, which works once
		String confirmation = mailbox().lastMessageTo(NEW_EMAIL).links().stream().map(RegistrationControllerAuthBindingTest::localPath).filter(path -> path.startsWith("/registrationConfirmation/")).findFirst().orElseThrow();
		mockMvc.perform(get(confirmation)).andExpect(status().is3xxRedirection());
		UserProfile user = findUser(NEW_EMAIL);
		assertNotNull(user, "The registration was not completed");
		assertEquals("Ann", user.getFirstName());
		assertEquals("Lee", user.getLastName());
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM YadaRegistrationRequest", Long.class));
		mockMvc.perform(get(confirmation)).andExpect(status().is3xxRedirection());
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM YadaUserCredentials WHERE username=?", Long.class, NEW_EMAIL));
	}

	@Test
	void passwordResetUsesOnlyTheEmail() throws Exception {
		mockMvc.perform(post("/yadaPasswordResetPost").with(csrf()).param("email", USER_EMAIL)).andExpect(status().is3xxRedirection());
		long ownId = jdbc.queryForObject("SELECT id FROM YadaRegistrationRequest WHERE email=?", Long.class, USER_EMAIL);

		mockMvc.perform(withForgedFields(post("/yadaPasswordResetPost").with(csrf()), ownId).param("email", ADMIN_EMAIL).param("password", "attacker-password"))
			.andExpect(status().is3xxRedirection());

		assertEquals(USER_EMAIL, jdbc.queryForObject("SELECT email FROM YadaRegistrationRequest WHERE id=?", String.class, ownId), "The existing request was overwritten");
		List<Map<String, Object>> adminRows = jdbc.queryForList("SELECT * FROM YadaRegistrationRequest WHERE email=?", ADMIN_EMAIL);
		assertEquals(1, adminRows.size());
		Map<String, Object> adminRow = adminRows.get(0);
		assertNotEquals(ownId, ((Number) adminRow.get("id")).longValue());
		assertEquals(TYPE_PASSWORD_RECOVERY, ((Number) adminRow.get("registrationType")).intValue());
		assertNull(adminRow.get("yadaUserCredentials_id"));
		assertNotEquals("attacker-password", adminRow.get("password"));
		assertNotEquals("0".repeat(64), adminRow.get("tokenHash"));
		assertEquals(30L, jdbc.queryForObject("SELECT TIMESTAMPDIFF(MINUTE, timestamp, expiration) FROM YadaRegistrationRequest WHERE email=?", Long.class, ADMIN_EMAIL));
	}

	@Test
	void theRecoveryLinkOpensTheFormOnlyWithItsSecret() throws Exception {
		mockMvc.perform(post("/yadaPasswordResetPost").with(csrf()).param("email", USER_EMAIL)).andExpect(status().is3xxRedirection());
		String link = mailbox().lastMessageTo(USER_EMAIL).links().stream().map(RegistrationControllerAuthBindingTest::localPath).filter(path -> path.startsWith("/passwordReset/")).findFirst().orElseThrow();
		assertTrue(link.matches("/passwordReset/\\d+-[0-9a-f]{64}"), link);
		// A valid link opens the home page (with a locale redirect here); an invalid one redirects to the reset form
		String valid = mockMvc.perform(get(link)).andReturn().getResponse().getRedirectedUrl();
		assertTrue(valid == null || !valid.endsWith("/passwordReset"), valid);
		long id = jdbc.queryForObject("SELECT id FROM YadaRegistrationRequest", Long.class);
		String digest = jdbc.queryForObject("SELECT tokenHash FROM YadaRegistrationRequest", String.class);
		for (String forged : new String[] { id + "-" + digest, id + "-1234" }) {
			String redirect = mockMvc.perform(get("/passwordReset/" + forged)).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
			assertTrue(redirect.endsWith("/passwordReset"), redirect);
		}
	}
}
