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
package com.diffplug.spotless.extra.java;

import java.util.Properties;
import java.util.function.Consumer;

import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.eclipse.EquoResourceHarness;

import org.assertj.core.util.Arrays;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/** Eclipse JDT wrapper integration tests */
public class EclipseJdtOrganizeImportStepImplTest extends EquoResourceHarness {
	private final static String INPUT = "package p; class C{}";
	private final static String EXPECTED = "package p;\nclass C {\n}";

	private static TestData TEST_DATA = null;

	@BeforeAll
	public static void initializeStatic() throws Exception {
		TEST_DATA = TestData.getTestDataOnFileSystem();
	}

	private static EquoBasedStepBuilder createBuilder() {
		return EclipseJdtOrganizeImportStep.createBuilder(TestProvisioner.mavenCentral());
	}

	public EclipseJdtOrganizeImportStepImplTest() {
		super(createBuilder(), INPUT, EXPECTED);
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
			} catch (AssertionFailedError e) {
				throw new AssertionFailedError(testFile + " - " + e.getMessage(), e.getExpected(), e.getActual());
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
		Assertions.assertEquals("Unexpected import organization " + TestData.toString(properties),
				expected, output);
	}
}
