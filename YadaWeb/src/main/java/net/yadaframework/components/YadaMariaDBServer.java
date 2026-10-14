package net.yadaframework.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaSystemException;

/**
 * Installs, starts or attaches to the embedded MariaDB database configured in &lt;database>&lt;embedded>.
 * <p>
 * An instance owns the database process only when it started that process. An instance that attached to a process
 * that was already running does not own it and never stops it. Attaching is refused when the running server uses a
 * data directory different from the configured one, so that a port collision can't silently use, or create schemas in,
 * someone else's database.
 * <p>
 * The process is not stopped when the application context closes: it is expected to keep running between
 * application restarts during development. Use {@link #stopOwnedDatabase()} to stop it explicitly.
 */
@Lazy // Create instance only when used
@Component("yadaMariaDBServer")
@DependsOn("config")
public class YadaMariaDBServer {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	private DB db;
	private Integer port;
	private boolean owner = false; // True when this instance called start() on the process
	private boolean attached = false; // True when this instance attached to an already running process
	private boolean stopped = false;
	
	private YadaConfiguration config;
	
	@Autowired 
	public YadaMariaDBServer(YadaConfiguration config) {
		this.config = config;
		try {
			boolean enabled = config.isUseEmbeddedDatabase();
			if (!enabled) {
				log.debug("Embedded database is disabled - using installed MySQL");
				return;
			}
			port = config.getEmbeddedDatabasePort();
			String baseDir = config.getEmbeddedDatabaseBaseDir();
			String dataDir = config.getEmbeddedDatabaseDataDir();
			String tmpDir = config.getEmbeddedDatabaseTmpDir();
			File baseDirFile = new File(baseDir);
			File embeddedDatabaseDataDirFile = new File(dataDir);
			// Fail with a clear message before MariaDB4j fails with an obscure one, and before creating any folder
			checkSupportedPaths(baseDirFile, embeddedDatabaseDataDirFile, new File(tmpDir));
			embeddedDatabaseDataDirFile.mkdirs();
			if (!embeddedDatabaseDataDirFile.isDirectory() || !embeddedDatabaseDataDirFile.canWrite()) {
				throw new YadaSystemException("Can't write embedded database to folder {}", embeddedDatabaseDataDirFile);
			}
			DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
			configBuilder.setBaseDir(baseDirFile);
			configBuilder.setDataDir(embeddedDatabaseDataDirFile);
			configBuilder.setTmpDir(tmpDir);
			configBuilder.setPort(port);
			// If the base folder is empty install the db, otherwise open it
			String[] filesInBaseDir = baseDirFile.list();
			boolean install = filesInBaseDir == null || filesInBaseDir.length == 0; // No files
			if (install) {
				db = YadaMariaDB.newEmbeddedDB(configBuilder.build());
				log.info("Installing new database in {}", baseDir);
				owner = true; // Set before starting so that a partial start can be cleaned up
				db.start();
			} else {
				db = YadaMariaDB.openEmbeddedDB(configBuilder.build());
				// Check if database process is running already
				try {
					Connection connection = openConnection();
					connection.close();
					// Yes it's running.
					attached = true;
				} catch (Exception e) {
					// Not running so start it
					log.debug("Starting database process");
					owner = true; // Set before starting so that a partial start can be cleaned up
					db.start();
				}
				if (attached) {
					// Must be checked before touching the server in any way
					ensureSameDataDirectory(embeddedDatabaseDataDirFile);
					log.debug("Attaching to running database instance");
				}
			}
			//
			log.info("Using embedded MariaDB on localhost:{} with data folder {}", port, embeddedDatabaseDataDirFile.getAbsolutePath());
			log.info("Database login as root with empty password");
			// Initialize database when not done already
			ImmutableHierarchicalConfiguration datasourceConfig = config.getConfiguration().immutableConfigurationAt("config/database/datasource");
			String configuredJdbcUrl = datasourceConfig.getString("jdbcUrl");
			Connection connection = ensureSchema(configuredJdbcUrl);
			if (connection!=null) {
				// Schema has been created
				// String configuredUser = datasourceConfig.getString("username");
				// String configuredPassword = datasourceConfig.getString("password");
				// createUser(configuredUser, configuredPassword, connection);
				connection.close();
				// If the schema has just been created, try to load the database from any configured sql
				loadDatabase();
			}
		} catch (Exception e) {
			YadaSystemException failure = new YadaSystemException("Failed to start the embedded MariaDB", e);
			// Don't leave behind a process that nobody can stop because this instance will never be registered
			try {
				stopOwnedDatabase();
			} catch (Exception cleanupException) {
				failure.addSuppressed(cleanupException);
			}
			throw failure;
		}
	}

	/**
	 * Fails when the embedded database folders can't work on the current operating system.
	 * <p>
	 * On Linux and macOS, MariaDB4j (up to at least 3.3.1, through ch.vorburger.exec 3.3.2) passes folder arguments
	 * to the MariaDB programs as --datadir="/some path", and the quotes reach the program literally because no shell is
	 * involved. The installation then fails with an obscure "Could not find my_print_defaults" error. Windows quotes the
	 * whole argument instead and is not affected.
	 * The YadaTests suite verifies that the MariaDB4j limitation still exists: when that test fails, remove this check.
	 * @param folders the embedded database folders
	 * @throws YadaSystemException when a folder contains whitespace on a non-Windows system
	 */
	static void checkSupportedPaths(File... folders) {
		if (SystemUtils.IS_OS_WINDOWS) {
			return;
		}
		for (File folder : folders) {
			if (folder != null && folder.getAbsolutePath().chars().anyMatch(Character::isWhitespace)) {
				throw new YadaSystemException("The embedded database folder \"{}\" contains whitespace: MariaDB4j can't install or start MariaDB in such a folder on this operating system. "
					+ "Configure a path without whitespace in <database><embedded> (baseDir, datadir, tmpDir) or in <paths><basePath>.", folder.getAbsolutePath());
			}
		}
	}

	/**
	 * Refuses to use a running server that stores its data in a different folder than the configured one.
	 * @param configuredDataDir the configured data folder
	 * @throws YadaSystemException when the folders differ or the check can't be performed
	 */
	private void ensureSameDataDirectory(File configuredDataDir) throws SQLException {
		String runningDataDir;
		try (Connection connection = openConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT @@datadir")) {
			resultSet.next();
			runningDataDir = resultSet.getString(1);
		}
		Path expected = normalizeDirectory(configuredDataDir.getAbsolutePath());
		Path actual = runningDataDir==null ? null : normalizeDirectory(runningDataDir);
		if (!expected.equals(actual)) {
			throw new YadaSystemException("The database already running on port {} uses data directory {} instead of the configured data directory {}: refusing to attach. Configure a different port.", port, runningDataDir, expected);
		}
	}

	private Path normalizeDirectory(String directory) {
		Path path = Path.of(directory).toAbsolutePath().normalize();
		try {
			return path.toRealPath();
		} catch (IOException e) {
			return path;
		}
	}

	/**
	 * Stops the database process started by this instance. It does nothing when the process was already running and this
	 * instance just attached to it, when the embedded database is disabled, or when the process has already been stopped.
	 * The process is otherwise left running when the application stops, so call this method only when the database should
	 * really go away, e.g. in tests.
	 * @throws YadaSystemException if the process could not be stopped
	 */
	public synchronized void stopOwnedDatabase() {
		if (!owner || stopped || db==null) {
			return;
		}
		try {
			db.stop();
			stopped = true;
			log.info("Embedded MariaDB on port {} stopped", port);
		} catch (ManagedProcessException e) {
			throw new YadaSystemException("Failed to stop the embedded MariaDB on port " + port, e);
		}
	}
	
	/**
	 * Loads the database from a sql file defined in &lt;database>&lt;embedded>&lt;sourceSql>
	 */
	private void loadDatabase() {
		File sourceSqlFile = config.getEmbeddedDatabaseSourceSql();
		if (sourceSqlFile!=null) {
			try (InputStream sourceStream = new FileInputStream(sourceSqlFile)) {
				db.source(sourceStream);
				log.info("Embedded database loaded from {}", sourceSqlFile.getAbsolutePath());
				boolean renamed = sourceSqlFile.renameTo(new File(sourceSqlFile.getAbsolutePath() + ".loaded"));
				if (!renamed) {
					log.info("Can't rename {} - do it manually to prevent loading again at startup", sourceSqlFile.getAbsolutePath());
				}
			} catch (Exception e) {
				log.error("Can't load embedded database", e);
			}
		}
	}

	/**
	 * Creates the database schema when it doesn't exist
	 * @param configuredJdbcUrl
	 * @return the database Connection only if the schema was created, null otherwise.
	 * @throws SQLException
	 */
	private Connection ensureSchema(String configuredJdbcUrl) throws SQLException {
		// Extract the schema from the url
		Pattern pattern = Pattern.compile("jdbc:mysql://[^/]+/(\\w+)");
		Matcher matcher = pattern.matcher(configuredJdbcUrl);
		String configuredSchemaName = null;
		if (matcher.find()) {
			configuredSchemaName = matcher.group(1);
		}
		Connection connection = openConnection();
		// Creating schema
		try (Statement stmt = connection.createStatement()) {		    
		    stmt.executeUpdate("CREATE DATABASE " + configuredSchemaName);
		    log.debug("Database schema {} created", configuredSchemaName);
		} catch (SQLException e) {
			log.debug("Database schema {} probably exists - skipping db creation", configuredSchemaName);
			connection.close();
			return null;
		}
		return connection;
	}
	
	private Connection openConnection() throws SQLException {
		String jdbcUrl = db.getConfiguration().getURL("");
		jdbcUrl = jdbcUrl.replaceAll("^jdbc:mariadb:", "jdbc:mysql:"); // Use mysql driver
		Connection connection = DriverManager.getConnection(jdbcUrl, "root", ""); // Connecting with default root privileges
		return connection;
	}
	
	// Grant tables are disabled by default so we don't need this
	@Deprecated
	private void createUser(String configuredUser, String configuredPassword, Connection connection) throws SQLException {
		 try (Statement stmt = connection.createStatement()) {
			 log.debug("Creating user {}", configuredUser);
			 stmt.executeUpdate("CREATE USER '" + configuredUser + "'@'localhost' IDENTIFIED BY '" + configuredPassword + "'");
			 stmt.executeUpdate("GRANT ALL ON *.* TO '" + configuredUser + "'@'localhost'");
	    }		
	}

	public Integer getPort() {
		return port;
	}
}
