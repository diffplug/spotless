/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class SpecificFilesTest extends MavenIntegrationHarness {
	private String testFile(int number, boolean absolute) throws IOException {
		var rel = "src/main/java/test" + number + ".java";
		Path path;
		if (absolute) {
			path = Paths.get(rootFolder().getAbsolutePath(), rel);
		} else {
			path = Paths.get(rel);
		}
		var result = path.toString();
		if (!isOnWindows()) {
			return result;
		} else {
			return result.replace("\\", "\\\\");
		}
	}

	private boolean isOnWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	private String testFile(int number) throws IOException {
		return testFile(number, false);
	}

	private String fixture(boolean formatted) {
		return "java/googlejavaformat/JavaCode" + (formatted ? "F" : "Unf") + "ormatted.test";
	}

	private void integration(String patterns, boolean firstFormatted, boolean secondFormatted, boolean thirdFormatted)
			throws IOException, InterruptedException {

		writePomWithJavaSteps(
				"<includes>",
				"  <include>src/**/java/**/*.java</include>",
				"</includes>",
				"<googleJavaFormat>",
				"  <version>1.10.0</version>",
				"</googleJavaFormat>");

		setFile(testFile(1)).toResource(fixture(false));
		setFile(testFile(2)).toResource(fixture(false));
		setFile(testFile(3)).toResource(fixture(false));

		mavenRunner()
				.withArguments("spotless:apply", "-DspotlessFiles=" + patterns)
				.runNoError();

		assertFile(testFile(1)).sameAsResource(fixture(firstFormatted));
		assertFile(testFile(2)).sameAsResource(fixture(secondFormatted));
		assertFile(testFile(3)).sameAsResource(fixture(thirdFormatted));
	}

	@Test
	void singleFile() throws IOException, InterruptedException {
		integration(testFile(2, true), false, true, false);
	}

	@Test
	void multiFile() throws IOException, InterruptedException {
		integration(testFile(1, true) + "," + testFile(3, true), true, false, true);
	}

	@Test
	void regexp() throws IOException, InterruptedException {
		String pattern;
		if (isOnWindows())
			pattern = "\".*\\\\src\\\\main\\\\java\\\\test(1|3).java\"";
		else
			pattern = "'.*/src/main/java/test(1|3).java'";
		integration(pattern, true, false, true);
	}
}
