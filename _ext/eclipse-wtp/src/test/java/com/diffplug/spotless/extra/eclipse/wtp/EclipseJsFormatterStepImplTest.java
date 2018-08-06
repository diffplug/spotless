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
package com.diffplug.spotless.extra.eclipse.wtp;

import static com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.LINE_DELIMITER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.junit.Test;

public class EclipseJsFormatterStepImplTest {
	private final static String ILLEGAL_CHAR = Character.toString((char) 254);
	private final static String UNFORMATTED = "var TEST = TEST || {};\n" +
			"TEST.say = function() {\n" +
			"  console.log(\"Hello world!\"); }\n".replaceAll("\n", LINE_DELIMITER);
	private final static String FORMATTED = "var TEST = TEST || {};\n" +
			"TEST.say = function () {\n" +
			"    console.log(\"Hello world!\");\n}\n".replaceAll("\n", LINE_DELIMITER);
	// Single line comment remains untouched
	private final static String SINGLE_LINE_COMMENT = "//  One line \"hello world\"\n".replaceAll("\n", LINE_DELIMITER);
	// JavaDoc comment get indentation. Within PPRE code, HTML entities are escaped.
	private final static String PRE_CODE_UNFORMATTED = "/**<pre>\"Hello\"</pre>*/\n".replaceAll("\n", LINE_DELIMITER);
	private final static String PRE_CODE_FORMATTED = "/**\n * <pre>\n * &quot;Hello&quot;\n * </pre>\n */\n".replaceAll("\n", LINE_DELIMITER);

	@Test
	public void defaultFormat() throws Exception {
		String output = format(UNFORMATTED, config -> {});
		assertEquals("Unexpected formatting with default preferences.",
				FORMATTED, output);
	}

	@Test
	public void invalidSyntax() throws Exception {
		boolean exceptionCaught = false;
		try {
			format(UNFORMATTED.replace("()", ""), config -> {});
		} catch (IllegalArgumentException e) {
			exceptionCaught = true;
			assertThat(e.getMessage()).as("Exception has no hint about invalid syntax.").contains(Arrays.asList("Invalid", "syntax"));
		}
		assertThat(exceptionCaught).as("No IllegalArgumentException thrown for invalid syntax.").isTrue();
	}

	@Test
	public void illegalCharacter() throws Exception {
		String output = format(UNFORMATTED.replace("function", "function" + ILLEGAL_CHAR), config -> {});
		assertThat(output).as("Illegal ASCII charactes are not treated on best effort basis.").contains("function" + ILLEGAL_CHAR);
	}

	@Test
	public void validConfiguration() throws Exception {
		String output = format(UNFORMATTED, config -> {
			config.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaScriptCore.TAB);
		});
		assertEquals("User configuration ignored by formatter.",
				FORMATTED.replace("    ", "\t"), output);
	}

	@Test
	public void invalidConfiguration() throws Exception {
		String output = format(UNFORMATTED, config -> {
			config.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "-1");
		});
		assertEquals("Invalid user configuration not treated on best effort basis.",
				FORMATTED.replace("    ", ""), output);
	}

	@Test
	public void formatComments() throws Exception {
		String output = format(SINGLE_LINE_COMMENT + PRE_CODE_UNFORMATTED + UNFORMATTED, config -> {});
		assertEquals("Invalid user configuration not treated on best effort basis.",
				SINGLE_LINE_COMMENT + PRE_CODE_FORMATTED + FORMATTED, output);
	}

	private static String format(final String input, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		EclipseJsFormatterStepImpl formatter = new EclipseJsFormatterStepImpl(properties);
		return formatter.format(input);
	}

}
