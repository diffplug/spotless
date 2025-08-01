/*
 * Copyright 2016-2021 DiffPlug
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;

class GradleIncrementalResolutionTest extends GradleIntegrationHarness {
	@Test
	void failureDoesntTriggerAll() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        target '*.md'",
				"        custom 'lowercase', { str ->",
				"            String result = str.toLowerCase(Locale.ROOT)",
				"            println(\"<${result.trim()}>\")",
				"            return result",
				"        }",
				"        bumpThisNumberIfACustomStepChanges(1)",
				"    }",
				"}");
		// test our harness (build makes things lower case)
		writeState("ABC");
		assertState("ABC");
		writeState("aBc");
		assertState("aBc");
		// check will run against all three the first time.
		checkRanAgainst("abc");
		// Subsequent runs will use the cached error message
		checkRanAgainstNoneButError().contains("> The following files had format violations:\n" +
				"      b.md\n" +
				"          @@ -1 +1 @@\n" +
				"          -B\n" +
				"          +b");
		checkRanAgainstNoneButError().contains("> The following files had format violations:\n" +
				"      b.md\n" +
				"          @@ -1 +1 @@\n" +
				"          -B\n" +
				"          +b");
		// apply will simply copy outputs the first time: no formatters executed
		applyRanAgainst("");
		// the second time, it will only run on the file that was changed by apply
		applyRanAgainst("b");
		// and nobody the last time
		applyRanAgainst("");

		// if we change just one file
		writeState("Abc");
		// then check runs against just the changed file
		checkRanAgainst("a");
		// even after failing once the error is still there
		checkRanAgainstNoneButError().contains("> The following files had format violations:\n" +
				"      a.md\n" +
				"          @@ -1 +1 @@\n" +
				"          -A\n" +
				"          +a");
		// and so does apply
		applyRanAgainst();
		applyRanAgainst("a");
		// until the issue has been fixed
		applyRanAgainst("");
	}

	private String filename(String name) {
		return name.toLowerCase(Locale.ROOT) + ".md";
	}

	private void writeState(String state) throws IOException {
		for (char c : state.toCharArray()) {
			String letter = new String(new char[]{c});
			boolean exists = new File(rootFolder(), filename(letter)).exists();
			boolean needsChanging = exists && !read(filename(letter)).trim().equals(letter);
			if (!exists || needsChanging) {
				setFile(filename(letter)).toContent(letter);
			}
		}
	}

	private void assertState(String state) throws IOException {
		for (char c : state.toCharArray()) {
			String letter = new String(new char[]{c});
			if (Character.isLowerCase(c)) {
				assertEquals(letter.toLowerCase(Locale.ROOT), read(filename(letter)).trim());
			} else {
				assertEquals(letter.toUpperCase(Locale.ROOT), read(filename(letter)).trim());
			}
		}
	}

	private void applyRanAgainst(String... ranAgainst) throws IOException {
		taskRanAgainst("spotlessApply", ranAgainst);
	}

	private void checkRanAgainst(String... ranAgainst) throws IOException {
		taskRanAgainst("spotlessCheck", ranAgainst);
	}

	private AbstractStringAssert<?> checkRanAgainstNoneButError() throws IOException {
		String console = taskRanAgainst("spotlessCheck");
		return Assertions.assertThat(console);
	}

	private String taskRanAgainst(String task, String... ranAgainst) throws IOException {
		pauseForFilesystem();
		String console = StringPrinter.buildString(Errors.rethrow().wrap(printer -> {
			boolean expectFailure = "spotlessCheck".equals(task) && !isClean();
			if (expectFailure) {
				gradleRunner().withArguments(task).forwardStdOutput(printer.toWriter()).forwardStdError(printer.toWriter()).buildAndFail();
			} else {
				gradleRunner().withArguments(task).forwardStdOutput(printer.toWriter()).build();
			}
		}));
		SortedSet<String> added = new TreeSet<>();
		for (String line : console.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
				added.add(trimmed.substring(1, trimmed.length() - 1));
			}
		}
		assertEquals(concat(Arrays.asList(ranAgainst)), concat(added));
		return console;
	}

	private String concat(Iterable<String> iterable) {
		StringBuilder result = new StringBuilder();
		for (String item : iterable) {
			result.append(item);
		}
		return result.toString();
	}

	private boolean isClean() throws IOException {
		for (File file : rootFolder().listFiles()) {
			if (file.isFile() && file.getName().length() == 4 && file.getName().endsWith(".md")) {
				String content = read(file.getName());
				if (!content.toLowerCase(Locale.ROOT).equals(content)) {
					return false;
				}
			}
		}
		return true;
	}
}
