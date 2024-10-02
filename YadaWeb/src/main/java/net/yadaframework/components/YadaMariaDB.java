package net.yadaframework.components;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

/**
 * Allows reopening of an existing MariaDB data folder
 */
public class YadaMariaDB extends DB {

	protected YadaMariaDB(DBConfiguration config) {
		super(config);
	}

    /**
     * This factory method is the mechanism for opening an existing embedded database for use. This
     * method assumes that the database has already been prepared for use.
     *
     * @param config Configuration of the embedded instance
     * @return a new DB instance
     * @throws ManagedProcessException if something fatal went wrong
     */
    public static DB openEmbeddedDB(DBConfiguration config) throws ManagedProcessException {
    	YadaMariaDB db = new YadaMariaDB(config);
        db.prepareDirectories();
        return db;
    }

    /**
     * This factory method is the mechanism for opening an existing embedded database for use. This
     * method assumes that the database has already been prepared for use with default
     * configuration, allowing only for specifying port.
     *
     * @param port the port to start the embedded database on
     * @return a new DB instance
     * @throws ManagedProcessException if something fatal went wrong
     */
    public static DB openEmbeddedDB(int port) throws ManagedProcessException {
        DBConfigurationBuilder config = DBConfigurationBuilder.newBuilder();
        config.setPort(port);
        return openEmbeddedDB(config.build());
    }
}
