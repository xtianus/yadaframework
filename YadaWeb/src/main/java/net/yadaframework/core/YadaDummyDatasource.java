package net.yadaframework.core;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;

public class YadaDummyDatasource implements DataSource {
	private static Logger log = LoggerFactory.getLogger(YadaDummyDatasource.class);
	
	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		log.warn("YadaDummyDatasource called");
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		log.warn("YadaDummyDatasource called");
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		log.warn("YadaDummyDatasource called");
		return false;
	}

	@Override
	public Connection getConnection() throws SQLException {
		log.warn("YadaDummyDatasource called");
		return null;
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		log.warn("YadaDummyDatasource called");
		return null;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		log.warn("YadaDummyDatasource called");
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		log.warn("YadaDummyDatasource called");
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		log.warn("YadaDummyDatasource called");
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		log.warn("YadaDummyDatasource called");
		return 0;
	}

}
