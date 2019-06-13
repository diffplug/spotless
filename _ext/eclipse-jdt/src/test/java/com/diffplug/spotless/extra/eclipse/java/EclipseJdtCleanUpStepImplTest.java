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

import org.junit.BeforeClass;
import org.junit.Test;

/** Eclipse JDT wrapper integration tests */
public class EclipseJdtCleanUpStepImplTest {
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
	public void nominal() throws Throwable {
		organizeImportTest("Simple", config -> {});
	}

	private static void organizeImportTest(final String fileName, final Consumer<Properties> config) throws Exception {
		organizeImportTest(TEST_DATA.input(fileName), TEST_DATA.afterOrganizedImports(fileName), config);
	}

	private static void organizeImportTest(final String input, final String expected, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		EclipseJdtCleanUpStepImpl formatter = new EclipseJdtCleanUpStepImpl(properties);
		String output = formatter.organizeImport(input);
		assertEquals("Unexpected import organization " + toString(properties),
				expected, output);
	}

	private static String toString(Properties properties) {
		StringBuilder result = new StringBuilder();
		result.append('[');
		properties.forEach((k, v) -> {
			result.append(k.toString());
			result.append('=');
			result.append(v.toString());
			result.append(';');
		});
		result.append(']');
		return result.toString();
	}

}
