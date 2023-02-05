/*
 * Copyright 2023 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.maven.java;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class JavaDefaultIncludesTest extends MavenIntegrationHarness {

	private static final String UNFORMATTED = "java/removeunusedimports/JavaCodeWithPackageUnformatted.test";
	private static final String FORMATTED = "java/removeunusedimports/JavaCodeWithPackageFormatted.test";

	private static final String FILE_1 = "src/main/java/com/diffplug/spotless/One.java";
	private static final String FILE_2 = "src/test/java/com/diffplug/spotless/Two.java";
	private static final String FILE_3 = "src/com/diffplug/spotless/Three.java";
	private static final String FILE_4 = "test/com/diffplug/spotless/Four.java";
	private static final String FILE_5 = "foo/bar/Five.java";

	@BeforeEach
	void beforeEach() {
		for (String file : Arrays.asList(FILE_1, FILE_2, FILE_3, FILE_4, FILE_5)) {
			setFile(file).toResource(UNFORMATTED);
		}
	}

	@Test
	void noCustomConfiguration() throws Exception {
		writePomWithBuildConfiguration();

		mavenRunner().withArguments("spotless:apply").runNoError();

		// Files 1, 2 are formatted because they live under default Maven source & test source dirs
		assertFile(FILE_1).sameAsResource(FORMATTED);
		assertFile(FILE_2).sameAsResource(FORMATTED);

		// Files 3, 4, 5 are not formatted because they live outside default Maven source & test source dirs
		assertFile(FILE_3).sameAsResource(UNFORMATTED);
		assertFile(FILE_4).sameAsResource(UNFORMATTED);
		assertFile(FILE_5).sameAsResource(UNFORMATTED);
	}

	@Test
	void customSourceDirConfiguration() throws Exception {
		writePomWithBuildConfiguration("<sourceDirectory>src</sourceDirectory>");

		mavenRunner().withArguments("spotless:apply").runNoError();

		// Files 1, 2, 3 are formatted because they live under the custom-configured source dir
		assertFile(FILE_1).sameAsResource(FORMATTED);
		assertFile(FILE_2).sameAsResource(FORMATTED);
		assertFile(FILE_3).sameAsResource(FORMATTED);

		// File 4, 5 are not formatted because they live outside the custom-configured source dir and default test source dir
		assertFile(FILE_4).sameAsResource(UNFORMATTED);
		assertFile(FILE_5).sameAsResource(UNFORMATTED);
	}

	@Test
	void customTestSourceDirConfiguration() throws Exception {
		writePomWithBuildConfiguration("<testSourceDirectory>test</testSourceDirectory>");

		mavenRunner().withArguments("spotless:apply").runNoError();

		// File 1 is formatted because it lives under the default source dir
		assertFile(FILE_1).sameAsResource(FORMATTED);
		// File 4 is formatted because it lives under the custom-configured test source dir
		assertFile(FILE_4).sameAsResource(FORMATTED);

		// Files 2, 3, 5 are not formatted because they live outside the default source dir and custom-configured test source dir
		assertFile(FILE_2).sameAsResource(UNFORMATTED);
		assertFile(FILE_3).sameAsResource(UNFORMATTED);
		assertFile(FILE_5).sameAsResource(UNFORMATTED);
	}

	@Test
	void customSourceDirAndTestSourceDirConfiguration() throws Exception {
		writePomWithBuildConfiguration(
				"<sourceDirectory>src</sourceDirectory>",
				"<testSourceDirectory>test</testSourceDirectory>");

		mavenRunner().withArguments("spotless:apply").runNoError();

		// Files 1, 2, 3, 4 are formatted because they live under custom-configured source and test source dirs
		assertFile(FILE_1).sameAsResource(FORMATTED);
		assertFile(FILE_2).sameAsResource(FORMATTED);
		assertFile(FILE_3).sameAsResource(FORMATTED);
		assertFile(FILE_4).sameAsResource(FORMATTED);

		// File 5 is not formatted because it lives outside custom-configured source and test source dirs
		assertFile(FILE_5).sameAsResource(UNFORMATTED);
	}

	@Test
	void sameCustomSourceDirAndTestSourceDirConfiguration() throws Exception {
		writePomWithBuildConfiguration(
				"<sourceDirectory>foo/bar</sourceDirectory>",
				"<testSourceDirectory>foo/bar</testSourceDirectory>");

		mavenRunner().withArguments("spotless:apply").runNoError();

		// Files 1, 2, 3, 4 are not formatted because they live outside custom-configured source and test source dirs
		assertFile(FILE_1).sameAsResource(UNFORMATTED);
		assertFile(FILE_2).sameAsResource(UNFORMATTED);
		assertFile(FILE_3).sameAsResource(UNFORMATTED);
		assertFile(FILE_4).sameAsResource(UNFORMATTED);

		// File 5 is formatted because it lives under the custom-configured source/test source dir
		assertFile(FILE_5).sameAsResource(FORMATTED);
	}

	@Test
	void nestedCustomSourceDirAndTestSourceDirConfiguration() throws Exception {
		writePomWithBuildConfiguration(
				"<sourceDirectory>foo</sourceDirectory>",
				"<testSourceDirectory>foo/bar</testSourceDirectory>");

		mavenRunner().withArguments("spotless:apply").runNoError();

		// Files 1, 2, 3, 4 are not formatted because they live outside custom-configured source and test source dirs
		assertFile(FILE_1).sameAsResource(UNFORMATTED);
		assertFile(FILE_2).sameAsResource(UNFORMATTED);
		assertFile(FILE_3).sameAsResource(UNFORMATTED);
		assertFile(FILE_4).sameAsResource(UNFORMATTED);

		// File 5 is formatted because it lives under the custom-configured source/test source dir
		assertFile(FILE_5).sameAsResource(FORMATTED);
	}

	private void writePomWithBuildConfiguration(String... build) throws IOException {
		String xml = createPomXmlContent(build, new String[]{"<java>", "<removeUnusedImports/>", "</java>"});
		setFile("pom.xml").toContent(xml);
	}
}
