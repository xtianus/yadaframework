package net.yadaframework.core;

import org.vibur.dbcp.ViburDBCPDataSource;
import org.vibur.dbcp.pool.TakenConnection;

/**
 * ViburDBCPDataSource that keeps the original pool validation error visible on startup failures.
 * When connection tracking is enabled and {@link #start()} fails before the pool is created
 * (for example on invalid pool settings), Vibur 26.0 calls {@code terminate()}, whose unconditional
 * {@link #getTakenConnections()} call throws a NullPointerException that hides the original
 * IllegalArgumentException. This override returns an empty array when there is no pool to inspect.
 */
public class YadaViburDBCPDataSource extends ViburDBCPDataSource {

	@Override
	public TakenConnection[] getTakenConnections() {
		if (!isPoolEnableConnectionTracking() || getPool() == null) {
			return new TakenConnection[0];
		}
		return super.getTakenConnections();
	}
}
