package net.yadaframework.example.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import net.yadaframework.components.YadaMariaDBServer;
import net.yadaframework.example.support.ExampleTestDatabase;
import net.yadaframework.example.support.ExampleWebTestBase;

/**
 * When another process takes the chosen database port before MariaDB binds it, the database starts on a new port
 * in a new folder. A second database is started and stopped here; the running application keeps its own.
 */
class EmbeddedDatabaseStartupRetryTest extends ExampleWebTestBase {
	@Autowired private ApplicationContext applicationContext;

	@Test
	void startsOnANewPortWhenThePortIsTaken() throws Exception {
		String previousPort = System.getProperty(ExampleTestDatabase.PORT_PROPERTY);
		String previousDirectory = System.getProperty(ExampleTestDatabase.DATABASE_DIR_PROPERTY);
		TestRootConfig.EmbeddedDatabaseStartupRetry retry = new TestRootConfig.EmbeddedDatabaseStartupRetry();
		retry.setBeanFactory(applicationContext);
		YadaMariaDBServer server = null;
		try {
			ExampleTestDatabase.renewDatabaseAllocation();
			int takenPort = ExampleTestDatabase.databasePort();
			try (ServerSocket intruder = new ServerSocket(takenPort, 1, InetAddress.getLoopbackAddress())) {
				server = (YadaMariaDBServer) retry.postProcessBeforeInstantiation(YadaMariaDBServer.class, "yadaMariaDBServer");
			}
			assertNotEquals(takenPort, server.getPort());
			assertTrue(ExampleTestDatabase.isListening(server.getPort()));
		} finally {
			if (server != null) {
				server.stopOwnedDatabase();
				assertFalse(ExampleTestDatabase.isListening(server.getPort()), "The second database is still running");
			}
			System.setProperty(ExampleTestDatabase.PORT_PROPERTY, previousPort);
			System.setProperty(ExampleTestDatabase.DATABASE_DIR_PROPERTY, previousDirectory);
		}
	}
}
