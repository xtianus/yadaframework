package net.yadaframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Verifies how the embedded Tomcat harness chooses classpath roots for test webapps.
 */
class YadaEmbeddedTomcatTestServerTest {

	/**
	 * Keeps only the roots that belong to the target module and matching source set.
	 */
	@Test
	void resolveClasspathDirectoriesKeepsOnlyMatchingSourceSetRootsFromSameModule() {
		Path webappDir = Path.of("C:/work/tests/YadaWebSecurity/build/resources/test/itest-webapp");
		Path sameModuleTestClasses = Path.of("C:/work/tests/YadaWebSecurity/build/classes/java/test");
		Path sameModuleTestResources = Path.of("C:/work/tests/YadaWebSecurity/build/resources/test");
		Path sameModuleMainClasses = Path.of("C:/work/tests/YadaWebSecurity/build/classes/java/main");
		Path otherModuleTestFixtures = Path.of("C:/work/tests/YadaWeb/build/classes/java/testFixtures");
		Path otherModuleResources = Path.of("C:/work/tests/YadaExamples/build/resources/main");

		List<Path> resolvedRoots = YadaEmbeddedTomcatTestServer.resolveClasspathDirectories(webappDir,
			List.of(sameModuleTestClasses, sameModuleTestResources, sameModuleMainClasses, otherModuleTestFixtures, otherModuleResources),
			List.of());

		assertIterableEquals(List.of(sameModuleTestClasses.toAbsolutePath().normalize(), sameModuleTestResources.toAbsolutePath().normalize()), resolvedRoots);
	}

	/**
	 * Appends explicit extra roots after the inferred defaults without duplicating directories.
	 */
	@Test
	void resolveClasspathDirectoriesAppendsExplicitExtraRootsAfterDefaults() {
		Path webappDir = Path.of("C:/work/tests/YadaWebSecurity/build/resources/test/itest-webapp");
		Path sameModuleTestClasses = Path.of("C:/work/tests/YadaWebSecurity/build/classes/java/test");
		Path explicitExtraRoot = Path.of("C:/work/tests/YadaWeb/build/classes/java/testFixtures");

		List<Path> resolvedRoots = YadaEmbeddedTomcatTestServer.resolveClasspathDirectories(webappDir,
			List.of(sameModuleTestClasses, explicitExtraRoot),
			List.of(explicitExtraRoot));

		assertIterableEquals(List.of(sameModuleTestClasses.toAbsolutePath().normalize(), explicitExtraRoot.toAbsolutePath().normalize()), resolvedRoots);
	}

	/**
	 * Rejects layouts where no Gradle build root can be inferred and no explicit roots are supplied.
	 */
	@Test
	void resolveClasspathDirectoriesRequiresBuildAncestorWhenNoExtraRootsAreProvided() {
		Path webappDir = Path.of("C:/work/tests/custom-layout/itest-webapp");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> YadaEmbeddedTomcatTestServer.resolveClasspathDirectories(webappDir,
			List.of(),
			List.of()));

		assertEquals("Unable to infer classpath roots for webappDir C:\\work\\tests\\custom-layout\\itest-webapp. Add explicit classpath roots when using a non-Gradle layout.", exception.getMessage());
	}
}
