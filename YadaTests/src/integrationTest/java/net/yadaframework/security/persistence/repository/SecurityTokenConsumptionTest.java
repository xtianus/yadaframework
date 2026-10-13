package net.yadaframework.security.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.security.components.YadaSecurityTokenCodec;
import net.yadaframework.security.components.YadaSecurityTokenCodec.ParsedSecurityToken;
import net.yadaframework.security.components.YadaTokenHandler;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;

/**
 * Token issuance and consumption of {@link YadaRegistrationRequestDao} and {@link YadaAutoLoginTokenDao} on the real
 * database: consumption locks the row in the database so that of two concurrent transactions only one uses a token,
 * a rollback leaves the token usable, and tokens loaded from the database can't produce links.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityTokenConsumptionTest {
	private ApplicationSession session;
	private YadaRegistrationRequestDao registrationRequestDao;
	private YadaAutoLoginTokenDao autoLoginTokenDao;
	private YadaUserCredentialsDao userCredentialsDao;
	private YadaTokenHandler tokenHandler;
	private YadaSecurityTokenCodec codec;
	private TransactionTemplate transactionTemplate;
	/** Own threads: the common pool may have a single thread, which the first blocked transaction would hold */
	private final ExecutorService executor = Executors.newFixedThreadPool(2);

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
		registrationRequestDao = session.application().getBean(YadaRegistrationRequestDao.class);
		autoLoginTokenDao = session.application().getBean(YadaAutoLoginTokenDao.class);
		userCredentialsDao = session.application().getBean(YadaUserCredentialsDao.class);
		tokenHandler = session.application().getBean(YadaTokenHandler.class);
		codec = session.application().getBean(YadaSecurityTokenCodec.class);
		transactionTemplate = new TransactionTemplate(session.application().getBean(PlatformTransactionManager.class));
	}

	@AfterAll
	void stopApplication() {
		executor.shutdownNow();
		if (session != null) {
			session.close();
		}
	}

	@BeforeEach
	void resetData() {
		session.fixtures().reset();
	}

	private YadaRegistrationRequest issueRecovery(String email) {
		YadaRegistrationRequest request = new YadaRegistrationRequest();
		request.setEmail(email);
		request.setPassword("unused");
		request.setRegistrationType(YadaRegistrationType.PASSWORD_RECOVERY);
		return registrationRequestDao.issue(request);
	}

	private ParsedSecurityToken parsedTokenOf(YadaRegistrationRequest request) {
		return codec.parse(tokenHandler.makeLink(request, null));
	}

	private long requestRows() {
		return session.fixtures().countRows("YadaRegistrationRequest");
	}

	/**
	 * Waits until a transaction of the schema is waiting for a row lock, or fails when the waiting future ends first
	 */
	private void awaitLockWait(CompletableFuture<?> waiting) throws Exception {
		long deadline = System.currentTimeMillis() + 20_000;
		try (Connection connection = DriverManager.getConnection(session.run().configuredJdbcUrl(), "root", ""); Statement statement = connection.createStatement()) {
			while (System.currentTimeMillis() < deadline) {
				if (waiting.isDone()) {
					throw new AssertionError("The second transaction ended without waiting: " + (waiting.isCompletedExceptionally() ? "exception" : waiting.getNow(null)));
				}
				// MariaDB reads a primary key row while optimizing the query, so the blocked SELECT ... FOR UPDATE can wait in
				// the "Statistics" state without a LOCK WAIT transaction
				try (ResultSet resultSet = statement.executeQuery("SELECT (SELECT COUNT(*) FROM information_schema.INNODB_TRX WHERE trx_state='LOCK WAIT') + (SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE COMMAND='Query' AND INFO LIKE '%for update' AND ID<>CONNECTION_ID())")) {
					resultSet.next();
					if (resultSet.getInt(1) > 0) {
						return;
					}
				}
				Thread.sleep(20);
			}
		}
		StringBuilder diagnostics = new StringBuilder();
		try (Connection connection = DriverManager.getConnection(session.run().configuredJdbcUrl(), "root", ""); Statement statement = connection.createStatement()) {
			for (String query : new String[] { "SELECT trx_id, trx_state, trx_query FROM information_schema.INNODB_TRX", "SELECT ID, COMMAND, STATE, INFO FROM information_schema.PROCESSLIST WHERE DB IS NOT NULL" }) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					while (resultSet.next()) {
						diagnostics.append(resultSet.getString(1)).append(" | ").append(resultSet.getString(2)).append(" | ").append(resultSet.getString(3)).append(" | ").append(query.contains("PROCESSLIST") ? resultSet.getString(4) : "").append('\n');
					}
				}
			}
		}
		throw new AssertionError("The second transaction never waited for the row lock:\n" + diagnostics);
	}

	@Test
	void ofTwoConcurrentConsumptionsOnlyOneRunsTheAction() throws Exception {
		YadaRegistrationRequest request = issueRecovery(TestDataService.USER_EMAIL);
		ParsedSecurityToken token = parsedTokenOf(request);
		CountDownLatch firstHoldsTheLock = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CompletableFuture<Optional<String>> first = CompletableFuture.supplyAsync(() -> registrationRequestDao.consume(token, YadaRegistrationType.PASSWORD_RECOVERY, YadaRegistrationRequest.class, locked -> {
			firstHoldsTheLock.countDown();
			try {
				assertTrue(releaseFirst.await(30, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			return "first";
		}), executor);
		assertTrue(firstHoldsTheLock.await(20, TimeUnit.SECONDS));
		CompletableFuture<Optional<String>> second = CompletableFuture.supplyAsync(() -> registrationRequestDao.consume(token, YadaRegistrationType.PASSWORD_RECOVERY, YadaRegistrationRequest.class, locked -> "second"), executor);
		awaitLockWait(second);
		releaseFirst.countDown();
		assertEquals(Optional.of("first"), first.get(30, TimeUnit.SECONDS));
		assertEquals(Optional.empty(), second.get(30, TimeUnit.SECONDS));
		assertEquals(0, requestRows());
	}

	@Test
	void ofTwoConcurrentAutologinsOnlyOneLogsIn() throws Exception {
		YadaUserCredentials user = userCredentialsDao.findFirstByUsername(TestDataService.USER_EMAIL);
		YadaAutoLoginToken autoLoginToken = tokenHandler.makeAutoLoginToken(user);
		ParsedSecurityToken token = codec.parse(codec.format(autoLoginToken.getId(), autoLoginToken.getRawSecret()));
		CountDownLatch firstConsumed = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CompletableFuture<YadaUserCredentials> first = CompletableFuture.supplyAsync(() -> transactionTemplate.execute(status -> {
			YadaUserCredentials consumed = autoLoginTokenDao.consume(token);
			firstConsumed.countDown();
			try {
				assertTrue(releaseFirst.await(30, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			return consumed;
		}), executor);
		assertTrue(firstConsumed.await(20, TimeUnit.SECONDS));
		assertEquals(1, session.fixtures().countRows("YadaAutoLoginToken"), "The first consumption is already committed");
		CompletableFuture<YadaUserCredentials> second = CompletableFuture.supplyAsync(() -> autoLoginTokenDao.consume(token), executor);
		awaitLockWait(second);
		releaseFirst.countDown();
		assertNotNull(first.get(30, TimeUnit.SECONDS));
		assertNull(second.get(30, TimeUnit.SECONDS));
		assertEquals(0, session.fixtures().countRows("YadaAutoLoginToken"));
	}

	@Test
	void aRollbackDuringThePasswordChangeKeepsTheTokenUsable() {
		YadaRegistrationRequest request = issueRecovery(TestDataService.USER_EMAIL);
		ParsedSecurityToken token = parsedTokenOf(request);
		String passwordBefore = userCredentialsDao.findFirstByUsername(TestDataService.USER_EMAIL).getPassword();
		assertThrows(IllegalStateException.class, () -> registrationRequestDao.consume(token, YadaRegistrationType.PASSWORD_RECOVERY, YadaRegistrationRequest.class, locked -> {
			YadaUserCredentials user = userCredentialsDao.findFirstByUsername(locked.getEmail());
			userCredentialsDao.changePassword(user, "changed-password");
			throw new IllegalStateException("Failure after the password change");
		}));
		assertEquals(passwordBefore, userCredentialsDao.findFirstByUsername(TestDataService.USER_EMAIL).getPassword(), "The password change was not rolled back");
		assertEquals(1, requestRows(), "The token was consumed by a failed change");
		assertEquals(Optional.of("ok"), registrationRequestDao.consume(token, YadaRegistrationType.PASSWORD_RECOVERY, YadaRegistrationRequest.class, locked -> "ok"));
	}

	@Test
	void aTokenIsConsumedOnlyForItsPurpose() {
		YadaRegistrationRequest request = issueRecovery(TestDataService.USER_EMAIL);
		ParsedSecurityToken token = parsedTokenOf(request);
		for (YadaRegistrationType purpose : new YadaRegistrationType[] { YadaRegistrationType.REGISTRATION, YadaRegistrationType.EMAIL_CHANGE, YadaRegistrationType.SOCIAL_REGISTRATION }) {
			assertEquals(Optional.empty(), registrationRequestDao.consume(token, purpose, YadaRegistrationRequest.class, locked -> "used"));
			assertNull(registrationRequestDao.findValid(token, purpose, YadaRegistrationRequest.class));
		}
		assertEquals(1, requestRows());
		assertNotNull(registrationRequestDao.findValid(token, YadaRegistrationType.PASSWORD_RECOVERY, YadaRegistrationRequest.class));
	}

	@Test
	void reloadedTokensCannotProduceLinks() {
		YadaRegistrationRequest request = issueRecovery(TestDataService.USER_EMAIL);
		ParsedSecurityToken token = parsedTokenOf(request);
		YadaRegistrationRequest reloaded = registrationRequestDao.findValid(token, YadaRegistrationType.PASSWORD_RECOVERY, YadaRegistrationRequest.class);
		assertNull(reloaded.getRawSecret());
		assertThrows(YadaInvalidUsageException.class, () -> tokenHandler.makeLink(reloaded, null));
		assertFalse(reloaded.toString().contains(token.secret()));
		assertFalse(reloaded.toString().contains(reloaded.getTokenHash()));

		YadaUserCredentials user = userCredentialsDao.findFirstByUsername(TestDataService.USER_EMAIL);
		YadaAutoLoginToken issued = tokenHandler.makeAutoLoginToken(user);
		assertNotNull(tokenHandler.makeAutologinLink(issued, "/", null, "http://localhost"));
		YadaAutoLoginToken reloadedAutoLogin = autoLoginTokenDao.findValid(codec.parse(codec.format(issued.getId(), issued.getRawSecret())));
		assertNull(reloadedAutoLogin.getRawSecret());
		assertThrows(YadaInvalidUsageException.class, () -> tokenHandler.makeAutologinLink(reloadedAutoLogin, "/", null, "http://localhost"));
		assertFalse(reloadedAutoLogin.toString().contains(issued.getRawSecret()));
	}

	@Test
	void existingRequestsCannotBeIssuedOrSavedAgain() {
		YadaRegistrationRequest request = issueRecovery(TestDataService.USER_EMAIL);
		assertThrows(YadaInvalidUsageException.class, () -> registrationRequestDao.issue(request));
		@SuppressWarnings("deprecation")
		Runnable save = () -> registrationRequestDao.save(request);
		assertThrows(YadaInvalidUsageException.class, save::run);
		YadaRegistrationRequest withoutType = new YadaRegistrationRequest();
		withoutType.setEmail("x@yts.test");
		withoutType.setPassword("unused");
		assertThrows(YadaInvalidUsageException.class, () -> registrationRequestDao.issue(withoutType));
	}

	@Test
	void autologinTokensMustExpireAndLeadToLocalPaths() {
		YadaUserCredentials user = userCredentialsDao.findFirstByUsername(TestDataService.USER_EMAIL);
		assertThrows(YadaInvalidUsageException.class, () -> tokenHandler.makeAutoLoginToken(user, null));
		YadaAutoLoginToken issued = tokenHandler.makeAutoLoginToken(user, new Date(System.currentTimeMillis() + 60_000));
		for (String action : new String[] { "https://evil.example", "//evil.example", "/\\evil.example", "javascript:alert(1)", "/%2F%2Fevil.example" }) {
			assertThrows(YadaInvalidUsageException.class, () -> tokenHandler.makeAutologinLink(issued, action, null, "http://localhost"), action);
		}
		String link = tokenHandler.makeAutologinLink(issued, "/dashboard?tab=users", "#top", "http://localhost");
		assertTrue(link.matches("http://localhost/autologin/" + issued.getId() + "-[0-9a-f]{64}\\?action=%2Fdashboard%3Ftab%3Dusers#top"), link);
	}
}
