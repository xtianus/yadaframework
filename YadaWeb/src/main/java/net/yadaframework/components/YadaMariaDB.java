package net.yadaframework.components;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(YadaMariaDB.class);

	protected YadaMariaDB(DBConfiguration config) {
		super(config);
	}

	/**
	 * Same as {@link DB#newEmbeddedDB(DBConfiguration)} of MariaDB4j 3.3.1, but runs mariadb-install-db with the configured
	 * tmpDir: see {@link #createDBInstallProcess()}. The binaries are unpacked and the database is installed only when
	 * the data folder is empty; an existing data folder is reused.
	 * @param config Configuration of the embedded instance
	 * @return a new DB instance, not started
	 * @throws ManagedProcessException if something fatal went wrong
	 */
	public static DB newEmbeddedDB(DBConfiguration config) throws ManagedProcessException {
		YadaMariaDB db = new YadaMariaDB(config);
		db.prepareDirectories();
		try {
			Path dataDir = Paths.get(config.getDataDir().getAbsolutePath());
			if (Files.isDirectory(dataDir)) {
				try (Stream<Path> entries = Files.list(dataDir)) {
					if (entries.findFirst().isEmpty()) {
						db.unpackEmbeddedDb();
						db.install();
					}
				}
			}
		} catch (IOException e) {
			throw new ManagedProcessException(e.getMessage(), e);
		}
		return db;
	}

	/**
	 * The install command of MariaDB4j 3.3.1 with an added --tmpdir argument; check it again when upgrading MariaDB4j.
	 * <p>
	 * MariaDB4j passes --tmpdir only to the server, so the bootstrap mariadbd of mariadb-install-db uses the shared system
	 * temporary folder. mariadbd deletes every "#sql*" file of its tmpdir when it starts, and the installation keeps
	 * temporary tables there (e.g. mysql.tmp_user_sys): two installations running at the same time on one machine can delete
	 * each other's tables and fail with "ERROR: 1051 Unknown table 'mysql.tmp_user_sys'".
	 * <p>
	 * Windows is not fixed: there the original MariaDB4j command runs, and the installation keeps using the system TEMP folder.
	 */
	@Override
	protected ManagedProcess createDBInstallProcess() throws ManagedProcessException, IOException {
		if (configuration.isWindows()) {
			return super.createDBInstallProcess();
		}
		log.info("Installing a new embedded database to: {}", configuration.getBaseDir());
		return createUnixInstallProcessBuilder().build();
	}

	/**
	 * @return the builder of the non-Windows install command, see {@link #createDBInstallProcess()}
	 */
	ManagedProcessBuilder createUnixInstallProcessBuilder() throws ManagedProcessException, IOException {
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
		return builder;
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
