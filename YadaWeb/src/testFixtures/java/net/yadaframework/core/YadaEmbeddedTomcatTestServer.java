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
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.web.WebApplicationInitializer;

/**
 * Starts an embedded Tomcat instance for servlet integration tests without blocking the JVM.
 */
public class YadaEmbeddedTomcatTestServer implements AutoCloseable {
	private final Tomcat tomcat = new Tomcat();
	private final Path baseDir;
	private final String contextPath;
	private final Path webappDir;
	private final List<Path> extraClasspathRoots;

	private boolean started = false;

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
		this.baseDir = Files.createTempDirectory("yada-tomcat-itest");
		this.extraClasspathRoots = normalizePaths(extraClasspathRoots);
	}

	/**
	 * Starts the embedded server.
	 * @throws Exception if startup fails
	 */
	public void start() throws Exception {
		if (started) {
			return;
		}
		if (!Files.isDirectory(webappDir)) {
			throw new IllegalArgumentException("webappDir must exist and be a directory: " + webappDir);
		}
		tomcat.setBaseDir(baseDir.toString());
		tomcat.setPort(0);
		Connector connector = tomcat.getConnector();
		connector.setThrowOnFailure(true);
		tomcat.setAddDefaultWebXmlToWebapp(false);
		StandardContext context = (StandardContext) tomcat.addWebapp(contextPath, webappDir.toString());
		List<Path> classpathDirectories = getJvmClasspathDirectories();
		List<Path> mountedClasspathRoots = resolveClasspathDirectories(webappDir, classpathDirectories, extraClasspathRoots);
		context.setParentClassLoader(buildParentClassLoader(Thread.currentThread().getContextClassLoader(), classpathDirectories, mountedClasspathRoots));
		context.setResources(buildResources(context, mountedClasspathRoots));
		tomcat.start();
		started = true;
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

	@Override
	public void close() throws Exception {
		try {
			if (started) {
				tomcat.stop();
			}
		} finally {
			tomcat.destroy();
			deleteBaseDir();
			started = false;
		}
	}

	private void deleteBaseDir() throws IOException {
		if (!Files.exists(baseDir)) {
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
