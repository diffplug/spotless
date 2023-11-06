package com.diffplug.spotless.maven.groovy;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

import org.junit.jupiter.api.Test;

class RemoveSemiColonsTest extends MavenIntegrationHarness {

	@Test
	void testRemoveSemiColonsString() throws Exception {
		writePomWithGroovySteps("<removeSemiColons/>");
		runTest("Hello World;", "Hello World");
	}

	@Test
	void testNotRemoveSemiColonsString() throws Exception {
		writePomWithGroovySteps("<removeSemiColons/>");
		runTest("Hello;World", "Hello;World");
	}

	@Test
	void testRemoveSemiColons() throws Exception {
		writePomWithGroovySteps("<removeSemiColons/>");

		String path = "src/main/groovy/test.groovy";
		setFile(path).toResource("groovy/removesemicolons/GroovyCodeWithSemiColons.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("groovy/removesemicolons/GroovyCodeWithSemiColonsFormatted.test");

	}

	private void runTest(String sourceContent, String targetContent) throws Exception {
		String path = "src/main/groovy/test.groovy";
		setFile(path).toContent(sourceContent);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).hasContent(targetContent);
	}
}
