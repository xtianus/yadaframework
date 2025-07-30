package net.yadaframework.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaSystemException;

@Lazy // Create instance only when used
@Component("yadaMariaDBServer")
@DependsOn("config")
public class YadaMariaDBServer {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	private DB db;
	private Integer port;
	
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
			String embeddedDatabaseDataDir = config.getEmbeddedDatabaseDataDir();
			File embeddedDatabaseDataDirFile = new File(embeddedDatabaseDataDir);
			embeddedDatabaseDataDirFile.mkdirs();
			if (!embeddedDatabaseDataDirFile.canWrite()) {
				throw new YadaSystemException("Can't write embedded database to folder {}", embeddedDatabaseDataDirFile);
			}
			DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
			configBuilder.setPort(0); // Set to 0 for automatic port selection
			configBuilder.setDataDir(embeddedDatabaseDataDirFile);
			db = DB.newEmbeddedDB(configBuilder.build());
			db.start();
			port = configBuilder.getPort();
			log.info("Using embedded MariaDB on localhost:{} with folder {}", port, embeddedDatabaseDataDirFile.getAbsolutePath());
			log.info("You can login as root with empty password");
			// Initialize database when not done already
			ImmutableHierarchicalConfiguration datasourceConfig = config.getConfiguration().immutableConfigurationAt("config/database/datasource");
			String configuredJdbcUrl = datasourceConfig.getString("jdbcUrl");
			Connection connection = ensureSchema(configuredJdbcUrl);
			if (connection!=null) {
				// String configuredUser = datasourceConfig.getString("username");
				// String configuredPassword = datasourceConfig.getString("password");
				// createUser(configuredUser, configuredPassword, connection);
				connection.close();
				// If the schema has just been created, try to load the database from any configured sql
				loadDatabase();
			}
		} catch (Exception e) {
			throw new YadaSystemException("Failed to start the embedded MariaDB", e);
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
		// Creating schema
		String jdbcUrl = db.getConfiguration().getURL("test"); // Connects to the default schema
		jdbcUrl = jdbcUrl.replaceAll("^jdbc:mariadb:", "jdbc:mysql:"); // Use mysql driver
		Connection connection = DriverManager.getConnection(jdbcUrl, "root", ""); // Connecting with default root privileges
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
