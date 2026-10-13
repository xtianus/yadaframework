package net.yadaframework.example.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Chooses the folders and the port of the embedded MariaDB of one test JVM and publishes them as the system properties
 * read by src/test/resources/conf.webapp.test.xml.
 * <p>
 * The run folder is {@code <yex.buildDir>/test-db/<pid>} (the Gradle "test" task passes the YadaExamples build folder,
 * stops leftover database processes of earlier runs and deletes "test-db" before running), so each run starts from an
 * empty schema and concurrent runs, also from other checkouts, never share files. The database files are in
 * {@code db-<n>} below it. The port is a free one, never the default MySQL/MariaDB or Yada embedded port; when another
 * process takes it before MariaDB binds it, {@link #renewDatabaseAllocation()} gives a new port and database folder.
 * MariaDB4j stops the database process with a JVM shutdown hook.
 */
public final class ExampleTestDatabase {
	public static final String RUN_DIR_PROPERTY = "yex.test.runDir";
	public static final String DATABASE_DIR_PROPERTY = "yex.test.databaseDir";
	public static final String PORT_PROPERTY = "yex.test.databasePort";
	private static final Set<Integer> FORBIDDEN_PORTS = Set.of(3306, 63306);
	private static int allocation = 0;

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
		createDirectories(runDirectory);
		System.setProperty(RUN_DIR_PROPERTY, runDirectory.toString());
		renewDatabaseAllocation();
	}

	/**
	 * Picks a new free port and a new empty database folder. The configuration reads both when the database starts.
	 */
	public static synchronized void renewDatabaseAllocation() {
		allocation++;
		Path databaseDirectory = Path.of(System.getProperty(RUN_DIR_PROPERTY)).resolve("db-" + allocation);
		createDirectories(databaseDirectory);
		System.setProperty(DATABASE_DIR_PROPERTY, databaseDirectory.toString());
		System.setProperty(PORT_PROPERTY, Integer.toString(findFreePort()));
	}

	/**
	 * @return the port currently configured for the database
	 */
	public static int databasePort() {
		return Integer.parseInt(System.getProperty(PORT_PROPERTY));
	}

	/**
	 * @param port a local port
	 * @return true when something accepts connections on it
	 */
	public static boolean isListening(int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 500);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private static void createDirectories(Path directory) {
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't create the test database folder " + directory, e);
		}
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
