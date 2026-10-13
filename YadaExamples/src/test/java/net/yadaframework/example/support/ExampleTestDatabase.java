package net.yadaframework.example.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Chooses the folder and the port of the embedded MariaDB of one test JVM and publishes them as the system properties
 * read by src/test/resources/conf.webapp.test.xml.
 * <p>
 * The folder is {@code <yex.buildDir>/test-db/<pid>} (the Gradle "test" task passes the YadaExamples build folder and
 * deletes "test-db" before running), so each run starts from an empty schema and concurrent runs, also from other
 * checkouts, never share files. The port is a free one, never the default MySQL/MariaDB or Yada embedded port.
 * MariaDB4j stops the database process with a JVM shutdown hook.
 */
public final class ExampleTestDatabase {
	public static final String RUN_DIR_PROPERTY = "yex.test.runDir";
	public static final String PORT_PROPERTY = "yex.test.databasePort";
	private static final Set<Integer> FORBIDDEN_PORTS = Set.of(3306, 63306);

	private ExampleTestDatabase() {
	}

	/**
	 * Sets the folder and port properties once per JVM. Must run before the Yada configuration is first read.
	 */
	public static synchronized void prepare() {
		if (System.getProperty(PORT_PROPERTY) != null) {
			return;
		}
		Path buildDirectory = Path.of(System.getProperty("yex.buildDir", "build")).toAbsolutePath().normalize();
		Path runDirectory = buildDirectory.resolve("test-db").resolve(Long.toString(ProcessHandle.current().pid()));
		try {
			Files.createDirectories(runDirectory);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't create the test database folder " + runDirectory, e);
		}
		System.setProperty(RUN_DIR_PROPERTY, runDirectory.toString());
		System.setProperty(PORT_PROPERTY, Integer.toString(findFreePort()));
	}

	/**
	 * Port zero can't be used because Yada writes the configured port in the JDBC url
	 */
	private static int findFreePort() {
		for (int attempt = 0; attempt < 50; attempt++) {
			try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
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
}
