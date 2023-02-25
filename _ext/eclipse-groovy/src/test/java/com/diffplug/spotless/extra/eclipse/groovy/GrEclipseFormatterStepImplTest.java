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
package com.diffplug.spotless.extra.eclipse.groovy;

import static com.diffplug.spotless.extra.eclipse.groovy.GrEclipseFormatterStepImpl.IGNORE_FORMATTER_PROBLEMS;
import static org.codehaus.groovy.eclipse.refactoring.PreferenceConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.jdt.core.JavaCore;
import org.junit.jupiter.api.Test;

/** Smoke test checking that transitive dependencies are complete. */
class GrEclipseFormatterStepImplTest {

	private final static TestData TEST_DATA = TestData.getTestDataOnFileSystem();
	private final static String PARSER_EXCEPTION = "class Test { void method() {} ";
	private final static String SCANNER_EXCEPTION = "{";
	private final static String BOUNDED_WILDCARDS_UNFORMATTED = "foo(Map<String, ? extends  Object> e)\n{\ne.clear();\n}";
	private final static String BOUNDED_WILDCARDS_FORMATTED = "foo(Map<String, ? extends  Object> e) {\n\te.clear();\n}";

	@Test
	void defaultFormat() throws Throwable {
		String output = format(TEST_DATA.input("nominal.test"), config -> {});
		assertEquals(TEST_DATA.expected("nominal.test"),
				output, "Unexpected default formatting.");
	}

	@Test
	void validConfiguration() throws Throwable {
		String output = format(TEST_DATA.input("nominal.test"), config -> {
			config.put(GROOVY_FORMATTER_REMOVE_UNNECESSARY_SEMICOLONS, "true");
		});
		assertEquals(TEST_DATA.expected("nominal.test").replace(";", ""),
				output, "Unexpected formatting for custom configuration.");
	}

	@Test
	void invalidConfiguration() throws Throwable {
		String output = format(TEST_DATA.input("nominal.test"), config -> {
			config.put(GROOVY_FORMATTER_INDENTATION, JavaCore.SPACE);
			config.put(GROOVY_FORMATTER_INDENTATION_SIZE, "noInteger");
		});
		assertEquals(TEST_DATA.expected("nominal.test").replace("\t", "    "),
				output, "Groovy formatter does not replace invalid preferences by their defaults.");
	}

	/** Test the handling AntlrParserPlugin exceptions by GroovyLogManager.manager logging */
	@Test
	void parserException() throws Throwable {
		assertThrows(IllegalArgumentException.class, () -> format(PARSER_EXCEPTION, config -> {}));
	}

	/** Test the handling GroovyDocumentScanner exceptions by GroovyCore logging */
	@Test
	void scannerException() throws Throwable {
		assertThrows(IllegalArgumentException.class, () -> format(SCANNER_EXCEPTION, config -> {}));
	}

	/**
	 * Test the handling bounded wildcards templates
	 * No exception since Groovy-Eclipse 3.0.0.
	 * Formatting fixed with Groovy-Eclipse 3.14 (org.codehaus.groovy:groovy[3.+]).
	 */
	@Test
	void boundedWildCards() throws Throwable {
		String output = format(BOUNDED_WILDCARDS_UNFORMATTED, config -> {});
		assertEquals(BOUNDED_WILDCARDS_FORMATTED,
				output, "Unexpected formatting after bounded wildcards.");
	}

	@Test
	void ignoreCompilerProblems() throws Throwable {
		Consumer<Properties> ignoreCompilerProblems = config -> {
			config.setProperty(IGNORE_FORMATTER_PROBLEMS, "true");
		};
		format(PARSER_EXCEPTION, ignoreCompilerProblems);
		format(SCANNER_EXCEPTION, ignoreCompilerProblems);
		//Test is passed if it does not throw an exception. See issue 237.
	}

	private static String format(final String input, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		GrEclipseFormatterStepImpl formatter = new GrEclipseFormatterStepImpl(properties);
		return formatter.format(input);
	}

}
