/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RegisterDependenciesInRootTest extends GradleIntegrationTest {
	@Test
	public void registerDependencies() throws IOException {
		setFile("settings.gradle")
				.toLines("include 'sub'");
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins { id 'com.diffplug.gradle.spotless' }");
		setFile("sub/build.gradle").toLines(
				"apply plugin: 'com.diffplug.gradle.spotless'",
				"",
				"spotless {",
				"  java {",
				"    target 'src/main/java/**/*.java'",
				"    googleJavaFormat('1.2')",
				"  }",
				"}");

		// works fine on old versions
		String oldestSupported = gradleRunner()
				.withArguments("spotlessCheck").build().getOutput();
		Assertions.assertThat(oldestSupported.replace("\r", "")).startsWith(
				":spotlessCheck UP-TO-DATE\n" +
						":sub:spotlessJava\n" +
						":sub:spotlessJavaCheck\n" +
						":sub:spotlessCheck\n" +
						"\n" +
						"BUILD SUCCESSFUL");

		// generates a warning in 6.0
		setFile("gradle.properties").toLines();
		String warningStarts = gradleRunner().withGradleVersion("6.0")
				.withArguments("spotlessCheck").build().getOutput();
		assertWarning(warningStarts, true);

		// we can make the old version generate the warning with spotless_register_dependencies_in_root
		setFile("gradle.properties").toLines(
				"spotless_register_dependencies_in_root=true");
		String oldestSupportedWithRegisterDependencies = gradleRunner()
				.withArguments("spotlessCheck").build().getOutput();
		assertWarning(oldestSupportedWithRegisterDependencies, true);

		// fix the root project
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins { id 'com.diffplug.gradle.spotless' }",
				"spotless {",
				"  java {",
				"    targetEmptyForDeclaration()",
				"    googleJavaFormat('1.2')",
				"  }",
				"}");
		assertWarning(warningStarts, true);

		setFile("gradle.properties").toLines(
				"spotless_register_dependencies_in_root=true");
		String oldestSupportedWithRegisterDependenciesFixed = gradleRunner()
				.withArguments("spotlessCheck").build().getOutput();
		assertWarning(oldestSupportedWithRegisterDependenciesFixed, false);

		setFile("gradle.properties").toLines();
		String warningStartsFixed = gradleRunner().withGradleVersion("6.0")
				.withArguments("spotlessCheck").build().getOutput();
		assertWarning(warningStartsFixed, false);
	}

	private void assertWarning(String input, boolean isThere) {
		String warning = "This subproject is using a formatter that was not used in the root project.  To enable\n" +
				"performance optimzations (and avoid Gradle 7 deprecation warnings), you must declare\n" +
				"all of your formatters within the root project.  For example, if your subproject has\n" +
				"a `java {}` block but your root project does not, just add a matching `java {}` block to\n" +
				"your root project.  If you want to make it clear that it is intentional that the target\n" +
				"is empty, you can do this in your root build.gradle:\n" +
				"\n" +
				"  spotless {\n" +
				"    java {\n" +
				"      targetEmptyForDeclaration()\n" +
				"      [...same steps as subproject...]\n" +
				"    }\n" +
				"  }\n" +
				"\n" +
				"To help you figure out which block is missing, the step you are missing is\n" +
				"  step name: google-java-format\n" +
				"  requested: com.google.googlejavaformat:google-java-format:1.2 with transitives\n";
		if (isThere) {
			Assertions.assertThat(input.replace("\r", "")).contains(warning);
		} else {
			Assertions.assertThat(input.replace("\r", "")).doesNotContain(warning);
		}
	}
}
