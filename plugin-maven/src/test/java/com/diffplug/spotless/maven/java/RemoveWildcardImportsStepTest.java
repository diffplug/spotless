package com.diffplug.spotless.maven.java;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class RemoveWildcardImportsStepTest extends MavenIntegrationHarness {

	@Test
	void testRemoveWildcardImports() throws Exception {
		writePomWithJavaSteps("<removeWildcardImports/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/removewildcardimports/JavaCodeWildcardsFormatted.test");
	}
}
