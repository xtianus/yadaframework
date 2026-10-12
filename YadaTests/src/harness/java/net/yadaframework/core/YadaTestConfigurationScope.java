package net.yadaframework.core;

import java.nio.file.Path;
import java.util.Objects;

import org.springframework.context.ApplicationContext;

import net.yadaframework.components.YadaUtil;

/**
 * Applies a specific Yada configuration file for the lifetime of the scope.
 * Closing the scope restores the previous configuration file property, static configuration and
 * {@link YadaUtil#applicationContext}, so that the closed application can't be reached from static code.
 */
public class YadaTestConfigurationScope implements AutoCloseable {
	private static final Object MONITOR = new Object();

	private static boolean active = false;

	private final String previousConfigurationFile;
	private final YadaConfiguration previousConfig;
	private final ApplicationContext previousApplicationContext;

	private boolean closed = false;

	/**
	 * Activates the given configuration file and clears any cached static configuration.
	 * @param configurationFile absolute path to the configuration file to use
	 */
	public YadaTestConfigurationScope(Path configurationFile) {
		Objects.requireNonNull(configurationFile, "configurationFile must not be null");
		synchronized (MONITOR) {
			if (active) {
				throw new IllegalStateException("A YadaTestConfigurationScope is already active");
			}
			active = true;
			previousConfigurationFile = System.getProperty(YadaAppConfig.CONFIGURATION_FILE_SYSTEM_PROPERTY);
			previousConfig = YadaAppConfig.CONFIG;
			previousApplicationContext = YadaUtil.applicationContext;
			System.setProperty(YadaAppConfig.CONFIGURATION_FILE_SYSTEM_PROPERTY, configurationFile.toAbsolutePath().toString());
			YadaAppConfig.CONFIG = null;
		}
	}

	@Override
	public void close() {
		synchronized (MONITOR) {
			if (closed) {
				return;
			}
			YadaConfiguration activeConfig = YadaAppConfig.CONFIG;
			if (activeConfig != null && activeConfig != previousConfig) {
				activeConfig.stopReloadingTrigger();
			}
			if (previousConfigurationFile == null) {
				System.clearProperty(YadaAppConfig.CONFIGURATION_FILE_SYSTEM_PROPERTY);
			} else {
				System.setProperty(YadaAppConfig.CONFIGURATION_FILE_SYSTEM_PROPERTY, previousConfigurationFile);
			}
			YadaAppConfig.CONFIG = previousConfig;
			YadaUtil.applicationContext = previousApplicationContext;
			active = false;
			closed = true;
		}
	}
}
