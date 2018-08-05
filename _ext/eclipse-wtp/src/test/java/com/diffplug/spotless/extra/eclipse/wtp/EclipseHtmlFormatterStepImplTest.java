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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames.*;
import static org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.junit.BeforeClass;
import org.junit.Test;

//@RunWith(QuarantiningRunner.class)
//@Quarantine({"org.eclipse", "org.osgi", "com.diffplug"})
public class EclipseHtmlFormatterStepImplTest {

	private static TestData TEST_DATA = null;

	@BeforeClass
	public static void initializeStatic() throws Exception {
		TEST_DATA = TestData.getTestDataOnFileSystem("html");
	}

	@Test
	public void formatHtml4() throws Exception {
		String output = format(TEST_DATA.input("html4.html"), config -> {});
		assertEquals("Unexpected HTML4 formatting.",
				TEST_DATA.expected("html4.html"), output);
	}

	@Test
	public void formatHtml5() throws Exception {
		String output = format(TEST_DATA.input("html5.html"), config -> {});
		assertEquals("Unexpected HTML5 formatting.",
				TEST_DATA.expected("html5.html"), output);
	}

	@Test
	public void changeHtmlConfiguration() throws Exception {
		String output = format(TEST_DATA.input("html5.html"), config -> {});
		assertEquals("Unexpected HTML5 formatting with default configuration.",
				TEST_DATA.expected("html5.html"), output);
		output = format(TEST_DATA.input("html5.html"), config -> {
			config.put(CLEANUP_TAG_NAME_CASE, Integer.toString(HTMLCorePreferenceNames.UPPER));
		});
		assertEquals("Unexpected HTML5 formatting with custom configuration.",
				TEST_DATA.expected("html5_upper.html"), output);
		output = format(TEST_DATA.input("html5.html"), config -> {});
		assertEquals("Unexpected HTML5 formatting after reset of default configuration.",
				TEST_DATA.expected("html5.html"), output);
	}

	@Test
	public void invalidConfiguration() throws Throwable {
		String output = format(TEST_DATA.input("html5.html"), config -> {
			config.put(TAG_NAME_CASE, "Not an integer");
		});
		assertEquals("Unexpected HTML5 formatting with invlaid configuration.",
				TEST_DATA.expected("html5.html"), output);
	}

	@Test
	public void invalidSyntax() throws Exception {
		String output = format(TEST_DATA.input("invalid_syntax.html"), config -> {});
		assertEquals("Unexpected HTML formatting in case syntax is not valid.",
				TEST_DATA.expected("invalid_syntax.html"), output);
	}

	@Test
	public void formatJavaScript() throws Exception {
		String output = format(TEST_DATA.input("javascript.html"), config -> {});
		assertEquals("Unexpected JS formatting.",
				TEST_DATA.expected("javascript.html"), output);
	}

	@Test
	public void changeJsConfiguration() throws Exception {
		String output = format(TEST_DATA.input("javascript.html"), config -> {});
		assertEquals("Unexpected JS formatting with default configuration.",
				TEST_DATA.expected("javascript.html"), output);
		output = format(TEST_DATA.input("javascript.html"), config -> {
			config.put(FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON, JavaScriptCore.INSERT);
		});
		assertEquals("Unexpected JS formatting with custom configuration.",
				TEST_DATA.expected("javascript_semicolon.html"), output);
		output = format(TEST_DATA.input("javascript.html"), config -> {});
		assertEquals("Unexpected HTML5 formatting after reset of default configuration.",
				TEST_DATA.expected("javascript.html"), output);
	}

	@Test
	public void formatCSS() throws Exception {
		String output = format(TEST_DATA.input("css.html"), config -> {

		});
		assertEquals("Unexpected CSS formatting.",
				TEST_DATA.expected("css.html"), output);
	}

	@Test
	public void changeCssConfiguration() throws Exception {
		format(TEST_DATA.input("css.html"), config -> {});
		boolean exceptionCaught = false;
		try {
			format(TEST_DATA.input("css.html"), config -> {
				config.put(QUOTE_ATTR_VALUES, "TRUE");
			});
		} catch (IllegalArgumentException e) {
			exceptionCaught = true;
			assertThat(e.getMessage()).as("Exception has no hint about multiple configurations.").contains(Arrays.asList("multiple", "configurations"));
		}
		assertThat(exceptionCaught).as("No IllegalArgumentException thrown for reconfiguration of CSS formatter.").isTrue();
	}

	private static String format(final String[] input, final Consumer<Properties> config) throws Exception {
		return format(input[0], config);
	}

	private static String format(final String input, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		EclipseHtmlFormatterStepImpl formatter = new EclipseHtmlFormatterStepImpl(properties);
		return formatter.format(input);
	}
}
