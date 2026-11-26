package net.yadaframework.tests.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

/**
 * A reference copy of a migrated embedded database, used to skip the installation and the Flyway migration
 * of every test run. Copying the files of a cleanly stopped database into the empty data folder of a run
 * is much faster than running mariadb-install-db and the baseline migration again: the framework then reuses
 * the data folder as it does for any database that is already installed.
 * <p>
 * The template is built once by booting the application into a throwaway run created in the same folder as the
 * run that needs it, then stored beside the run folders in a folder whose name contains a hash of the application
 * migrations and of the MariaDB4j version: when either changes, a new template is built. The template folder never
 * holds a database server by itself, so it survives across test runs and across Gradle invocations until the hash
 * changes or the folder is deleted.
 * <p>
 * The schema name must be constant ({@link TestRun#schemaName()}) because the copied files contain it.
 * Runs that don't migrate at startup, that load a sourceSql seed, or whose data folder is not empty (a restart),
 * don't use the template.
 */
final class TemplateDatabase {
	private static final Logger log = LoggerFactory.getLogger(TemplateDatabase.class);
	private static final String TEMPLATE_PREFIX = "template-db-";
	private static final String STAGING_INFIX = ".tmp-";
	private static final String MARKER_FILE = "complete.txt";
	/** Bump when the template content changes for reasons the hash can't see */
	private static final int FORMAT_VERSION = 2;
	/** Templates that could not be built in this JVM: not retried, so that a repeatable failure doesn't double every start */
	private static final Set<Path> failedTemplates = ConcurrentHashMap.newKeySet();

	private static boolean buildingTemplate = false;
	private static String templateHash = null;

	private TemplateDatabase() {
	}

	/**
	 * Fills the empty data folder of the run with the template files, building the template first when missing.
	 * Does nothing when the template can't be built or copied: the run then installs and migrates as usual.
	 * @param run the run whose application is about to start
	 */
	static void prepareDataDirectory(TestRun run) {
		// A sourceSql seed is loaded only when the schema is created, which never happens on a copied database
		if (!run.migrationAtStartup() || run.sourceSql() != null || buildingTemplate || !isEmpty(run.dataDirectory())) {
			return;
		}
		try {
			Path template = templateDirectory(run);
			if (failedTemplates.contains(template)) {
				return;
			}
			if (!isComplete(template)) {
				synchronized (TemplateDatabase.class) {
					if (!isComplete(template)) {
						buildTemplateOrRememberFailure(template);
					}
				}
			}
			if (isComplete(template)) {
				copyContents(template.resolve("datadir"), run.dataDirectory());
				copyContents(template.resolve("basedir"), run.databaseBaseDirectory());
				log.info("Test database of {} copied from the template in {}", run, template);
			}
		} catch (Exception e) {
			log.warn("Test database template not used: " + e, e);
			emptyDataDirectory(run);
		}
	}

	private static void buildTemplateOrRememberFailure(Path template) throws IOException {
		try {
			buildTemplate(template);
		} catch (IOException | RuntimeException e) {
			failedTemplates.add(template);
			throw e;
		}
		if (!isComplete(template)) {
			// The publication failed, e.g. the folder can't be renamed: building again would fail the same way
			failedTemplates.add(template);
			log.warn("Test database template {} could not be published: the runs of this JVM install and migrate their database", template);
		}
	}

	/**
	 * Boots the application once into a throwaway run beside the template, then publishes its cleanly stopped
	 * data folder as the template
	 */
	private static void buildTemplate(Path template) throws IOException {
		long startNanos = System.nanoTime();
		buildingTemplate = true;
		log.info("Building the test database template {}: the first application start installs and migrates a throwaway database", template);
		try {
			deleteAbandonedStagingFolders(template.getParent());
			try (TestRun templateRun = TestRun.createIn(template.getParent())) {
				try (TestApplication application = TestApplication.start(templateRun)) {
					// The close of the application stops the database cleanly, making its files safe to copy
				}
				publish(templateRun, template);
			}
		} finally {
			buildingTemplate = false;
		}
		log.info("Test database template built in {} ms", (System.nanoTime() - startNanos) / 1_000_000);
	}

	/**
	 * Copies the data folder of the closed template run into the template folder, atomically: other JVMs either
	 * see the complete template or nothing. When another JVM has published first, this copy is discarded.
	 */
	private static void publish(TestRun templateRun, Path target) throws IOException {
		if (isComplete(target)) {
			return;
		}
		Path staging = target.resolveSibling(target.getFileName() + STAGING_INFIX + ProcessHandle.current().pid());
		deleteRecursively(staging);
		// The binaries are copied too: YadaMariaDB unpacks them only when the data folder is empty,
		// so a run whose data folder comes from the template would otherwise have no mariadbd to start
		copyRecursively(templateRun.dataDirectory(), staging.resolve("datadir"));
		copyRecursively(templateRun.databaseBaseDirectory(), staging.resolve("basedir"));
		Files.writeString(staging.resolve(MARKER_FILE), templateHash(), StandardCharsets.UTF_8);
		try {
			Files.move(staging, target);
		} catch (IOException e) {
			// Another JVM published first, or the rename is not supported: the template is simply not shared
			deleteRecursively(staging);
		}
	}

	/**
	 * Deletes the staging folders left by JVMs that were killed while publishing a template
	 */
	private static void deleteAbandonedStagingFolders(Path runsParent) {
		if (!Files.isDirectory(runsParent)) {
			return;
		}
		try (Stream<Path> entries = Files.list(runsParent)) {
			for (Path entry : entries.toList()) {
				String name = entry.getFileName().toString();
				int infixPosition = name.lastIndexOf(STAGING_INFIX);
				if (!name.startsWith(TEMPLATE_PREFIX) || infixPosition < 0) {
					continue;
				}
				try {
					long pid = Long.parseLong(name.substring(infixPosition + STAGING_INFIX.length()));
					if (ProcessHandle.of(pid).isEmpty()) {
						deleteRecursively(entry);
						log.info("Deleted the abandoned template staging folder {}", entry);
					}
				} catch (NumberFormatException e) {
					// Not a staging folder of ours
				}
			}
		} catch (IOException e) {
			log.warn("Can't delete the abandoned template staging folders in {}", runsParent, e);
		}
	}

	/**
	 * @return the template folder beside the run folders, named after the template hash so that a migration
	 * or MariaDB4j change builds a new template instead of reusing a stale one
	 */
	private static Path templateDirectory(TestRun run) {
		return run.directory().getParent().resolve(TEMPLATE_PREFIX + templateHash());
	}

	private static boolean isComplete(Path template) {
		return Files.isRegularFile(template.resolve(MARKER_FILE));
	}

	private static boolean isEmpty(Path directory) {
		if (!Files.isDirectory(directory)) {
			return true;
		}
		try (Stream<Path> entries = Files.list(directory)) {
			return entries.findFirst().isEmpty();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @return a hash of the application migration files and of the MariaDB4j version, computed once per JVM
	 */
	private static synchronized String templateHash() {
		if (templateHash == null) {
			templateHash = computeTemplateHash();
		}
		return templateHash;
	}

	/**
	 * The MariaDB version and the MariaDB4j jar are included because the template holds the MariaDB binaries
	 * that the runs start: without them, an upgrade would keep running the old server from a surviving template.
	 */
	private static String computeTemplateHash() {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update((byte) FORMAT_VERSION);
			DBConfigurationBuilder mariaDB4jConfiguration = DBConfigurationBuilder.newBuilder();
			String mariaDB4jJar = Path.of(DB.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName().toString();
			digest.update((mariaDB4jConfiguration.getDatabaseVersion() + "|" + mariaDB4jConfiguration.getOS() + "|" + mariaDB4jJar).getBytes(StandardCharsets.UTF_8));
			Path migrations = TestRun.buildDirectory().resolve("resources/main/database");
			List<Path> files = Files.isDirectory(migrations) ? listMigrationFiles(migrations) : List.of();
			for (Path file : files) {
				digest.update(file.getFileName().toString().getBytes(StandardCharsets.UTF_8));
				digest.update(Files.readAllBytes(file));
			}
			return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
		} catch (Exception e) {
			throw new IllegalStateException("Can't compute the test database template hash", e);
		}
	}

	private static List<Path> listMigrationFiles(Path migrations) throws IOException {
		try (Stream<Path> files = Files.list(migrations)) {
			return files.filter(file -> file.getFileName().toString().startsWith("V")).sorted().toList();
		}
	}

	private static void copyContents(Path source, Path target) throws IOException {
		try (Stream<Path> entries = Files.list(source)) {
			for (Path entry : entries.toList()) {
				copyRecursively(entry, target.resolve(entry.getFileName().toString()));
			}
		}
	}

	private static void copyRecursively(Path source, Path target) throws IOException {
		if (Files.isDirectory(source)) {
			Files.createDirectories(target);
			try (Stream<Path> entries = Files.list(source)) {
				for (Path entry : entries.toList()) {
					copyRecursively(entry, target.resolve(entry.getFileName().toString()));
				}
			}
		} else {
			Files.copy(source, target);
		}
	}

	/**
	 * Empties the data folder after a failed copy, so that the database installs normally instead of
	 * opening a half-copied data folder
	 */
	private static void emptyDataDirectory(TestRun run) {
		try {
			deleteContents(run.dataDirectory());
			deleteContents(run.databaseBaseDirectory());
		} catch (IOException e) {
			log.warn("Can't empty the data folder after a failed template copy", e);
		}
	}

	private static void deleteContents(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			return;
		}
		try (Stream<Path> entries = Files.list(directory)) {
			for (Path entry : entries.toList()) {
				deleteRecursively(entry);
			}
		}
	}

	private static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		if (Files.isDirectory(path)) {
			deleteContents(path);
		}
		Files.delete(path);
	}
}
