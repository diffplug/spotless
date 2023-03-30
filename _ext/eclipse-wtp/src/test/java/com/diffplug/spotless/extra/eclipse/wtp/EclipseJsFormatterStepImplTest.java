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
package com.diffplug.spotless.extra.eclipse.wtp;

import static com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.LINE_DELIMITER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Properties;

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EclipseJsFormatterStepImplTest {
	private final static String ILLEGAL_CHAR = Character.toString((char) 254);
	private final static String UNFORMATTED = "var TEST = TEST || {};\n" +
			"TEST.say = function() {\n" +
			"  console.log(\"Hello world!\"); }\n".replaceAll("\n", LINE_DELIMITER);
	private final static String FORMATTED = "var TEST = TEST || {};\n" +
			"TEST.say = function () {\n" +
			"\tconsole.log(\"Hello world!\");\n}\n".replaceAll("\n", LINE_DELIMITER);
	// Single line comment remains untouched
	private final static String SINGLE_LINE_COMMENT = "//  One line \"hello world\"\n".replaceAll("\n", LINE_DELIMITER);
	// JavaDoc comment get indentation. Within PPRE code, HTML entities are escaped.
	private final static String PRE_CODE_UNFORMATTED = "/**<pre>\"Hello\"</pre>*/\n".replaceAll("\n", LINE_DELIMITER);
	private final static String PRE_CODE_FORMATTED = "/**\n * <pre>\n * &quot;Hello&quot;\n * </pre>\n */\n".replaceAll("\n", LINE_DELIMITER);

	private EclipseJsFormatterStepImpl formatter;

	@BeforeEach
	void initialize() throws Exception {
		/*
		 * The instantiation can be repeated for each step, but only with the same configuration
		 * All formatter configuration is stored in
		 * org.eclipse.core.runtime/.settings/org.eclipse.jst.jsdt.core.prefs.
		 */
		var properties = new Properties();
		properties.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaScriptCore.TAB);
		formatter = new EclipseJsFormatterStepImpl(properties);
	}

	@Test
	void invalidSyntax() throws Exception {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> formatter.format(UNFORMATTED.replace("()", "")));
		assertThat(error.getMessage()).as("Exception has no hint about invalid syntax.").contains(Arrays.asList("Invalid", "syntax"));
	}

	@Test
	void illegalCharacter() throws Exception {
		String output = formatter.format(UNFORMATTED.replace("function", "function" + ILLEGAL_CHAR));
		assertThat(output).as("Illegal ASCII charactes are not treated on best effort basis.").contains("function" + ILLEGAL_CHAR);
	}

	@Test
	void nominal() throws Exception {
		String output = formatter.format(UNFORMATTED);
		assertEquals(FORMATTED, output, "User configuration ignored by formatter.");
	}

	@Test
	void formatComments() throws Exception {
		String output = formatter.format(SINGLE_LINE_COMMENT + PRE_CODE_UNFORMATTED + UNFORMATTED);
		assertEquals(SINGLE_LINE_COMMENT + PRE_CODE_FORMATTED + FORMATTED,
				output, "Invalid user configuration not treated on best effort basis.");
	}

	@Test
	void configurationChange() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> new EclipseJsFormatterStepImpl(new Properties()));
	}

}
