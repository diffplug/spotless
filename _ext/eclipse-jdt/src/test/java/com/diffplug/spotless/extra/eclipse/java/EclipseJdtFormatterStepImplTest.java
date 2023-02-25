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
package com.diffplug.spotless.extra.eclipse.java;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.ResourceHarness;

/** Eclipse JDT wrapper integration tests */
class EclipseJdtFormatterStepImplTest extends ResourceHarness {

	private static String UNFORMATTED;
	private static String FORMATTED;

	@BeforeAll
	static void beforeAll() throws IOException {
		UNFORMATTED = getTestResource("java/eclipse/JavaCodeUnformatted.test");
		FORMATTED = getTestResource("java/eclipse/JavaCodeFormatted.test");
	}

	private Properties config;

	@BeforeEach
	void beforeEach() throws IOException {
		File settingsFile = createTestFile("java/eclipse/formatter.xml");
		config = FormatterProperties.from(settingsFile).getProperties();
	}

	private final static String ILLEGAL_CHAR = Character.toString((char) 254);

	@Test
	void nominal() throws Throwable {
		assertEquals(FORMATTED, format(UNFORMATTED));
	}

	@Test
	public void invalidSyntax() throws Throwable {
		try {
			String invalidSyntax = FORMATTED.replace("{", "");
			assertEquals(invalidSyntax, format(invalidSyntax));
		} catch (IndexOutOfBoundsException e) {
			/*
			 * Some JDT versions throw exception, but this changed again in later versions.
			 * Anyhow, exceptions are acceptable, since Spotless should format valid Java code.
			 */
		}
	}

	@Test
	void invalidCharater() throws Throwable {
		String invalidInput = UNFORMATTED + ILLEGAL_CHAR;
		assertEquals(invalidInput, format(invalidInput));
	}

	@Test
	void invalidConfiguration() throws Throwable {
		config.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		config.setProperty(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "noInteger");
		String defaultTabReplacement = "    ";
		assertEquals(FORMATTED.replace("\t", defaultTabReplacement), format(FORMATTED));
	}

	@Test
	void htmlPreTag() throws Throwable {
		config.clear();
		config.setProperty(
				DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER,
				DefaultCodeFormatterConstants.TRUE);
		String formatted = getTestResource("java/eclipse/HtmlPreTagFormatted.test");
		String unformatted = getTestResource("java/eclipse/HtmlPreTagUnformatted.test");
		assertEquals(formatted, format(unformatted), "Failed to create internal code formatter. See Spotless issue #191");
	}

	@Test
	void moduleInfo() throws Throwable {
		File settingsFile = createTestFile("java/eclipse/ModuleInfo.prefs");
		config = FormatterProperties.from(settingsFile).getProperties();
		String formatted = getTestResource("java/eclipse/ModuleInfoFormatted.test");
		String unformatted = getTestResource("java/eclipse/ModuleInfoUnformatted.test");
		assertEquals(formatted, format(unformatted, "whatever/module-info.java"), "Jvm9 module info not formatted.");
	}

	private String format(final String input) throws Exception {
		return format(input, "");
	}

	private String format(final String input, final String fileName) throws Exception {
		EclipseJdtFormatterStepImpl formatter = new EclipseJdtFormatterStepImpl(config);
		return formatter.format(input, new File(fileName));
	}

}
