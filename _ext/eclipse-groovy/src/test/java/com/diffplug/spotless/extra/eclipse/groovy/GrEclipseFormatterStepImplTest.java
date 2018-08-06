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
package com.diffplug.spotless.extra.eclipse.groovy;

import static com.diffplug.spotless.extra.eclipse.groovy.GrEclipseFormatterStepImpl.IGNORE_FORMATTER_PROBLEMS;
import static org.codehaus.groovy.eclipse.refactoring.PreferenceConstants.*;
import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;

/** Smoke test checking that transitive dependencies are complete. */
public class GrEclipseFormatterStepImplTest {

	private final static TestData TEST_DATA = TestData.getTestDataOnFileSystem();
	private final static String PARSER_EXCEPTION = "class Test { void method() {} ";
	private final static String SCANNER_EXCEPTION = "{";
	private final static String BOUNDED_WILDCARDS = "foo(Map<String, ? extends Object> e) {}";

	@Test
	public void defaultFormat() throws Throwable {
		String output = format(TEST_DATA.input("nominal.test"), config -> {});
		assertEquals("Unexpected default formatting.",
				TEST_DATA.expected("nominal.test"), output);
	}

	@Test
	public void validConfiguration() throws Throwable {
		String output = format(TEST_DATA.input("nominal.test"), config -> {
			config.put(GROOVY_FORMATTER_REMOVE_UNNECESSARY_SEMICOLONS, "true");
		});
		assertEquals("Unexpected formatting fro custom configuration.",
				TEST_DATA.expected("nominal.test").replace(";", ""), output);
	}

	@Test
	public void invalidConfiguration() throws Throwable {
		String output = format(TEST_DATA.input("nominal.test"), config -> {
			config.put(GROOVY_FORMATTER_INDENTATION, JavaCore.SPACE);
			config.put(GROOVY_FORMATTER_INDENTATION_SIZE, "noInteger");
		});
		assertEquals("Groovy formatter does not replace invalid preferences by their defaults.",
				TEST_DATA.expected("nominal.test").replace("\t", "    "), output);
	}

	/** Test the handling AntlrParserPlugin exceptions by GroovyLogManager.manager logging */
	@Test(expected = IllegalArgumentException.class)
	public void parserException() throws Throwable {
		format(PARSER_EXCEPTION, config -> {});
	}

	/** Test the handling GroovyDocumentScanner exceptions by GroovyCore logging */
	@Test(expected = IllegalArgumentException.class)
	public void scannerException() throws Throwable {
		format(SCANNER_EXCEPTION, config -> {});
	}

	/** Test the handling bounded wildcards templates */
	@Test(expected = IllegalArgumentException.class)
	public void boundedWildCards() throws Throwable {
		format(BOUNDED_WILDCARDS, config -> {});
	}

	@Test
	public void ignoreCompilerProblems() throws Throwable {
		Consumer<Properties> ignoreCompilerProblems = config -> {
			config.setProperty(IGNORE_FORMATTER_PROBLEMS, "true");
		};
		format(PARSER_EXCEPTION, ignoreCompilerProblems);
		format(SCANNER_EXCEPTION, ignoreCompilerProblems);
		format(BOUNDED_WILDCARDS, ignoreCompilerProblems);
		//Test is passed if it does not throw an exception. See issue 237.
	}

	private static String format(final String input, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		GrEclipseFormatterStepImpl formatter = new GrEclipseFormatterStepImpl(properties);
		return formatter.format(input);
	}

}
