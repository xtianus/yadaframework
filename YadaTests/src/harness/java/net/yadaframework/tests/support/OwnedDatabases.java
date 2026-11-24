package net.yadaframework.tests.support;

import java.util.ArrayList;
import java.util.List;

import net.yadaframework.components.YadaMariaDBServer;

/**
 * Keeps the embedded database components created by test applications outside any Spring context,
 * so that a failed context refresh can't lose the handle needed to stop an owned database process.
 */
public final class OwnedDatabases {
	private static final List<YadaMariaDBServer> servers = new ArrayList<>();

	private OwnedDatabases() {
	}

	/**
	 * Records a constructed database component.
	 * @param server the component, which may or may not own its process
	 */
	public static synchronized void register(YadaMariaDBServer server) {
		servers.add(server);
	}

	/**
	 * @return the components recorded so far, without removing them
	 */
	public static synchronized List<YadaMariaDBServer> registered() {
		return List.copyOf(servers);
	}

	/**
	 * Stops every recorded component that owns its process and forgets the ones that stopped.
	 * A component that fails to stop stays recorded, so that a later call can try again.
	 * Continues past individual failures and reports them together.
	 * @throws IllegalStateException carrying each stop failure as suppressed exception
	 */
	public static synchronized void stopAll() {
		IllegalStateException failure = null;
		for (YadaMariaDBServer server : new ArrayList<>(servers)) {
			try {
				server.stopOwnedDatabase();
				servers.remove(server);
			} catch (RuntimeException e) {
				if (failure == null) {
					failure = new IllegalStateException("Failed to stop an owned embedded database");
				}
				failure.addSuppressed(e);
			}
		}
		if (failure != null) {
			throw failure;
		}
	}
}
