package net.yadaframework.tests.support;

import java.util.Objects;
import java.util.function.Function;

/**
 * A {@link TestRun} with its running {@link TestApplication} and {@link DatabaseFixtures}, for test classes that share
 * one application. Closing it stops the application and then deletes the run. When the application does not stop
 * cleanly the run folder is kept, with its logs and data, for diagnosis.
 */
public final class ApplicationSession implements AutoCloseable {
	private final TestRun run;
	private final TestApplication application;
	private final DatabaseFixtures fixtures;

	private ApplicationSession(TestRun run, TestApplication application, DatabaseFixtures fixtures) {
		this.run = run;
		this.application = application;
		this.fixtures = fixtures;
	}

	/**
	 * Starts the application on a new run and resets its data.
	 * @param seederFactory gives the seeder of the started application, e.g. {@code app -> app.getBean(TestDataService.class)::seedDefaults}
	 * @return the running session
	 */
	public static ApplicationSession start(Function<TestApplication, Runnable> seederFactory) {
		Objects.requireNonNull(seederFactory);
		TestRun run = TestRun.create();
		TestApplication application = null;
		try {
			application = TestApplication.start(run);
			DatabaseFixtures fixtures = new DatabaseFixtures(application, seederFactory.apply(application));
			fixtures.reset();
			return new ApplicationSession(run, application, fixtures);
		} catch (RuntimeException e) {
			closeInOrder(application, run, e);
			throw e;
		}
	}

	/**
	 * Closes the application, then deletes the run only if the application closed cleanly.
	 * Every failure is added to the given exception as suppressed.
	 */
	private static void closeInOrder(TestApplication application, TestRun run, RuntimeException failure) {
		if (application != null) {
			try {
				application.close();
			} catch (RuntimeException e) {
				failure.addSuppressed(e);
				return; // Keep the run folder for diagnosis
			}
		}
		try {
			run.close();
		} catch (RuntimeException e) {
			failure.addSuppressed(e);
		}
	}

	public TestRun run() {
		return run;
	}

	public TestApplication application() {
		return application;
	}

	public DatabaseFixtures fixtures() {
		return fixtures;
	}

	@Override
	public void close() {
		IllegalStateException failure = new IllegalStateException("The application session did not close cleanly: " + run);
		closeInOrder(application, run, failure);
		if (failure.getSuppressed().length > 0) {
			throw failure;
		}
	}
}
