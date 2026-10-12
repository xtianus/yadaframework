package net.yadaframework.tests.components;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import net.yadaframework.commerce.persistence.entity.YadaOrderStatus;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.repository.YadaPersistentEnumDao;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.tests.persistence.entity.UserProfile;
import net.yadaframework.tests.persistence.repository.ArticleDao;
import net.yadaframework.tests.persistence.repository.UserProfileDao;

/**
 * Creates the application fixtures used by the Java tests, always through the application DAOs
 * so that every write is committed in its own transaction.
 * <p>
 * Reset contract: the test harness empties every application table (keeping the Flyway history)
 * and then calls {@link #seedDefaults()}, which recreates the same data with the same ids.
 */
@Service
public class TestDataService {
	public static final String ADMIN_EMAIL = "admin@yts.test";
	public static final String ADMIN_PASSWORD = "admin-password";
	public static final String USER_EMAIL = "user@yts.test";
	public static final String USER_PASSWORD = "user-password";
	/** Default articles: sku, English name, unit price (values that a double represents exactly) */
	public static final List<Object[]> ARTICLES = List.of(
		new Object[] { "SKU-A", "Armchair", 125.0 },
		new Object[] { "SKU-B", "Bookcase", 349.5 },
		new Object[] { "SKU-C", "Cabinet", 89.0 },
		new Object[] { "SKU-D", "Desk", 210.0 },
		new Object[] { "SKU-E", "Easel", 45.25 });

	private final YadaUserCredentialsDao yadaUserCredentialsDao;
	private final UserProfileDao userProfileDao;
	private final YadaConfiguration config;
	private final YadaPersistentEnumDao yadaPersistentEnumDao;
	private final ArticleDao articleDao;

	public TestDataService(YadaUserCredentialsDao yadaUserCredentialsDao, UserProfileDao userProfileDao, YadaConfiguration config, YadaPersistentEnumDao yadaPersistentEnumDao, ArticleDao articleDao) {
		this.yadaUserCredentialsDao = yadaUserCredentialsDao;
		this.userProfileDao = userProfileDao;
		this.config = config;
		this.yadaPersistentEnumDao = yadaPersistentEnumDao;
		this.articleDao = articleDao;
	}

	/**
	 * Creates the deterministic default data: the persistent order states, one administrator, one plain user and the default articles.
	 */
	public void seedDefaults() {
		yadaPersistentEnumDao.initDatabase(List.of(YadaOrderStatus.class), config.getLocales());
		createUser(ADMIN_EMAIL, ADMIN_PASSWORD, "admin", "USER", "ADMIN");
		createUser(USER_EMAIL, USER_PASSWORD, "user", "USER");
		for (Object[] definition : ARTICLES) {
			articleDao.createCommerceArticle((String) definition[0], Locale.ENGLISH, (String) definition[1], new YadaMoney((Double) definition[2]));
		}
	}

	/**
	 * Creates an enabled user with the given roles.
	 * @param email the login name
	 * @param password the clear-text password
	 * @param nickname the profile nickname
	 * @param roleKeys the configured role keys, e.g. "USER"
	 * @return the new profile, or null when a user with that email exists already
	 */
	public UserProfile createUser(String email, String password, String nickname, String... roleKeys) {
		Set<Integer> roles = new LinkedHashSet<>();
		for (String roleKey : roleKeys) {
			Integer roleId = config.getRoleId(roleKey);
			if (roleId == null) {
				throw new IllegalArgumentException("Role " + roleKey + " is not configured");
			}
			roles.add(roleId);
		}
		Map<String, Object> userDefinition = new HashMap<>();
		userDefinition.put("email", email);
		userDefinition.put("password", password);
		userDefinition.put("nickname", nickname);
		userDefinition.put("roles", roles);
		return yadaUserCredentialsDao.create(userDefinition, UserProfile.class);
	}

	/**
	 * @param email the login name
	 * @return the profile of that user, or null
	 */
	public UserProfile findUser(String email) {
		return userProfileDao.findUserProfileByUsername(email);
	}
}
