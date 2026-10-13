package net.yadaframework.example.web.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import net.yadaframework.example.support.ExampleWebTestBase;

/**
 * The profile edit and add-user endpoint of the example binds only the editable fields: the profile to change is chosen
 * by the userProfileId selector alone, forged profile and credential ids, versions, password hashes and timestamps are
 * never copied, the stored and requested roles are authorized before anything is written, and an unknown selector
 * fails instead of creating a user.
 */
class UserProfileIdentitySecurityTest extends ExampleWebTestBase {
	private static final String EDIT_URL = "/dashboard/userwrite/ajaxAddOrUpdateUserProfile";
	private static final String NEW_EMAIL = "added@example.test";
	private static final String ATTACKER_PASSWORD = "attacker-password-1";

	@Autowired private DataSource dataSource;
	@Autowired private PasswordEncoder passwordEncoder;
	private JdbcTemplate jdbc;
	private MockHttpSession supervisor;
	private int roleUser;
	private int roleSupervisor;
	private int roleAdmin;

	@BeforeEach
	void cleanState() throws Exception {
		resetDatabase();
		jdbc = new JdbcTemplate(dataSource);
		supervisor = login(SUPERVISOR_EMAIL, SUPERVISOR_PASSWORD);
		roleUser = config.getRoleId("USER");
		roleSupervisor = config.getRoleId("SUPERVISOR");
		roleAdmin = config.getRoleId("ADMIN");
	}

	private long profileId(String email) {
		return findUser(email).getId();
	}

	private long credentialsId(String email) {
		return jdbc.queryForObject("SELECT id FROM YadaUserCredentials WHERE username=?", Long.class, email);
	}

	private Map<String, Object> credentialsRow(long credentialsId) {
		return jdbc.queryForMap("SELECT * FROM YadaUserCredentials WHERE id=?", credentialsId);
	}

	private List<Integer> roles(long credentialsId) {
		return jdbc.queryForList("SELECT roles FROM YadaUserCredentials_roles WHERE YadaUserCredentials_id=? ORDER BY roles", Integer.class, credentialsId);
	}

	/**
	 * @return every profile, credentials and role row, to check that a request changed nothing
	 */
	private String snapshot() {
		return jdbc.queryForList("SELECT * FROM YadaUserProfile ORDER BY id").toString()
			+ jdbc.queryForList("SELECT * FROM YadaUserCredentials ORDER BY id").toString()
			+ jdbc.queryForList("SELECT * FROM YadaUserCredentials_roles ORDER BY YadaUserCredentials_id, roles").toString();
	}

	/**
	 * A CSRF-valid post of the supervisor to the edit endpoint
	 */
	private MockHttpServletRequestBuilder edit() {
		return post(EDIT_URL).session(supervisor).with(csrf());
	}

	private MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
		assertTrue(response.getStatus() < 500, "Server error " + response.getStatus());
		return response;
	}

	@Test
	void theScanAttackDoesNotReachTheAdminCredentials() throws Exception {
		long userProfile = profileId(USER_EMAIL);
		long userCredentials = credentialsId(USER_EMAIL);
		long adminCredentials = credentialsId(ADMIN_EMAIL);
		long adminVersion = ((Number) credentialsRow(adminCredentials).get("version")).longValue();
		String before = snapshot();

		MockHttpServletResponse response = mockMvc.perform(edit()
			.param("userProfileId", Long.toString(userProfile))
			.param("userCredentials.id", Long.toString(adminCredentials))
			.param("userCredentials.version", Long.toString(adminVersion))
			.param("userCredentials.username", ADMIN_EMAIL)
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.newPassword", ATTACKER_PASSWORD)).andReturn().getResponse();

		assertEquals(before, snapshot(), "The request changed some user");
		assertEquals(userCredentials, jdbc.queryForObject("SELECT userCredentials_id FROM YadaUserProfile WHERE id=?", Long.class, userProfile));
		assertTrue(roles(adminCredentials).contains(roleAdmin));
		login(ADMIN_EMAIL, ADMIN_PASSWORD);
		// Not stopped by a database constraint violation but by the controller, which shows the form again
		assertEquals(200, response.getStatus());
		assertTrue(response.getContentAsString().contains("User already exists"));
	}

	@Test
	void aForgedProfileIdDoesNotRedirectTheEdit() throws Exception {
		long userProfile = profileId(USER_EMAIL);
		long targetProfile = profileId(TARGET_EMAIL);
		Map<String, Object> targetProfileRow = jdbc.queryForMap("SELECT * FROM YadaUserProfile WHERE id=?", targetProfile);
		Map<String, Object> targetCredentialsRow = credentialsRow(credentialsId(TARGET_EMAIL));

		send(edit()
			.param("userProfileId", Long.toString(userProfile))
			.param("id", Long.toString(targetProfile))
			.param("userCredentials.username", "renamed@example.test")
			.param("userCredentials.roles", Integer.toString(roleUser)));

		assertEquals(targetProfileRow, jdbc.queryForMap("SELECT * FROM YadaUserProfile WHERE id=?", targetProfile));
		assertEquals(targetCredentialsRow, credentialsRow(credentialsId(TARGET_EMAIL)));
		assertEquals(userProfile, profileId("renamed@example.test"), "The selected profile was not edited");
	}

	@Test
	void nestedCredentialPropertiesAreNotBound() throws Exception {
		long userProfile = profileId(USER_EMAIL);
		long userCredentials = credentialsId(USER_EMAIL);
		Map<String, Object> before = credentialsRow(userCredentials);
		long profileVersion = jdbc.queryForObject("SELECT version FROM YadaUserProfile WHERE id=?", Long.class, userProfile);

		send(edit()
			.param("userProfileId", Long.toString(userProfile))
			.param("version", "0")
			.param("firstName", "Forged")
			.param("userCredentials.username", "renamed@example.test")
			.param("userCredentials.enabled", "true")
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.password", "forged-hash")
			.param("userCredentials.version", "0"));

		Map<String, Object> after = credentialsRow(userCredentials);
		assertEquals("renamed@example.test", after.get("username"), "The permitted field was not saved");
		assertEquals(before.get("password"), after.get("password"));
		assertEquals(before.get("passwordDate"), after.get("passwordDate"));
		assertEquals(before.get("creationDate"), after.get("creationDate"));
		assertEquals(before.get("lastSuccessfulLogin"), after.get("lastSuccessfulLogin"));
		assertNull(jdbc.queryForObject("SELECT firstName FROM YadaUserProfile WHERE id=?", String.class, userProfile));
		assertTrue(jdbc.queryForObject("SELECT version FROM YadaUserProfile WHERE id=?", Long.class, userProfile) >= profileVersion);
		assertEquals(userCredentials, jdbc.queryForObject("SELECT userCredentials_id FROM YadaUserProfile WHERE id=?", Long.class, userProfile));
		login("renamed@example.test", USER_PASSWORD);
	}

	@Test
	void theStoredRolesOfTheTargetAreAuthorized() throws Exception {
		String before = snapshot();

		send(edit()
			.param("userProfileId", Long.toString(profileId(ADMIN_EMAIL)))
			.param("userCredentials.username", ADMIN_EMAIL)
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.newPassword", ATTACKER_PASSWORD));

		assertEquals(before, snapshot());
		login(ADMIN_EMAIL, ADMIN_PASSWORD);
	}

	@Test
	void theRequestedRolesAreAuthorizedBeforeAnyChange() throws Exception {
		String before = snapshot();

		send(edit()
			.param("userProfileId", Long.toString(profileId(USER_EMAIL)))
			.param("userCredentials.username", "renamed@example.test")
			.param("userCredentials.roles", Integer.toString(roleUser), Integer.toString(roleAdmin))
			.param("userCredentials.newPassword", ATTACKER_PASSWORD));

		assertEquals(before, snapshot(), "Something was committed before the roles were authorized");
	}

	@Test
	void addingAUserNeverWritesAnExistingRow() throws Exception {
		long userProfile = profileId(USER_EMAIL);
		long userCredentials = credentialsId(USER_EMAIL);
		Map<String, Object> userProfileRow = jdbc.queryForMap("SELECT * FROM YadaUserProfile WHERE id=?", userProfile);
		Map<String, Object> userCredentialsRow = credentialsRow(userCredentials);

		send(edit()
			.param("id", Long.toString(userProfile))
			.param("userCredentials.id", Long.toString(userCredentials))
			.param("userCredentials.username", NEW_EMAIL)
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.newPassword", "added-password-1"));

		assertEquals(userProfileRow, jdbc.queryForMap("SELECT * FROM YadaUserProfile WHERE id=?", userProfile));
		assertEquals(userCredentialsRow, credentialsRow(userCredentials));
		assertNotNull(findUser(NEW_EMAIL), "The new user was not created");
		assertNotEquals(userProfile, profileId(NEW_EMAIL));
		assertNotEquals(userCredentials, credentialsId(NEW_EMAIL));
	}

	@Test
	void addingAUserWithTheIdsOfAnAdminCreatesANewUser() throws Exception {
		long adminProfile = profileId(ADMIN_EMAIL);
		long adminCredentials = credentialsId(ADMIN_EMAIL);
		Map<String, Object> adminCredentialsRow = credentialsRow(adminCredentials);

		send(edit()
			.param("id", Long.toString(adminProfile))
			.param("userCredentials.id", Long.toString(adminCredentials))
			.param("userCredentials.username", NEW_EMAIL)
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.newPassword", "added-password-1"));

		assertEquals(adminCredentialsRow, credentialsRow(adminCredentials));
		assertEquals(adminCredentials, jdbc.queryForObject("SELECT userCredentials_id FROM YadaUserProfile WHERE id=?", Long.class, adminProfile));
		assertNotNull(findUser(NEW_EMAIL), "The new user was not created");
		assertNotEquals(adminProfile, profileId(NEW_EMAIL));
	}

	@Test
	void anUnknownProfileIdFailsInsteadOfCreatingAUser() throws Exception {
		String before = snapshot();

		send(edit()
			.param("userProfileId", "999999")
			.param("userCredentials.username", NEW_EMAIL)
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.newPassword", "added-password-1"));

		assertEquals(before, snapshot());
		assertNull(findUser(NEW_EMAIL));
	}

	@Test
	void theEditEndpointNeedsAPostWithCsrf() throws Exception {
		String before = snapshot();
		String userProfile = Long.toString(profileId(USER_EMAIL));

		mockMvc.perform(get(EDIT_URL).session(supervisor)
			.param("userProfileId", userProfile)
			.param("userCredentials.username", "renamed@example.test")
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.newPassword", ATTACKER_PASSWORD));
		mockMvc.perform(post(EDIT_URL).session(supervisor)
			.param("userProfileId", userProfile)
			.param("userCredentials.username", "renamed@example.test")
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.newPassword", ATTACKER_PASSWORD));

		assertEquals(before, snapshot());
	}

	@Test
	void aPermittedEditIsSaved() throws Exception {
		long userProfile = profileId(USER_EMAIL);
		long userCredentials = credentialsId(USER_EMAIL);

		send(edit()
			.param("userProfileId", Long.toString(userProfile))
			.param("userCredentials.username", " renamed@example.test ")
			.param("userCredentials.enabled", "true")
			.param("userCredentials.changePassword", "true")
			.param("userCredentials.roles", Integer.toString(roleSupervisor))
			.param("userCredentials.newPassword", "changed-password-1"));

		Map<String, Object> after = credentialsRow(userCredentials);
		assertEquals("renamed@example.test", after.get("username"));
		assertEquals(true, after.get("changePassword"));
		// The USER role is always added
		assertEquals(List.of(roleUser, roleSupervisor), roles(userCredentials));
		assertTrue(passwordEncoder.matches("changed-password-1", (String) after.get("password")));
		assertEquals(userCredentials, credentialsId("renamed@example.test"));
	}

	@Test
	void aDuplicateUsernameShowsTheFormAgain() throws Exception {
		String before = snapshot();

		MockHttpServletResponse response = send(edit()
			.param("userProfileId", Long.toString(profileId(USER_EMAIL)))
			.param("userCredentials.username", TARGET_EMAIL)
			.param("userCredentials.roles", Integer.toString(roleUser)));

		assertEquals(before, snapshot());
		String page = response.getContentAsString();
		assertTrue(page.contains("name=\"userForm\""), "The form was not shown again");
		assertTrue(page.contains("User already exists"), "The error was not shown");
	}

	@Test
	void aPermittedAddCreatesAnInvitedUser() throws Exception {
		send(edit()
			.param("userCredentials.username", NEW_EMAIL)
			.param("userCredentials.enabled", "true")
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.newPassword", "added-password-1")
			.param("inviteEmail", "true"));

		long added = credentialsId(NEW_EMAIL);
		assertEquals(List.of(roleUser), roles(added));
		assertNotNull(mailbox().lastMessageTo(NEW_EMAIL), "No invitation sent");
		login(NEW_EMAIL, "added-password-1");
	}

	@Test
	void theFailedAttemptsCanOnlyBeReset() throws Exception {
		long userProfile = profileId(USER_EMAIL);
		long userCredentials = credentialsId(USER_EMAIL);
		jdbc.update("UPDATE YadaUserCredentials SET failedAttempts=5, lastFailedAttempt=NOW() WHERE id=?", userCredentials);

		// Reset
		send(edit()
			.param("userProfileId", Long.toString(userProfile))
			.param("userCredentials.username", USER_EMAIL)
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.failedAttempts", "0"));
		Map<String, Object> after = credentialsRow(userCredentials);
		assertEquals(0, ((Number) after.get("failedAttempts")).intValue());
		assertNull(after.get("lastFailedAttempt"));

		// Any other value is not stored
		send(edit()
			.param("userProfileId", Long.toString(userProfile))
			.param("userCredentials.username", USER_EMAIL)
			.param("userCredentials.roles", Integer.toString(roleUser))
			.param("userCredentials.failedAttempts", "9"));
		after = credentialsRow(userCredentials);
		assertEquals(0, ((Number) after.get("failedAttempts")).intValue());
		assertNull(after.get("lastFailedAttempt"));
	}

	@Test
	void theEditFormShowsOnlyTheSelectedProfile() throws Exception {
		long userProfile = profileId(USER_EMAIL);
		String page = mockMvc.perform(get("/dashboard/userwrite/ajaxEditUserProfileForm").session(supervisor).param("userProfileId", Long.toString(userProfile)))
			.andReturn().getResponse().getContentAsString();
		assertTrue(page.contains("value=\"" + USER_EMAIL + "\""), "The selected profile is not in the form");
		assertTrue(page.contains("name=\"userProfileId\" value=\"" + userProfile + "\""), "The selector is not in the form");

		String newForm = mockMvc.perform(get("/dashboard/userwrite/ajaxEditUserProfileForm").session(supervisor))
			.andReturn().getResponse().getContentAsString();
		assertFalse(newForm.contains("name=\"userProfileId\""), "A new user form has a selector");
	}
}
