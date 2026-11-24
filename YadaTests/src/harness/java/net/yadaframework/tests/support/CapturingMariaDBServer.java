package net.yadaframework.tests.support;

import net.yadaframework.components.YadaMariaDBServer;
import net.yadaframework.core.YadaConfiguration;

/**
 * Test replacement of the "yadaMariaDBServer" bean that records itself in {@link OwnedDatabases} as soon as it is constructed.
 * A constructor failure is cleaned up by {@link YadaMariaDBServer} itself.
 */
public class CapturingMariaDBServer extends YadaMariaDBServer {

	public CapturingMariaDBServer(YadaConfiguration config) {
		super(config);
		OwnedDatabases.register(this);
		if (config.isUseEmbeddedDatabase() && getPort() == null) {
			throw new IllegalStateException("The embedded database is enabled but has no port: refusing to fall back to the configured url");
		}
	}
}
