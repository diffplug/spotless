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
package com.diffplug.spotless.extra.eclipse.java;

import static org.junit.Assert.*;

import java.util.Properties;
import java.util.function.Consumer;

import org.assertj.core.util.Arrays;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;

/** Eclipse JDT wrapper integration tests */
public class EclipseJdtCleanUpStepImplTest {
	private static String SOURCE_FILE_PATH = "some .. / \\ ill&formatted/$path";
	private static TestData TEST_DATA = null;

	@BeforeClass
	public static void initializeStatic() throws Exception {
		TEST_DATA = TestData.getTestDataOnFileSystem();
	}

	@Test
	public void emptyInput() throws Throwable {
		cleanUpTest("", "", config -> {});
	}

	@Test
	public void defaultConfiguration() throws Throwable {
		for (String testFile : Arrays.array("Simple", "Statics", "Wildcards")) {
			try {
				cleanUpTest(testFile, config -> {});
			} catch (ComparisonFailure e) {
				throw new ComparisonFailure(testFile + " - " + e.getMessage(), e.getExpected(), e.getActual());
			}
		}
	}

	@Test
	public void invalidConfiguration() throws Throwable {
		//Smoke test, no exceptions expected
		cleanUpTest("", "", config -> {
			config.put("invalid.key", "some.value");
		});
		cleanUpTest("", "", config -> {
			config.put(JavaCore.COMPILER_SOURCE, "-42");
		});
		cleanUpTest("", "", config -> {
			config.put(JavaCore.COMPILER_SOURCE, "Not an integer");
		});
	}

	@Test
	public void importConfiguration() throws Throwable {
		String defaultOrganizedInput = TEST_DATA.input("ImportConfiguration");
		cleanUpTest(defaultOrganizedInput, defaultOrganizedInput, config -> {});

		String customOrganizedOutput = TEST_DATA.afterOrganizedImports("ImportConfiguration");
		cleanUpTest(defaultOrganizedInput, customOrganizedOutput, config -> {
			config.put(CodeStyleConfiguration.ORGIMPORTS_IMPORTORDER, "foo;#foo;");
		});
	}

	private static void cleanUpTest(final String fileName, final Consumer<Properties> config) throws Exception {
		cleanUpTest(TEST_DATA.input(fileName), TEST_DATA.afterCleanUp(fileName), config);
	}

	private static void cleanUpTest(final String input, final String expected, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		EclipseJdtCleanUpStepImpl formatter = new EclipseJdtCleanUpStepImpl(properties);
		String output = formatter.format(input, SOURCE_FILE_PATH);
		assertEquals("Unexpected clean-up result " + TestData.toString(properties),
				expected, output);
	}
}
