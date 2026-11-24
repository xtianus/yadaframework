package net.yadaframework.tests.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.yadaframework.core.YadaConfiguration;

/**
 * Brings the application database back to a known state between scenarios.
 * <p>
 * Browser requests commit their own transactions, so a test-thread rollback can't undo them. {@link #reset()}
 * therefore empties every application table with committed statements, keeping the Flyway history, and then runs
 * the application seeder, which recreates the default data through the application DAOs. The auto-increment
 * counters are reset too, so the reseeded rows get the same ids every time.
 * <p>
 * Rows are removed with DELETE rather than TRUNCATE: TRUNCATE is a DDL statement in MariaDB and once failed with a
 * lock wait timeout while no test transaction was open. When a reset fails, the open transactions and
 * connections are added to the exception message.
 */
public final class DatabaseFixtures {
	private final TestApplication application;
	private final Runnable seeder;

	/**
	 * @param application the running application
	 * @param seeder recreates the default data, e.g. {@code testDataService::seedDefaults}
	 */
	public DatabaseFixtures(TestApplication application, Runnable seeder) {
		this.application = Objects.requireNonNull(application);
		this.seeder = Objects.requireNonNull(seeder);
	}

	/**
	 * Removes all scenario data, including rows written by browser requests and their dependent rows,
	 * then reseeds the default data.
	 */
	public void reset() {
		clear();
		seeder.run();
	}

	/**
	 * Empties every application table except the Flyway history.
	 */
	public void clear() {
		String flywayTable = application.getBean(YadaConfiguration.class).flywayTableName();
		TestRun run = application.run();
		try (Connection connection = DriverManager.getConnection(run.configuredJdbcUrl(), "root", "")) {
			List<String> tables = applicationTables(connection, run.schemaName(), flywayTable);
			try (Statement statement = connection.createStatement()) {
				// Tables are emptied as a whole, so the order of the foreign keys doesn't matter here
				statement.execute("SET FOREIGN_KEY_CHECKS=0");
				try {
					for (String table : tables) {
						statement.executeUpdate("DELETE FROM `" + table + "`");
					}
					for (String table : tables) {
						// Ignored by tables without an auto-increment column
						statement.execute("ALTER TABLE `" + table + "` AUTO_INCREMENT=1");
					}
				} finally {
					statement.execute("SET FOREIGN_KEY_CHECKS=1");
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Can't reset the database of " + run + "\n" + lockDiagnostics(run), e);
		}
	}

	/**
	 * Describes the open transactions and connections, to find who holds a lock that blocked the reset
	 */
	private static String lockDiagnostics(TestRun run) {
		StringBuilder result = new StringBuilder();
		try (Connection connection = DriverManager.getConnection(run.serverJdbcUrl(), "root", ""); Statement statement = connection.createStatement()) {
			appendRows(statement, "SELECT trx_mysql_thread_id, trx_state, trx_started, trx_rows_locked, trx_tables_locked, trx_query FROM information_schema.INNODB_TRX", "Open InnoDB transactions", result);
			appendRows(statement, "SELECT ID, COMMAND, TIME, STATE, INFO FROM information_schema.PROCESSLIST WHERE ID <> CONNECTION_ID()", "Connections", result);
		} catch (SQLException e) {
			result.append("Lock diagnostics unavailable: ").append(e.getMessage());
		}
		return result.toString();
	}

	private static void appendRows(Statement statement, String sql, String title, StringBuilder result) throws SQLException {
		result.append(title).append(":\n");
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			int columns = resultSet.getMetaData().getColumnCount();
			while (resultSet.next()) {
				result.append("  ");
				for (int column = 1; column <= columns; column++) {
					result.append(resultSet.getMetaData().getColumnLabel(column)).append('=').append(resultSet.getString(column)).append(column < columns ? ", " : "\n");
				}
			}
		}
	}

	/**
	 * @return the number of rows in a table of the application schema
	 */
	public long countRows(String table) {
		TestRun run = application.run();
		try (Connection connection = DriverManager.getConnection(run.configuredJdbcUrl(), "root", ""); Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM `" + table.replace("`", "") + "`")) {
			resultSet.next();
			return resultSet.getLong(1);
		} catch (SQLException e) {
			throw new IllegalStateException("Can't count the rows of " + table, e);
		}
	}

	private static List<String> applicationTables(Connection connection, String schema, String flywayTable) throws SQLException {
		List<String> tables = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME")) {
			statement.setString(1, schema);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					String table = resultSet.getString(1);
					if (!table.equalsIgnoreCase(flywayTable)) {
						tables.add(table);
					}
				}
			}
		}
		return tables;
	}
}
