package net.yadaframework.tests.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.core.YadaConfiguration;

/**
 * One isolated test run: a unique directory, a free embedded database port, a unique schema name
 * and a generated Yada configuration file that points at them.
 * <p>
 * Every invocation gets its own directory under {@code build/runs}, so concurrent and repeated runs never share state.
 * The embedded database never uses the default MySQL/MariaDB port or the default Yada embedded port.
 * Closing the run deletes its files only when the database port has been released.
 */
public final class TestRun implements AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(TestRun.class);
	/** Ports that a developer database or application may be using */
	private static final Set<Integer> FORBIDDEN_PORTS = Set.of(3306, 63306);
	private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private final String runId;
	private final Path directory;
	private final Path configurationFile;
	private final Path runConfigurationFile;
	private final Path settingsFile;
	private final Map<String, String> datasourceSettings = new LinkedHashMap<>();
	private Path databaseRoot;
	private int databasePort;
	private int allocation = 0;
	private Path sourceSql = null;
	private boolean migrationAtStartup = true;
	private boolean closed = false;
	private boolean customJdbcUrl = false;
	private String jdbcUrl = null;
	private String extraConfiguration = "";
	/** Per-run application settings, written to conf.settings.xml: element path relative to &lt;config> → values */
	private final Map<String, List<String>> settings = new LinkedHashMap<>();

	private TestRun(Path buildDirectory) throws IOException {
		this.runId = LocalDateTime.now().format(ID_FORMAT) + "_" + Long.toString(ProcessHandle.current().pid(), 36) + Integer.toString(ThreadLocalRandom.current().nextInt(36*36*36, 36*36*36*36), 36);
		this.directory = buildDirectory.toAbsolutePath().normalize().resolve("runs").resolve(runId);
		Files.createDirectories(directory);
		this.configurationFile = directory.resolve("configuration.xml");
		this.runConfigurationFile = directory.resolve("conf.run.xml");
		this.settingsFile = directory.resolve("conf.settings.xml");
		datasourceSettings.put("name", "yts" + runId);
		datasourceSettings.put("poolInitialSize", "1");
		datasourceSettings.put("poolMaxSize", "10");
		datasourceSettings.put("poolEnableConnectionTracking", "true");
		datasourceSettings.put("logQueryExecutionLongerThanMs", "2000");
		datasourceSettings.put("logStackTraceForLongQueryExecution", "false");
		datasourceSettings.put("logLargeResultSet", "500");
		datasourceSettings.put("logStackTraceForLargeResultSet", "false");
		datasourceSettings.put("includeQueryParameters", "true");
		datasourceSettings.put("statementCacheMaxSize", "50");
		allocateDatabase();
	}

	/**
	 * Creates a new run below {@code <buildDirectory>/runs/<unique-id>} with a free database port.
	 * @param buildDirectory the build directory of the YadaTests project, or any other writable directory
	 * @return the new run
	 */
	public static TestRun create(Path buildDirectory) {
		try {
			return new TestRun(buildDirectory);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't create the test run directory in " + buildDirectory, e);
		}
	}

	/**
	 * Creates a run in the YadaTests build directory, as passed by Gradle in the "yts.buildDir" system property.
	 * @return the new run
	 */
	public static TestRun create() {
		return create(buildDirectory());
	}

	/**
	 * @return the YadaTests build directory passed by Gradle, or "build" when running outside Gradle
	 */
	public static Path buildDirectory() {
		return Path.of(System.getProperty("yts.buildDir", "build")).toAbsolutePath();
	}

	/**
	 * Picks a new database port and a fresh database directory, then rewrites the configuration.
	 * Used when a startup lost the race for the previously chosen port.
	 */
	public synchronized void renewDatabaseAllocation() {
		allocateDatabase();
	}

	private void allocateDatabase() {
		allocation++;
		databaseRoot = directory.resolve("embeddedDB" + (allocation>1 ? "-" + allocation : ""));
		databasePort = findFreePort();
		writeConfiguration();
	}

	/**
	 * Finds a free unprivileged port that is not used by default database installations.
	 * Port zero can't be used because Yada rewrites the JDBC url with the configured port.
	 */
	static int findFreePort() {
		for (int attempt = 0; attempt < 50; attempt++) {
			try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
				socket.setReuseAddress(false);
				int port = socket.getLocalPort();
				if (port > 1024 && !FORBIDDEN_PORTS.contains(port)) {
					return port;
				}
			} catch (IOException e) {
				throw new UncheckedIOException("Can't allocate a free port", e);
			}
		}
		throw new IllegalStateException("No free port found");
	}

	/**
	 * Checks whether something accepts connections on the given local port.
	 * @param port the port to check
	 * @return true when a server is listening
	 */
	public static boolean isListening(int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 500);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Configures a sql file that the embedded database loads when the schema is first created.
	 * @param sourceSql the file to load, or null for none
	 */
	public synchronized void setSourceSql(Path sourceSql) {
		this.sourceSql = sourceSql == null ? null : sourceSql.toAbsolutePath();
		writeConfiguration();
	}

	/**
	 * Enables or disables the Flyway migration at application startup (enabled by default)
	 */
	public synchronized void setMigrationAtStartup(boolean migrationAtStartup) {
		this.migrationAtStartup = migrationAtStartup;
		writeConfiguration();
	}

	/**
	 * Overrides one of the Vibur datasource settings read by YadaJpaConfig, e.g. to break startup on purpose.
	 * @param name the element name inside &lt;datasource>
	 * @param value the value to write
	 */
	public synchronized void setDatasourceSetting(String name, String value) {
		datasourceSettings.put(name, value);
		writeConfiguration();
	}

	/**
	 * Writes a different datasource url in the configuration, e.g. to break startup on purpose.
	 * @param jdbcUrl the url to write, or null to omit the element
	 */
	public synchronized void setConfiguredJdbcUrl(String jdbcUrl) {
		this.customJdbcUrl = true;
		this.jdbcUrl = jdbcUrl;
		writeConfiguration();
	}

	/**
	 * Uses a specific embedded database port instead of the allocated free one, e.g. to point at another run's server.
	 * @param databasePort the port to write in the configuration
	 */
	public synchronized void setDatabasePort(int databasePort) {
		this.databasePort = databasePort;
		writeConfiguration();
	}

	/**
	 * Adds application settings to the run configuration, e.g. "&lt;yts>&lt;extraMigrationLocation>...&lt;/extraMigrationLocation>&lt;/yts>".
	 * @param xmlFragment elements to write inside &lt;config>, replacing any fragment set before
	 */
	public synchronized void setExtraConfiguration(String xmlFragment) {
		this.extraConfiguration = xmlFragment == null ? "" : xmlFragment;
		writeConfiguration();
	}

	/**
	 * Sets an application setting for this run only, without editing the shared conf.webapp.yts.xml.
	 * The settings have the highest priority: they override the run configuration and the application configuration.
	 * A setting given here replaces the whole element list of the same path in the other files, so that list settings
	 * (e.g. trusted proxies, roles) are never merged with the defaults.
	 * <p>
	 * The configuration is read when the application starts: set values before {@link TestApplication#start(TestRun)},
	 * or close the application and start it again on the same run.
	 * <p>
	 * Examples: {@code setConfigurationValue("security/autologinExpirationHours", "1")},
	 * {@code setConfigurationValue("security/trustedProxies/proxy", "127.0.0.1", "::1")},
	 * {@code setConfigurationValue("email/@throwExceptions", "true")}.
	 * @param path element names separated by "/" relative to &lt;config>; the last segment may be an attribute like "@name"
	 * @param values one value for a single element, several for a repeated element; none removes the setting
	 */
	public synchronized void setConfigurationValue(String path, String... values) {
		String normalizedPath = normalizeSettingPath(path);
		if (values == null || values.length == 0) {
			settings.remove(normalizedPath);
		} else {
			if (normalizedPath.contains("@") && values.length != 1) {
				throw new IllegalArgumentException("An attribute takes exactly one value: " + path);
			}
			settings.put(normalizedPath, List.of(values));
		}
		writeConfiguration();
	}

	/**
	 * Removes every setting set with {@link #setConfigurationValue(String, String...)}
	 */
	public synchronized void clearConfigurationValues() {
		settings.clear();
		writeConfiguration();
	}

	private static String normalizeSettingPath(String path) {
		if (path == null || !path.matches("[A-Za-z_][\\w.-]*(/[A-Za-z_][\\w.-]*)*(/@[A-Za-z_][\\w.-]*)?")) {
			throw new IllegalArgumentException("Invalid setting path (expected e.g. \"security/autologinExpirationHours\" or \"email/@throwExceptions\"): " + path);
		}
		return path;
	}

	/**
	 * Renders the settings as nested elements, merging common ancestors
	 */
	private String settingsXml() {
		SettingNode root = new SettingNode();
		for (Map.Entry<String, List<String>> setting : settings.entrySet()) {
			String[] segments = setting.getKey().split("/");
			SettingNode parent = root;
			int elementCount = segments[segments.length-1].startsWith("@") ? segments.length - 1 : segments.length;
			for (int i = 0; i < elementCount - 1; i++) {
				parent = parent.children.computeIfAbsent(segments[i], name -> new SettingNode());
			}
			if (elementCount < segments.length) {
				// Attribute of the last element
				parent = parent.children.computeIfAbsent(segments[elementCount-1], name -> new SettingNode());
				parent.attributes.put(segments[segments.length-1].substring(1), setting.getValue().get(0));
			} else {
				SettingNode leaf = parent.children.computeIfAbsent(segments[elementCount-1], name -> new SettingNode());
				leaf.values = setting.getValue();
			}
		}
		StringBuilder xml = new StringBuilder("<config>\n");
		root.appendChildren(xml, "\t");
		return xml.append("</config>\n").toString();
	}

	private static final class SettingNode {
		private final Map<String, SettingNode> children = new LinkedHashMap<>();
		private final Map<String, String> attributes = new LinkedHashMap<>();
		private List<String> values = null;

		private void appendChildren(StringBuilder xml, String indent) {
			for (Map.Entry<String, SettingNode> child : children.entrySet()) {
				child.getValue().append(xml, indent, child.getKey());
			}
		}

		private void append(StringBuilder xml, String indent, String name) {
			StringBuilder openTag = new StringBuilder("<").append(name);
			for (Map.Entry<String, String> attribute : attributes.entrySet()) {
				openTag.append(" ").append(attribute.getKey()).append("=\"").append(xml(attribute.getValue())).append("\"");
			}
			if (values != null) {
				for (String value : values) {
					xml.append(indent).append(openTag).append(">").append(xml(value)).append("</").append(name).append(">\n");
				}
			} else {
				xml.append(indent).append(openTag).append(">\n");
				appendChildren(xml, indent + "\t");
				xml.append(indent).append("</").append(name).append(">\n");
			}
		}
	}

	private void writeConfiguration() {
		StringBuilder datasource = new StringBuilder();
		String writtenJdbcUrl = customJdbcUrl ? jdbcUrl : configuredJdbcUrl();
		if (writtenJdbcUrl != null) {
			datasource.append("\t\t\t<jdbcUrl>").append(xml(writtenJdbcUrl)).append("</jdbcUrl>\n");
		}
		datasource.append("\t\t\t<username>root</username>\n");
		datasource.append("\t\t\t<password></password>\n");
		for (Map.Entry<String, String> setting : datasourceSettings.entrySet()) {
			datasource.append("\t\t\t<").append(setting.getKey()).append(">").append(xml(setting.getValue())).append("</").append(setting.getKey()).append(">\n");
		}
		String runConfiguration = """
			<config>
				<info>
					<env>dev</env>
					<appName>YadaTests</appName>
					<version>%s</version>
					<build>%s</build>
				</info>
				<paths>
					<basePath>%s</basePath>
					<serverAddress>http://localhost/</serverAddress>
				</paths>
				<database enabled="true">
					<embedded enabled="true" port="%d">
						<baseDir>%s</baseDir>
						<datadir>%s</datadir>
						<tmpDir>%s</tmpDir>
			%s		</embedded>
					<databaseMigrationAtStartup>%s</databaseMigrationAtStartup>
					<showSql>false</showSql>
					<datasource>
			%s		</datasource>
				</database>
			%s
			</config>
			""".formatted(runId, runId, xml(directory.toString()), databasePort,
				xml(databaseBaseDirectory().toString()), xml(dataDirectory().toString()), xml(databaseRoot.resolve("tmp").toString()),
				sourceSql == null ? "" : "\t\t\t\t<sourceSql>" + xml(sourceSql.toString()) + "</sourceSql>\n",
				migrationAtStartup, datasource, extraConfiguration);
		// The per-run settings override the run configuration, which overrides the application test configuration found on the classpath, if any
		String configuration = """
			<?xml version="1.0" encoding="UTF-8"?>
			<configuration>
				<header>
					<result>
						<expressionEngine config-class="org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine"/>
					</result>
				</header>
				<override>
					<xml config-name="settings" config-at="config" fileName="%s"/>
					<properties fileName="net.yadaframework.yadaweb.properties" config-optional="true"/>
					<properties fileName="net.yadaframework.yadawebsecurity.properties" config-optional="true"/>
					<properties fileName="net.yadaframework.yadawebcms.properties" config-optional="true"/>
					<properties fileName="net.yadaframework.yadawebcommerce.properties" config-optional="true"/>
					<xml config-name="run" config-at="config" fileName="%s"/>
					<xml config-name="application" config-at="config" fileName="conf.webapp.yts.xml" config-optional="true"/>
				</override>
			</configuration>
			""".formatted(xml(settingsFile.toString()), xml(runConfigurationFile.toString()));
		try {
			Files.writeString(settingsFile, settingsXml(), StandardCharsets.UTF_8);
			Files.writeString(runConfigurationFile, runConfiguration, StandardCharsets.UTF_8);
			Files.writeString(configurationFile, configuration, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't write the run configuration in " + directory, e);
		}
	}

	private static String xml(String value) {
		return StringEscapeUtils.escapeXml10(value);
	}

	/**
	 * Loads the run configuration without reloading triggers, for tests that build components directly.
	 * @return a new configuration instance
	 */
	public YadaConfiguration loadConfiguration() {
		try {
			YadaConfiguration config = new YadaConfiguration() {};
			CombinedConfigurationBuilder builder = new CombinedConfigurationBuilder().configure(new Parameters().fileBased().setFile(configurationFile.toFile()));
			config.setConfiguration(builder.getConfiguration());
			return config;
		} catch (ConfigurationException e) {
			throw new IllegalStateException("Can't load the run configuration " + configurationFile, e);
		}
	}

	/** @return the unique id of this run, made of word characters only */
	public String runId() {
		return runId;
	}

	/** @return the absolute run directory */
	public Path directory() {
		return directory;
	}

	/** @return the absolute path of the generated Yada configuration file */
	public Path configurationFile() {
		return configurationFile;
	}

	/** @return the port of the embedded database */
	public synchronized int databasePort() {
		return databasePort;
	}

	/** @return the schema used by the application, e.g. "yts_20261011120000_abc123" */
	public String schemaName() {
		return "yts_" + runId;
	}

	/** @return the folder holding the MariaDB binaries, data and tmp folders */
	public synchronized Path databaseRoot() {
		return databaseRoot;
	}

	/** @return the MariaDB binaries folder */
	public synchronized Path databaseBaseDirectory() {
		return databaseRoot.resolve("MariaDB");
	}

	/** @return the MariaDB data folder */
	public synchronized Path dataDirectory() {
		return databaseRoot.resolve("data");
	}

	/** @return the JDBC url written in the configuration */
	public synchronized String configuredJdbcUrl() {
		return "jdbc:mysql://localhost:" + databasePort + "/" + schemaName() + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";
	}

	/** @return a JDBC url to the database server without a default schema */
	public synchronized String serverJdbcUrl() {
		return "jdbc:mysql://localhost:" + databasePort + "/?useSSL=false&allowPublicKeyRetrieval=true";
	}

	/**
	 * Deletes the run files, but only when the database port has been released, so that the files of a
	 * process that is still running are never removed.
	 * @throws IllegalStateException when the database port is still in use
	 */
	@Override
	public synchronized void close() {
		if (closed) {
			return;
		}
		if (isListening(databasePort)) {
			throw new IllegalStateException("The embedded database port " + databasePort + " is still in use: keeping " + directory);
		}
		closed = true;
		deleteRecursively(directory);
		if (Files.exists(directory)) {
			throw new IllegalStateException("Some files of " + this + " could not be deleted: see the log");
		}
	}

	private static void deleteRecursively(Path path) {
		if (!Files.exists(path)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(path)) {
			paths.sorted(Comparator.reverseOrder()).forEach(file -> {
				try {
					Files.deleteIfExists(file);
				} catch (IOException e) {
					log.warn("Can't delete {}", file, e);
				}
			});
		} catch (IOException e) {
			log.warn("Can't delete {}", path, e);
		}
	}

	@Override
	public String toString() {
		return "TestRun " + runId + " (" + directory + ", database port " + databasePort + ")";
	}
}
