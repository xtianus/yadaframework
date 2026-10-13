package net.yadaframework.example.support;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.multipart.support.MultipartFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.example.core.TestRootConfig;
import net.yadaframework.example.core.TestServletConfig;
import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.example.persistence.repository.UserProfileDao;
import net.yadaframework.security.AuditFilter;
import net.yadaframework.security.CheckSessionFilter;
import net.yadaframework.security.components.YadaAuthenticationSuccessFilter;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;

/**
 * Base class of the example tests that need Hibernate, the database or the Spring Security filter chain.
 * <p>
 * It boots the real example configuration (see {@link TestRootConfig} and {@link TestServletConfig}) once per JVM, in a
 * cached Spring web test context over src/main/webapp, on the framework embedded MariaDB configured by
 * src/test/resources/conf.webapp.test.xml, where every example migration runs on a fresh schema. {@link #mockMvc} sends
 * requests through the same servlet filters as SecurityWebApplicationInitializer and then through the Spring Security
 * filter chain. MockMvc doesn't execute forwards: the YadaLocalePathVariableFilter forwards "/en/..." to "/...", so a
 * request to a locale path only returns the forwarded url; request the path without the locale instead.
 * <p>
 * Before the first test of each class the database is emptied (except the Flyway history) and the seeded accounts are
 * created again through the application DAOs, with the configured password encoder. Requests commit their own
 * transactions: a test that changes data and needs a clean state for the next test calls {@link #resetDatabase()}.
 * Use {@link #login(String, String)} to obtain an authenticated session through the real login form; a
 * spring-security-test mock user is acceptable only with the username of a seeded account.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration("src/main/webapp")
@ContextHierarchy({
	@ContextConfiguration(name = "root", classes = TestRootConfig.class),
	@ContextConfiguration(name = "servlet", classes = TestServletConfig.class)
})
public abstract class ExampleWebTestBase {
	/** Administrator: roles USER and ADMIN */
	public static final String ADMIN_EMAIL = "admin@example.test";
	public static final String ADMIN_PASSWORD = "admin-test-password";
	/** Supervisor: roles USER and SUPERVISOR; can manage and impersonate ordinary users */
	public static final String SUPERVISOR_EMAIL = "supervisor@example.test";
	public static final String SUPERVISOR_PASSWORD = "supervisor-test-password";
	/** Ordinary user (role USER) that the supervisor may manage */
	public static final String USER_EMAIL = "user@example.test";
	public static final String USER_PASSWORD = "user-test-password";
	/** Ordinary user (role USER) used as impersonation target */
	public static final String TARGET_EMAIL = "target@example.test";
	public static final String TARGET_PASSWORD = "target-test-password";

	/** The seeded accounts: email, password, nickname, role keys */
	private static final List<String[]> SEEDED_ACCOUNTS = List.of(
		new String[] { ADMIN_EMAIL, ADMIN_PASSWORD, "admin", "USER", "ADMIN" },
		new String[] { SUPERVISOR_EMAIL, SUPERVISOR_PASSWORD, "supervisor", "USER", "SUPERVISOR" },
		new String[] { USER_EMAIL, USER_PASSWORD, "user", "USER" },
		new String[] { TARGET_EMAIL, TARGET_PASSWORD, "target", "USER" });

	/** The test class whose data has been seeded last */
	private static Class<?> seededFor = null;

	static {
		ExampleTestDatabase.prepare();
	}

	/** The servlet context of the application, child of the root context */
	@Autowired protected WebApplicationContext webApplicationContext;
	@Autowired protected YadaConfiguration config;
	@Autowired protected UserProfileDao userProfileDao;
	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private DataSource dataSource;
	@Autowired private CapturingMailSender capturingMailSender;

	protected MockMvc mockMvc;

	@BeforeEach
	void setUpExampleTest() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.addFilters(applicationFilters())
			.apply(SecurityMockMvcConfigurers.springSecurity())
			.build();
		if (seededFor != getClass()) {
			resetDatabase();
			seededFor = getClass();
		}
	}

	/**
	 * The filters that SecurityWebApplicationInitializer installs before Spring Security, in the same order
	 */
	private Filter[] applicationFilters() throws ServletException {
		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter("UTF-8", true);
		Filter[] filters = new Filter[] {
			new CheckSessionFilter(),
			characterEncodingFilter,
			new AuditFilter(),
			new MultipartFilter(),
			new DelegatingFilterProxy("yadaLocalePathVariableFilter", webApplicationContext),
			new YadaAuthenticationSuccessFilter()
		};
		for (Filter filter : filters) {
			filter.init(new MockFilterConfig(webApplicationContext.getServletContext()));
		}
		return filters;
	}

	/**
	 * Empties every table except the Flyway history, resets the auto-increment counters, empties the mailbox and
	 * creates the seeded accounts again, so their ids are the same every time.
	 */
	protected void resetDatabase() throws SQLException {
		String flywayTable = config.flywayTableName();
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			List<String> tables = new ArrayList<>();
			try (ResultSet resultSet = statement.executeQuery("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE'")) {
				while (resultSet.next()) {
					String table = resultSet.getString(1);
					if (!table.equalsIgnoreCase(flywayTable)) {
						tables.add(table);
					}
				}
			}
			// DELETE rather than TRUNCATE, which is DDL in MariaDB; the order of the foreign keys doesn't matter
			statement.execute("SET FOREIGN_KEY_CHECKS=0");
			try {
				for (String table : tables) {
					statement.executeUpdate("DELETE FROM `" + table + "`");
					statement.execute("ALTER TABLE `" + table + "` AUTO_INCREMENT=1");
				}
			} finally {
				statement.execute("SET FOREIGN_KEY_CHECKS=1");
			}
		}
		capturingMailSender.clear();
		for (String[] account : SEEDED_ACCOUNTS) {
			Set<Integer> roles = new LinkedHashSet<>();
			for (int i = 3; i < account.length; i++) {
				roles.add(config.getRoleId(account[i]));
			}
			Map<String, Object> userDefinition = new HashMap<>();
			userDefinition.put("email", account[0]);
			userDefinition.put("password", account[1]);
			userDefinition.put("nickname", account[2]);
			userDefinition.put("roles", roles);
			yadaUserCredentialsDao.create(userDefinition, UserProfile.class);
		}
	}

	/**
	 * @param email the login name of a seeded or test-created account
	 * @return its profile, or null
	 */
	protected UserProfile findUser(String email) {
		return userProfileDao.findUserProfileByUsername(email);
	}

	/**
	 * @return the emails sent by the application since the last reset
	 */
	protected CapturingMailSender mailbox() {
		return capturingMailSender;
	}

	/**
	 * Logs in through the real login page and form, as a browser does
	 * @param email the username
	 * @param password the clear password
	 * @return the authenticated session, to pass to the next requests
	 * @throws AssertionError when the login fails
	 */
	protected MockHttpSession login(String email, String password) throws Exception {
		MockHttpSession session = new MockHttpSession(webApplicationContext.getServletContext());
		String token = loginFormCsrfToken(session);
		MvcResult result = mockMvc.perform(post("/loginPost").session(session).param("username", email).param("password", password).param("_csrf", token))
			.andExpect(authenticated().withUsername(email))
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private String loginFormCsrfToken(MockHttpSession session) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get("/login").session(session)).andReturn().getResponse();
		if (response.getStatus() != 200) {
			throw new AssertionError("The login page answered " + response.getStatus());
		}
		Document page = Jsoup.parse(response.getContentAsString());
		Element tokenInput = page.selectFirst("form#loginForm input[name=_csrf]");
		if (tokenInput == null) {
			throw new AssertionError("No login form with a CSRF token on the login page");
		}
		return tokenInput.attr("value");
	}

	/**
	 * The CSRF token of a session, as a page rendered for that session would contain it. Login replaces the token,
	 * so call this after {@link #login(String, String)}.
	 * @param session the session
	 * @return the token value to send as "_csrf" parameter
	 */
	protected String csrfToken(MockHttpSession session) throws Exception {
		MvcResult result = mockMvc.perform(get("/login").session(session)).andReturn();
		CsrfToken token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
		if (token == null) {
			throw new AssertionError("No CSRF token in the request");
		}
		return token.getToken();
	}
}
