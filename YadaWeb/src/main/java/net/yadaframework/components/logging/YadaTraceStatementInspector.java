package net.yadaframework.components.logging;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.MDC;

/**
 * Appends MDC-based trace metadata as SQL comments for slow log correlation.
 * Enabled by {@link net.yadaframework.core.YadaJpaConfig} when yadaLogDbStats is true,
 * so statements can be linked back to a request during capture windows.
 * Initialized by setting {@code hibernate.session_factory.statement_inspector} to this class.
 */
public class YadaTraceStatementInspector implements StatementInspector {
	private static final long serialVersionUID = 1L;

	@Override
	public String inspect(String sql) {
		if (sql == null || sql.isEmpty()) {
			return sql;
		}
		String traceId = sanitize(MDC.get("traceId"));
		if (traceId == null || traceId.isBlank()) {
			return sql;
		}
		StringBuilder comment = new StringBuilder(" /* traceId=").append(traceId).append(" */");
		if (sql.endsWith(";")) {
			return sql.substring(0, sql.length() - 1) + comment + ";";
		}
		return sql + comment;
	}

	private String sanitize(String value) {
		if (value == null) {
			return null;
		}
		return value.replace("/*", "")
			.replace("*/", "")
			.replace("\r", " ")
			.replace("\n", " ");
	}
}
