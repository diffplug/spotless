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
package com.diffplug.gradle.spotless.groovy.eclipse;

import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

/** Smoke test checking that transitive dependencies are complete. */
public class GrEclipseFormatterStepImplTest {

	private final static String INPUT = "class Test { void method() {} }";
	private final static String OUTPUT = "class Test {\n\tvoid method() {}\n}";
	private final static String PARSER_EXCEPTION = "class Test { void method() {} ";
	private final static String SCANNER_EXCEPTION = "{";
	private static GrEclipseFormatterStepImpl FORMATTER = null;

	@BeforeClass
	public static void initializeTestData() throws Exception {
		FORMATTER = new GrEclipseFormatterStepImpl(new Properties());
	}

	/**
	 * Test nominal scenario
	 * @throws Throwable No exception is expected
	 */
	@Test
	public void formatDefault() throws Throwable {
		String output = FORMATTER.format(INPUT);
		assertTrue(output.equals(OUTPUT));
	}

	/**
	 * Test the handling AntlrParserPlugin exceptions by SpotlessCstReporter logging
	 * @throws Throwable IllegalArgumentException is expected
	 */
	@Test(expected = IllegalArgumentException.class)
	public void parserException() throws Throwable {
		FORMATTER.format(PARSER_EXCEPTION);
	}

	/**
	 * Test the handling GroovyDocumentScanner exceptions by GroovyCore logging
	 * @throws Throwable IllegalArgumentException is expected
	 */
	@Test(expected = IllegalArgumentException.class)
	public void scannerException() throws Throwable {
		FORMATTER.format(SCANNER_EXCEPTION);
	}

}
