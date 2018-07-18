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

import static com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.LINE_DELIMITER;
import static org.junit.Assert.*;

import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.junit.Test;

/** Eclipse JDT wrapper integration tests */
public class EclipseJdtFormatterStepImplTest {

	private final static String UNFORMATTED = "package com.diffplug.gradle.spotless;\n" +
			"public class C {\n" +
			"  static void hello() {" +
			"    System.out.println(\"Hello World!\");\n" +
			"  }\n" +
			"}".replaceAll("\n", LINE_DELIMITER);
	private final static String FORMATTED = "package com.diffplug.gradle.spotless;\n" +
			"public class C {\n" +
			"\tstatic void hello() {\n" +
			"\t\tSystem.out.println(\"Hello World!\");\n" +
			"\t}\n" +
			"}".replaceAll("\n", LINE_DELIMITER);

	private final static String PRE_UNFORMATTED = "/**<pre>void f(){}</pre>*/\n".replaceAll("\n", LINE_DELIMITER);
	private final static String PRE_FORMATTED = "/**\n * <pre>\n * void f() {\n * }\n * </pre>\n */\n".replaceAll("\n", LINE_DELIMITER);

	private final static String ILLEGAL_CHAR = Character.toString((char) 254);

	@Test
	public void defaultFormat() throws Throwable {
		String output = format(UNFORMATTED, config -> {});
		assertEquals("Unexpected formatting with default preferences.",
				FORMATTED, output);
	}

	@Test
	public void invalidFormat() throws Throwable {
		String output = format(FORMATTED.replace("void hello() {", "void hello()  "), config -> {});
		assertTrue("Incomplete Java not formatted on best effort basis.", output.contains("void hello()  " + LINE_DELIMITER));
	}

	@Test
	public void invalidCharater() throws Throwable {
		String output = format(FORMATTED.replace("void hello() {", "void hello()" + ILLEGAL_CHAR + " {"), config -> {});
		assertTrue("Invalid charater not formatted on best effort basis.", output.contains("void hello()" + ILLEGAL_CHAR + " {" + LINE_DELIMITER));
	}

	@Test
	public void invalidConfiguration() throws Throwable {
		String output = format(FORMATTED, config -> {
			config.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
			config.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "noInteger");
		});
		assertEquals("Invalid indentation configuration not replaced by default value (4 spaces)",
				FORMATTED.replace("\t", "    "), output);
	}

	@Test
	/**	Test that an internal code formatter can be created to format the Java code within HTML pre-tags. (see also Spotless issue #191) */
	public void internalCodeFormatter() throws Throwable {
		String output = format(PRE_UNFORMATTED + UNFORMATTED, config -> {
			config.setProperty(
					DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER,
					DefaultCodeFormatterConstants.TRUE);
		});
		assertEquals("Code within HTML <pre> tag not formatted.",
				PRE_FORMATTED + FORMATTED, output);
	}

	private static String format(final String input, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		EclipseJdtFormatterStepImpl formatter = new EclipseJdtFormatterStepImpl(properties);
		return formatter.format(input);
	}

}
