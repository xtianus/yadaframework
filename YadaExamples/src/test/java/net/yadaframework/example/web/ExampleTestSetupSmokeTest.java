package net.yadaframework.example.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.flywaydb.core.api.MigrationVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.example.support.CapturedMail;
import net.yadaframework.example.support.ExampleWebTestBase;

/**
 * The example test setup works: the real configuration boots on the embedded database with every migration applied,
 * the login page renders the real form, a seeded account logs in through it and makes an authenticated POST with a
 * CSRF token, and emails are captured.
 */
class ExampleTestSetupSmokeTest extends ExampleWebTestBase {
	private static final Pattern MIGRATION_VERSION = Pattern.compile("V(\\d+(?:[._]\\d+)*)__.+\\.sql");

	@Autowired private DataSource dataSource;

	/**
	 * @return the highest migration version found in the "database" classpath folder of the example, e.g. "001"
	 */
	private static String latestMigrationVersion() throws Exception {
		Resource[] migrations = new PathMatchingResourcePatternResolver().getResources("classpath*:database/V*__*.sql");
		return Arrays.stream(migrations).map(Resource::getFilename).map(MIGRATION_VERSION::matcher).filter(Matcher::matches).map(matcher -> MigrationVersion.fromVersion(matcher.group(1)))
			.max(Comparator.naturalOrder()).map(MigrationVersion::getVersion)
			.orElseThrow(() -> new AssertionError("No migration found in classpath:database"));
	}

	@Test
	void latestMigrationIsInTheFlywayHistory() throws Exception {
		String latest = latestMigrationVersion();
		List<String> applied = new JdbcTemplate(dataSource).queryForList("SELECT version FROM " + config.flywayTableName() + " WHERE success=1 AND type='SQL'", String.class);
		assertTrue(applied.contains(latest), "Migration " + latest + " not applied: " + applied);
	}

	@Test
	void seededAccountsHaveTheirRoles() {
		UserProfile supervisor = findUser(SUPERVISOR_EMAIL);
		assertNotNull(supervisor);
		assertTrue(supervisor.hasAnyRoleKey("SUPERVISOR"));
		assertTrue(findUser(ADMIN_EMAIL).hasAnyRoleKey("ADMIN"));
		assertTrue(findUser(USER_EMAIL).hasAnyRoleKey("USER"));
		assertTrue(findUser(TARGET_EMAIL).hasAnyRoleKey("USER"));
		assertTrue(!findUser(USER_EMAIL).hasAnyRoleKey("SUPERVISOR", "ADMIN"));
	}

	@Test
	void loginPageRendersTheRealForm() throws Exception {
		String body = mockMvc.perform(get("/login")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		Document page = Jsoup.parse(body);
		assertNotNull(page.selectFirst("form#loginForm[action$=/loginPost] input[name=username]"), "No login form");
		assertTrue(!page.selectFirst("form#loginForm input[name=_csrf]").attr("value").isBlank(), "No CSRF token in the login form");
	}

	@Test
	void authenticatedPostWithCsrfToken() throws Exception {
		mockMvc.perform(post("/dashboard").with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/**/login"));
		MockHttpSession supervisorSession = login(SUPERVISOR_EMAIL, SUPERVISOR_PASSWORD);
		mockMvc.perform(post("/dashboard").session(supervisorSession)).andExpect(status().isForbidden());
		mockMvc.perform(post("/dashboard").session(supervisorSession).param("_csrf", csrfToken(supervisorSession))).andExpect(status().isOk()).andExpect(view().name("/dashboard/dashboard"));
		MockHttpSession userSession = login(USER_EMAIL, USER_PASSWORD);
		mockMvc.perform(post("/dashboard").session(userSession).param("_csrf", csrfToken(userSession))).andExpect(status().isForbidden());
	}

	@Test
	void configurationCanBeOverriddenForOneTest() throws Exception {
		assertEquals(48, config.getAutologinExpirationHours());
		overrideConfiguration("security/autologinExpirationHours", "2");
		overrideConfiguration("email/@throwExceptions", "true");
		assertEquals(2, config.getAutologinExpirationHours());
		assertTrue(config.isEmailThrowExceptions());
		assertEquals(240, config.getConfiguration().getInt("config/security/sessionTimeoutMinutes"), "Sibling values are kept");
		assertEquals(Integer.valueOf(4), config.getRoleId("SUPERVISOR"), "Configured lists are kept");
		overrideConfiguration("security/autologinExpirationHours");
		assertEquals(48, config.getAutologinExpirationHours(), "Removing an override restores the value");
		restoreConfiguration();
		assertFalse(config.isEmailThrowExceptions(), "The overrides survive the test");
	}

	@Test
	void emailsAreCaptured() throws Exception {
		mockMvc.perform(post("/yadaPasswordResetPost").param("email", USER_EMAIL).with(csrf())).andExpect(status().is3xxRedirection());
		CapturedMail mail = mailbox().lastMessageTo(USER_EMAIL);
		assertTrue(mail.links().stream().anyMatch(link -> link.contains("/passwordReset/")), mail.body());
		resetDatabase();
		assertTrue(mailbox().messages().isEmpty());
	}
}
