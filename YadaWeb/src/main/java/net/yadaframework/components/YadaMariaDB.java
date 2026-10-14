package net.yadaframework.components;

import java.io.IOException;

import ch.vorburger.exec.ManagedProcess;
import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfiguration.Executable;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

/**
 * Allows reopening of an existing MariaDB data folder, and installs new ones with the configured temporary folder
 */
public class YadaMariaDB extends DB {

	protected YadaMariaDB(DBConfiguration config) {
		super(config);
	}

	/**
	 * Unpacks and installs a new embedded database, like {@link DB#newEmbeddedDB(DBConfiguration)}, but runs
	 * mariadb-install-db with the configured tmpDir: see {@link #createDBInstallProcess()}.
	 * @param config Configuration of the embedded instance
	 * @return a new installed DB instance, not started
	 * @throws ManagedProcessException if something fatal went wrong
	 */
	public static DB newEmbeddedDB(DBConfiguration config) throws ManagedProcessException {
		YadaMariaDB db = new YadaMariaDB(config);
		db.prepareDirectories();
		db.unpackEmbeddedDb();
		db.install();
		return db;
	}

	/**
	 * Copy of the MariaDB4j 3.2.0 install command with an added --tmpdir argument (check it again when upgrading MariaDB4j).
	 * <p>
	 * MariaDB4j passes --tmpdir only to the server, so the bootstrap mariadbd of mariadb-install-db uses the shared system
	 * temporary folder. mariadbd deletes every "#sql*" file of its tmpdir when it starts, and the installation keeps
	 * temporary tables there (e.g. mysql.tmp_user_sys): two installations running at the same time on one machine can delete
	 * each other's tables and fail with "ERROR: 1051 Unknown table 'mysql.tmp_user_sys'". On Windows the original command is used.
	 */
	@Override
	protected ManagedProcess createDBInstallProcess() throws ManagedProcessException, IOException {
		if (configuration.isWindows()) {
			return super.createDBInstallProcess();
		}
		ManagedProcessBuilder builder = new ManagedProcessBuilder(configuration.getExecutable(Executable.InstallDB));
		builder.setOutputStreamLogDispatcher(getOutputStreamLogDispatcher("mysql_install_db"));
		builder.getEnvironment().put(configuration.getOSLibraryEnvironmentVarName(), configuration.getLibDir().getAbsolutePath());
		builder.setWorkingDirectory(configuration.getBaseDir());
		builder.addFileArgument("--datadir", configuration.getDataDir());
		builder.addFileArgument("--basedir", configuration.getBaseDir());
		builder.addArgument("--no-defaults");
		builder.addArgument("--force");
		builder.addArgument("--skip-name-resolve");
		builder.addFileArgument("--tmpdir", configuration.getTmpDir());
		return builder.build();
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
