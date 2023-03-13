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
public class EclipseJdtOrganizeImportStepImplTest {
	private static TestData TEST_DATA = null;

	@BeforeClass
	public static void initializeStatic() throws Exception {
		TEST_DATA = TestData.getTestDataOnFileSystem();
	}

	@Test
	public void emptyInput() throws Throwable {
		organizeImportTest("", "", config -> {});
	}

	@Test
	public void defaultConfiguration() throws Throwable {
		for (String testFile : Arrays.array("Simple", "Statics", "Wildcards")) {
			try {
				organizeImportTest(testFile, config -> {});
			} catch (ComparisonFailure e) {
				throw new ComparisonFailure(testFile + " - " + e.getMessage(), e.getExpected(), e.getActual());
			}
		}
	}

	@Test
	public void defaultPackage() throws Throwable {
		String input = TEST_DATA.input("Simple").replaceFirst("package .+", "");
		String expected = TEST_DATA.afterOrganizedImports("Simple").replaceFirst("package .+", "");
		organizeImportTest(input, expected, config -> {});
	}

	@Test
	public void invalidConfiguration() throws Throwable {
		//Smoke test, no exceptions expected
		organizeImportTest("", "", config -> {
			config.put("invalid.key", "some.value");
		});
		organizeImportTest("", "", config -> {
			config.put(JavaCore.COMPILER_SOURCE, "-42");
		});
		organizeImportTest("", "", config -> {
			config.put(JavaCore.COMPILER_SOURCE, "Not an integer");
		});
	}

	@Test
	public void customConfiguration() throws Throwable {
		String defaultOrganizedInput = TEST_DATA.input("ImportConfiguration");
		organizeImportTest(defaultOrganizedInput, defaultOrganizedInput, config -> {});

		String customOrganizedOutput = TEST_DATA.afterOrganizedImports("ImportConfiguration");
		organizeImportTest(defaultOrganizedInput, customOrganizedOutput, config -> {
			config.put(CodeStyleConfiguration.ORGIMPORTS_IMPORTORDER, "foo;#foo;");
		});
	}

	private static void organizeImportTest(final String fileName, final Consumer<Properties> config) throws Exception {
		organizeImportTest(TEST_DATA.input(fileName), TEST_DATA.afterOrganizedImports(fileName), config);
	}

	private static void organizeImportTest(final String input, final String expected, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		EclipseJdtOrganizeImportStepImpl formatter = new EclipseJdtOrganizeImportStepImpl(properties);
		String output = formatter.format(input);
		assertEquals("Unexpected import organization " + TestData.toString(properties),
				expected, output);
	}
}
