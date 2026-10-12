package net.yadaframework.core;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Starts an embedded Tomcat instance for servlet integration tests without blocking the JVM.
 * <p>
 * By default the web application initializers found in the mounted classpath roots are run. Use
 * {@link #setInitializerClassNames(Collection)} to run exactly the listed ones instead, and {@link #setDelegate(boolean)}
 * to load every class from the test classpath first, so that test code and the application share the same classes.
 */
public class YadaEmbeddedTomcatTestServer implements AutoCloseable {
	private final Tomcat tomcat = new Tomcat();
	private final String contextPath;
	private final Path webappDir;
	private final List<Path> extraClasspathRoots;
	private final Map<String, String> contextParameters = new LinkedHashMap<>();

	private Path baseDir;
	private boolean deleteBaseDir = true;
	private List<String> initializerClassNames = null;
	private boolean delegate = false;
	private StandardContext context;
	private volatile Throwable rootContextFailure = null;
	/** Tomcat sets these JVM-wide properties to its base directory: they are restored on close */
	private static final List<String> CATALINA_PROPERTIES = List.of("catalina.base", "catalina.home");
	private final Map<String, String> previousCatalinaProperties = new LinkedHashMap<>();
	private boolean tomcatConfigured = false; // Tomcat has its base directory and can be stopped
	private boolean started = false;
	private boolean closed = false;

	/**
	 * Creates a server mounted at the root context path.
	 * @param webappDir webapp directory containing WEB-INF resources
	 * @throws IOException if the Tomcat base directory cannot be created
	 */
	public YadaEmbeddedTomcatTestServer(Path webappDir) throws IOException {
		this("", webappDir, List.of());
	}

	/**
	 * Creates a server mounted at the provided context path.
	 * @param contextPath servlet context path, empty for root
	 * @param webappDir webapp directory containing WEB-INF resources
	 * @throws IOException if the Tomcat base directory cannot be created
	 */
	public YadaEmbeddedTomcatTestServer(String contextPath, Path webappDir) throws IOException {
		this(contextPath, webappDir, List.of());
	}

	/**
	 * Creates a server mounted at the root context path with explicit extra classpath roots.
	 * @param webappDir webapp directory containing WEB-INF resources
	 * @param extraClasspathRoots extra classpath roots to mount into WEB-INF/classes
	 * @throws IOException if the Tomcat base directory cannot be created
	 */
	public YadaEmbeddedTomcatTestServer(Path webappDir, Path... extraClasspathRoots) throws IOException {
		this("", webappDir, List.of(extraClasspathRoots));
	}

	/**
	 * Creates a server mounted at the provided context path with explicit extra classpath roots.
	 * @param contextPath servlet context path, empty for root
	 * @param webappDir webapp directory containing WEB-INF resources
	 * @param extraClasspathRoots extra classpath roots to mount into WEB-INF/classes
	 * @throws IOException if the Tomcat base directory cannot be created
	 */
	public YadaEmbeddedTomcatTestServer(String contextPath, Path webappDir, Path... extraClasspathRoots) throws IOException {
		this(contextPath, webappDir, List.of(extraClasspathRoots));
	}

	/**
	 * Creates a server mounted at the root context path with explicit extra classpath roots.
	 * @param webappDir webapp directory containing WEB-INF resources
	 * @param extraClasspathRoots extra classpath roots to mount into WEB-INF/classes
	 * @throws IOException if the Tomcat base directory cannot be created
	 */
	public YadaEmbeddedTomcatTestServer(Path webappDir, Collection<Path> extraClasspathRoots) throws IOException {
		this("", webappDir, extraClasspathRoots);
	}

	/**
	 * Creates a server mounted at the provided context path with explicit extra classpath roots.
	 * @param contextPath servlet context path, empty for root
	 * @param webappDir webapp directory containing WEB-INF resources
	 * @param extraClasspathRoots extra classpath roots to mount into WEB-INF/classes
	 * @throws IOException if the Tomcat base directory cannot be created
	 */
	public YadaEmbeddedTomcatTestServer(String contextPath, Path webappDir, Collection<Path> extraClasspathRoots) throws IOException {
		this.contextPath = contextPath == null ? "" : contextPath;
		this.webappDir = webappDir.toAbsolutePath();
		this.extraClasspathRoots = normalizePaths(extraClasspathRoots);
	}

	/**
	 * Uses the given folder as Tomcat base directory instead of a new temporary folder.
	 * The folder is created when missing and is not deleted on close.
	 * @param baseDirectory the Tomcat base directory
	 */
	public void setBaseDirectory(Path baseDirectory) {
		this.baseDir = baseDirectory.toAbsolutePath().normalize();
		this.deleteBaseDir = false;
	}

	/**
	 * Runs exactly the listed web application initializers, in the order chosen by Spring, instead of
	 * the ones found in the mounted classpath roots.
	 * @param initializerClassNames fully qualified names of WebApplicationInitializer classes
	 */
	public void setInitializerClassNames(Collection<String> initializerClassNames) {
		this.initializerClassNames = List.copyOf(initializerClassNames);
	}

	/**
	 * When true, the web application loads every class from the test classpath first (parent-first delegation),
	 * so that objects obtained from the application context can be used directly by the test code.
	 * @param delegate true for parent-first class loading
	 */
	public void setDelegate(boolean delegate) {
		this.delegate = delegate;
	}

	/**
	 * Adds a servlet context init parameter, e.g. "globalInitializerClasses".
	 * @param name the parameter name
	 * @param value the parameter value
	 */
	public void addContextParameter(String name, String value) {
		contextParameters.put(name, value);
	}

	/**
	 * Starts the embedded server.
	 * @throws Exception if startup fails
	 */
	public void start() throws Exception {
		if (started) {
			return;
		}
		if (closed) {
			throw new IllegalStateException("The server has been closed");
		}
		if (!Files.isDirectory(webappDir)) {
			throw new IllegalArgumentException("webappDir must exist and be a directory: " + webappDir);
		}
		try {
			doStart();
		} catch (Throwable e) {
			// Release the connector and the context of a partial start
			try {
				close();
			} catch (Exception cleanupException) {
				e.addSuppressed(cleanupException);
			}
			throw e;
		}
	}

	private void doStart() throws Exception {
		if (baseDir == null) {
			// Keep runtime files inside the build folder when the build tells where it is
			String buildDir = System.getProperty("yts.buildDir");
			if (buildDir != null) {
				baseDir = Files.createTempDirectory(Files.createDirectories(Path.of(buildDir, "tomcat")), "yada-tomcat-itest");
			} else {
				baseDir = Files.createTempDirectory("yada-tomcat-itest");
			}
		} else {
			Files.createDirectories(baseDir);
		}
		for (String property : CATALINA_PROPERTIES) {
			previousCatalinaProperties.put(property, System.getProperty(property));
		}
		tomcat.setBaseDir(baseDir.toString());
		tomcatConfigured = true;
		tomcat.setPort(0);
		Connector connector = tomcat.getConnector();
		connector.setThrowOnFailure(true);
		tomcat.setAddDefaultWebXmlToWebapp(false);
		context = (StandardContext) tomcat.addWebapp(contextPath, webappDir.toString());
		context.setDelegate(delegate);
		contextParameters.forEach(context::addParameter);
		// Spring stores a root context failure in the servlet context, which is cleared when Tomcat stops the failed application
		context.addLifecycleListener(event -> {
			if (Lifecycle.BEFORE_STOP_EVENT.equals(event.getType()) && context.getServletContext() != null
				&& context.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) instanceof Throwable failure) {
				rootContextFailure = failure;
			}
		});
		StandardJarScanner jarScanner = new StandardJarScanner();
		jarScanner.setScanClassPath(false);
		jarScanner.setScanManifest(false);
		jarScanner.setScanAllDirectories(false);
		context.setJarScanner(jarScanner);
		List<Path> classpathDirectories = getJvmClasspathDirectories();
		List<Path> mountedClasspathRoots = resolveClasspathDirectories(webappDir, classpathDirectories, extraClasspathRoots);
		ClassLoader parentClassLoader;
		Set<Class<?>> initializerClasses;
		if (initializerClassNames != null) {
			parentClassLoader = Thread.currentThread().getContextClassLoader();
			initializerClasses = loadInitializerClasses(initializerClassNames, parentClassLoader);
		} else {
			parentClassLoader = buildParentClassLoader(Thread.currentThread().getContextClassLoader(), classpathDirectories, mountedClasspathRoots);
			initializerClasses = loadMountedInitializerClasses(mountedClasspathRoots, parentClassLoader);
		}
		context.setParentClassLoader(parentClassLoader);
		context.setResources(buildResources(context, mountedClasspathRoots));
		context.setContainerSciFilter(Pattern.quote(SpringServletContainerInitializer.class.getName()));
		context.addServletContainerInitializer(new SpringServletContainerInitializer(), initializerClasses);
		tomcat.start();
		started = true;
		// Tomcat does not throw when the web application fails to start: it just marks the context as failed
		if (context.getState() != LifecycleState.STARTED || !context.getState().isAvailable()) {
			throw new IllegalStateException("The web application at '" + contextPath + "' failed to start (state " + context.getState() + ")" + (rootContextFailure == null ? ": see the log for the cause" : ""), rootContextFailure);
		}
	}

	/**
	 * Loads an explicit list of web application initializers.
	 * @param classNames the initializer class names
	 * @param classLoader the classloader to load them with
	 * @return the initializer classes
	 * @throws IllegalArgumentException when a class is missing or is not a concrete WebApplicationInitializer
	 */
	private Set<Class<?>> loadInitializerClasses(Collection<String> classNames, ClassLoader classLoader) {
		Set<Class<?>> initializerClasses = new LinkedHashSet<>();
		for (String className : classNames) {
			Class<?> initializerClass;
			try {
				initializerClass = Class.forName(className, false, classLoader);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Initializer class not found: " + className, e);
			}
			if (Modifier.isAbstract(initializerClass.getModifiers()) || !WebApplicationInitializer.class.isAssignableFrom(initializerClass)) {
				throw new IllegalArgumentException("Not a concrete WebApplicationInitializer: " + className);
			}
			initializerClasses.add(initializerClass);
		}
		return initializerClasses;
	}

	/**
	 * Returns the root Spring application context of the running web application.
	 * @return the root context, or null when the application has none
	 * @throws IllegalStateException when the server is not running
	 */
	public WebApplicationContext getRootApplicationContext() {
		if (!started || context == null) {
			throw new IllegalStateException("The server is not running");
		}
		return WebApplicationContextUtils.getWebApplicationContext(context.getServletContext());
	}

	/**
	 * @return true when the server started and its web application is available
	 */
	public boolean isAvailable() {
		return started && context != null && context.getState().isAvailable();
	}

	/**
	 * Resolves an application-relative path against the server base URI.
	 * @param path application-relative path
	 * @return resolved URI
	 */
	public URI resolve(String path) {
		String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
		return getBaseUri().resolve(normalizedPath);
	}

	/**
	 * Returns the server base URI with a trailing slash.
	 * @return base URI
	 */
	public URI getBaseUri() {
		return URI.create("http://127.0.0.1:" + getPort() + contextPath + "/");
	}

	/**
	 * Returns the dynamically assigned HTTP port.
	 * @return HTTP port
	 */
	public int getPort() {
		return tomcat.getConnector().getLocalPort();
	}

	/**
	 * Builds the Tomcat WEB-INF/classes resources for the resolved classpath roots.
	 * @param context the Tomcat context
	 * @param classpathDirectories the classpath directories to mount
	 * @return the configured Tomcat resource root
	 */
	private WebResourceRoot buildResources(StandardContext context, Collection<Path> classpathDirectories) {
		WebResourceRoot resources = new StandardRoot(context);
		for (Path classpathDirectory : classpathDirectories) {
			resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", classpathDirectory.toString(), "/"));
		}
		return resources;
	}

	/**
	 * Builds the parent classloader used by the Tomcat context.
	 * @param parentClassLoader the current parent classloader
	 * @param classpathDirectories the available classpath directories
	 * @param mountedClasspathRoots the directories mounted into WEB-INF/classes
	 * @return the parent classloader to use for the Tomcat context
	 */
	private ClassLoader buildParentClassLoader(ClassLoader parentClassLoader, Collection<Path> classpathDirectories, Collection<Path> mountedClasspathRoots) {
		Set<String> blockedClassNames = findBlockedInitializerClasses(classpathDirectories, mountedClasspathRoots, parentClassLoader);
		if (blockedClassNames.isEmpty()) {
			return parentClassLoader;
		}
		return new InitializerFilteringClassLoader(parentClassLoader, blockedClassNames);
	}

	/**
	 * Loads the web application initializers that belong to the mounted WEB-INF/classes roots.
	 * @param mountedClasspathRoots the classpath roots mounted into WEB-INF/classes
	 * @param parentClassLoader the classloader used to load candidate initializer classes
	 * @return the concrete initializer classes available to Spring's ServletContainerInitializer
	 */
	private Set<Class<?>> loadMountedInitializerClasses(Collection<Path> mountedClasspathRoots, ClassLoader parentClassLoader) {
		Set<Class<?>> initializerClasses = new LinkedHashSet<>();
		for (Path mountedClasspathRoot : mountedClasspathRoots) {
			for (String className : findInitializerClasses(mountedClasspathRoot, parentClassLoader)) {
				try {
					Class<?> initializerClass = Class.forName(className, false, parentClassLoader);
					initializerClasses.add(initializerClass);
				} catch (ClassNotFoundException | LinkageError ignored) {
					// Ignore classes that cannot be loaded while preparing the embedded test webapp.
				}
			}
		}
		return initializerClasses;
	}

	/**
	 * Resolves the classpath directories for the target webapp.
	 * @param webappDir the target webapp directory
	 * @param classpathDirectories the available classpath directories
	 * @param extraClasspathRoots explicit extra roots to append after inferred defaults
	 * @return the directories that should be mounted into WEB-INF/classes
	 */
	static List<Path> resolveClasspathDirectories(Path webappDir, Collection<Path> classpathDirectories, Collection<Path> extraClasspathRoots) {
		List<Path> normalizedExtraRoots = normalizePaths(extraClasspathRoots);
		Path normalizedWebappDir = webappDir.toAbsolutePath().normalize();
		Path buildDir = findBuildDirectory(normalizedWebappDir);
		if (buildDir == null) {
			if (normalizedExtraRoots.isEmpty()) {
				throw new IllegalArgumentException("Unable to infer classpath roots for webappDir " + normalizedWebappDir + ". Add explicit classpath roots when using a non-Gradle layout.");
			}
			return normalizedExtraRoots;
		}
		Path moduleRoot = buildDir.getParent();
		String sourceSetName = detectSourceSetName(buildDir, normalizedWebappDir);
		Set<Path> resolvedRoots = new LinkedHashSet<>();
		for (Path classpathDirectory : classpathDirectories) {
			Path normalizedDirectory = classpathDirectory.toAbsolutePath().normalize();
			if (!normalizedDirectory.startsWith(moduleRoot)) {
				continue;
			}
			if (normalizedWebappDir.startsWith(normalizedDirectory)) {
				continue;
			}
			if (sourceSetName != null && !matchesSourceSet(buildDir, normalizedDirectory, sourceSetName)) {
				continue;
			}
			resolvedRoots.add(normalizedDirectory);
		}
		resolvedRoots.addAll(normalizedExtraRoots);
		return List.copyOf(resolvedRoots);
	}

	/**
	 * Reads the current JVM classpath and returns the directory entries in their original order.
	 * @return the directory entries from the current JVM classpath
	 */
	private List<Path> getJvmClasspathDirectories() {
		Set<Path> directories = new LinkedHashSet<>();
		String[] classpathEntries = System.getProperty("java.class.path", "").split(System.getProperty("path.separator"));
		for (String classpathEntry : classpathEntries) {
			if (classpathEntry == null || classpathEntry.isBlank()) {
				continue;
			}
			Path path = Path.of(classpathEntry);
			if (Files.isDirectory(path)) {
				directories.add(path.toAbsolutePath());
			}
		}
		return new ArrayList<>(directories);
	}

	/**
	 * Normalizes a collection of paths while preserving the original order.
	 * @param paths the paths to normalize
	 * @return the normalized paths
	 */
	private static List<Path> normalizePaths(Collection<Path> paths) {
		Set<Path> normalizedPaths = new LinkedHashSet<>();
		if (paths != null) {
			for (Path path : paths) {
				if (path != null) {
					normalizedPaths.add(path.toAbsolutePath().normalize());
				}
			}
		}
		return List.copyOf(normalizedPaths);
	}

	/**
	 * Finds the nearest build directory ancestor of the given path.
	 * @param path the path to inspect
	 * @return the nearest build directory ancestor, or null when missing
	 */
	private static Path findBuildDirectory(Path path) {
		Path currentPath = path;
		while (currentPath != null) {
			Path fileName = currentPath.getFileName();
			if (fileName != null && "build".equals(fileName.toString())) {
				return currentPath;
			}
			currentPath = currentPath.getParent();
		}
		return null;
	}

	/**
	 * Detects the Gradle source set name from the webapp output path.
	 * @param buildDir the build directory that owns the webapp
	 * @param webappDir the webapp directory
	 * @return the source set name, or null when it cannot be inferred
	 */
	private static String detectSourceSetName(Path buildDir, Path webappDir) {
		Path relativePath = buildDir.relativize(webappDir);
		if (relativePath.getNameCount() >= 2 && "resources".equals(relativePath.getName(0).toString())) {
			return relativePath.getName(1).toString();
		}
		return null;
	}

	/**
	 * Checks whether a classpath directory belongs to the detected source set.
	 * @param buildDir the build directory that owns the path
	 * @param classpathDirectory the classpath directory to test
	 * @param sourceSetName the expected source set name
	 * @return true when the path belongs to the expected source set
	 */
	private static boolean matchesSourceSet(Path buildDir, Path classpathDirectory, String sourceSetName) {
		Path relativePath = buildDir.relativize(classpathDirectory);
		if (relativePath.getNameCount() < 2) {
			return false;
		}
		String rootFolder = relativePath.getName(0).toString();
		if ("resources".equals(rootFolder)) {
			return sourceSetName.equals(relativePath.getName(1).toString());
		}
		if ("classes".equals(rootFolder)) {
			return sourceSetName.equals(relativePath.getFileName().toString());
		}
		return false;
	}

	/**
	 * Finds the initializer classes that should be hidden from the Tomcat parent classloader.
	 * @param classpathDirectories the available classpath directories
	 * @param mountedClasspathRoots the directories mounted into WEB-INF/classes
	 * @param parentClassLoader the parent classloader used to load candidate classes
	 * @return the blocked initializer class names
	 */
	private Set<String> findBlockedInitializerClasses(Collection<Path> classpathDirectories, Collection<Path> mountedClasspathRoots, ClassLoader parentClassLoader) {
		Set<Path> mountedRoots = new LinkedHashSet<>(normalizePaths(mountedClasspathRoots));
		Set<String> blockedClassNames = new LinkedHashSet<>();
		for (Path classpathDirectory : classpathDirectories) {
			Path normalizedDirectory = classpathDirectory.toAbsolutePath().normalize();
			if (mountedRoots.contains(normalizedDirectory)) {
				continue;
			}
			blockedClassNames.addAll(findInitializerClasses(normalizedDirectory, parentClassLoader));
		}
		return blockedClassNames;
	}

	/**
	 * Finds the concrete WebApplicationInitializer classes inside one classpath directory.
	 * @param classpathDirectory the classpath directory to scan
	 * @param parentClassLoader the parent classloader used to load candidate classes
	 * @return the initializer classes found in the directory
	 */
	private Set<String> findInitializerClasses(Path classpathDirectory, ClassLoader parentClassLoader) {
		Set<String> initializerClasses = new LinkedHashSet<>();
		try (var paths = Files.walk(classpathDirectory)) {
			paths.filter(Files::isRegularFile)
				.filter(path -> path.toString().endsWith(".class"))
				.filter(path -> !path.getFileName().toString().contains("$"))
				.forEach(path -> {
					String className = toClassName(classpathDirectory, path);
					try {
						Class<?> candidateClass = Class.forName(className, false, parentClassLoader);
						if (!Modifier.isAbstract(candidateClass.getModifiers()) && WebApplicationInitializer.class.isAssignableFrom(candidateClass)) {
							initializerClasses.add(className);
						}
					} catch (ClassNotFoundException | LinkageError ignored) {
						// Ignore classes that cannot be loaded while building the filtered parent classloader.
					}
				});
		} catch (IOException ignored) {
			// Ignore directories that cannot be scanned while building the filtered parent classloader.
		}
		return initializerClasses;
	}

	/**
	 * Converts a class file path into a Java class name relative to one classpath root.
	 * @param classpathDirectory the classpath root directory
	 * @param classFile the class file path
	 * @return the fully qualified Java class name
	 */
	private String toClassName(Path classpathDirectory, Path classFile) {
		String relativeClassFile = classpathDirectory.relativize(classFile).toString();
		String className = relativeClassFile.substring(0, relativeClassFile.length() - ".class".length());
		return className.replace('\\', '.').replace('/', '.');
	}

	/**
	 * Filters out blocked initializer classes from parent class loading and resource lookups.
	 */
	private static class InitializerFilteringClassLoader extends ClassLoader {
		private final Set<String> blockedClassNames;
		private final Set<String> blockedResourceNames;

		/**
		 * Creates the filtering classloader.
		 * @param parent the parent classloader to delegate to
		 * @param blockedClassNames the fully qualified class names to hide
		 */
		InitializerFilteringClassLoader(ClassLoader parent, Collection<String> blockedClassNames) {
			super(parent);
			Set<String> blockedResources = new LinkedHashSet<>();
			for (String blockedClassName : blockedClassNames) {
				blockedResources.add(blockedClassName.replace('.', '/') + ".class");
			}
			this.blockedClassNames = Set.copyOf(blockedClassNames);
			this.blockedResourceNames = Set.copyOf(blockedResources);
		}

		@Override
		public URL getResource(String name) {
			if (blockedResourceNames.contains(name)) {
				return null;
			}
			return super.getResource(name);
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			if (blockedResourceNames.contains(name)) {
				return Collections.emptyEnumeration();
			}
			return super.getResources(name);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (blockedClassNames.contains(name)) {
				throw new ClassNotFoundException(name);
			}
			return super.loadClass(name, resolve);
		}
	}

	/**
	 * Stops the server and releases its port. Safe to call more than once.
	 * @throws Exception if stopping fails
	 */
	@Override
	public void close() throws Exception {
		if (closed) {
			return;
		}
		closed = true;
		if (!tomcatConfigured) {
			// Tomcat was never set up: touching it would create a default base directory in the working directory
			if (deleteBaseDir) {
				deleteBaseDir();
			}
			return;
		}
		try {
			if (tomcat.getServer().getState().isAvailable()) {
				tomcat.stop();
			}
		} finally {
			try {
				tomcat.destroy();
			} finally {
				started = false;
				restoreCatalinaProperties();
				if (deleteBaseDir) {
					deleteBaseDir();
				}
			}
		}
	}

	private void restoreCatalinaProperties() {
		previousCatalinaProperties.forEach((property, value) -> {
			if (value == null) {
				System.clearProperty(property);
			} else {
				System.setProperty(property, value);
			}
		});
		previousCatalinaProperties.clear();
	}

	private void deleteBaseDir() throws IOException {
		if (baseDir == null || !Files.exists(baseDir)) {
			return;
		}
		try (var paths = Files.walk(baseDir)) {
			paths.sorted((first, second) -> second.getNameCount() - first.getNameCount())
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException ignored) {
						// Ignore cleanup failures in test utilities.
					}
				});
		}
	}
}
