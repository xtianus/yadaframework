package net.yadaframework.security;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.yadaframework.tests.support.TestApplication;

/**
 * Plain JDBC access to the schema of the test application, outside the application transactions, so the tests see
 * only committed data.
 */
final class AuthDb {
	private final TestApplication application;

	AuthDb(TestApplication application) {
		this.application = application;
	}

	Connection connect() throws SQLException {
		return DriverManager.getConnection(application.run().configuredJdbcUrl(), "root", "");
	}

	/**
	 * @return the rows of the query, as column label to value maps
	 */
	List<Map<String, Object>> rows(String sql, Object... parameters) throws SQLException {
		try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int i = 0; i < parameters.length; i++) {
				statement.setObject(i + 1, parameters[i]);
			}
			List<Map<String, Object>> result = new ArrayList<>();
			try (ResultSet resultSet = statement.executeQuery()) {
				ResultSetMetaData metaData = resultSet.getMetaData();
				while (resultSet.next()) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (int column = 1; column <= metaData.getColumnCount(); column++) {
						row.put(metaData.getColumnLabel(column), resultSet.getObject(column));
					}
					result.add(row);
				}
			}
			return result;
		}
	}

	int update(String sql, Object... parameters) throws SQLException {
		try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int i = 0; i < parameters.length; i++) {
				statement.setObject(i + 1, parameters[i]);
			}
			return statement.executeUpdate();
		}
	}

	long count(String sql, Object... parameters) throws SQLException {
		Object value = rows(sql, parameters).get(0).values().iterator().next();
		return ((Number) value).longValue();
	}

	/**
	 * @return the text of every value of every row of a table, for "no secret stored" checks
	 */
	String tableText(String table) throws SQLException {
		StringBuilder text = new StringBuilder();
		for (Map<String, Object> row : rows("SELECT * FROM `" + table + "`")) {
			text.append(row).append('\n');
		}
		return text.toString();
	}
}
