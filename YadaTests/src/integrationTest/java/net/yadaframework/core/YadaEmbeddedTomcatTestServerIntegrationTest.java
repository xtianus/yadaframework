package net.yadaframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.yadaframework.core.allowlist.FirstTestInitializer;
import net.yadaframework.core.allowlist.InitializerRecorder;
import net.yadaframework.core.allowlist.SecondTestInitializer;
import net.yadaframework.tests.support.TestRun;

/**
 * Initializer selection, class sharing and cleanup of the embedded Tomcat fixture, without database.
 */
class YadaEmbeddedTomcatTestServerIntegrationTest {
	private TestRun run;
	private Path webappDir;
	private Path mountedClasses;

	@BeforeEach
	void prepare() throws Exception {
		InitializerRecorder.ranAndClear();
		run = TestRun.create();
		webappDir = Files.createDirectories(run.directory().resolve("webapp/WEB-INF"));
		Files.writeString(webappDir.resolve("web.xml"), "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"6.0\" metadata-complete=\"true\"/>");
		webappDir = webappDir.getParent();
		// The folder that contains both test initializers
		mountedClasses = Path.of(FirstTestInitializer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	@AfterEach
	void cleanup() {
		run.close();
	}

	private static boolean isListening(int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Test
	void onlyTheAllowedInitializerRunsWhenSeveralShareAFolder() throws Exception {
		assertTrue(Files.exists(mountedClasses.resolve(SecondTestInitializer.class.getName().replace('.', '/') + ".class")));
		int port;
		try (YadaEmbeddedTomcatTestServer server = new YadaEmbeddedTomcatTestServer(webappDir, mountedClasses)) {
			server.setBaseDirectory(run.directory().resolve("tomcat"));
			server.setInitializerClassNames(List.of(FirstTestInitializer.class.getName()));
			server.setDelegate(true);
			server.addContextParameter(InitializerRecorder.ENABLED_PARAMETER, "true");
			server.start();
			port = server.getPort();
			assertTrue(server.isAvailable());
			// Parent-first delegation: the initializer that ran is the very class seen by the test
			assertEquals(List.of(FirstTestInitializer.class), InitializerRecorder.ranAndClear());
		}
		assertFalse(isListening(port), "The HTTP port is still open after close");
	}

	@Test
	void unknownInitializerFailsBeforeStarting() throws Exception {
		YadaEmbeddedTomcatTestServer server = new YadaEmbeddedTomcatTestServer(webappDir, mountedClasses);
		server.setBaseDirectory(run.directory().resolve("tomcat"));
		server.setInitializerClassNames(List.of("net.yadaframework.core.allowlist.Missing"));
		assertThrows(IllegalArgumentException.class, server::start);
		assertTrue(InitializerRecorder.ranAndClear().isEmpty());
		// A second close after the failed start is harmless
		server.close();
	}
}
