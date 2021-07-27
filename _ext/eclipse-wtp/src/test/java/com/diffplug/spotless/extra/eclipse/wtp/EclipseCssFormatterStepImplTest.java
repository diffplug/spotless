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
import static org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EclipseCssFormatterStepImplTest {

	private final static String ILLEGAL_CHAR = Character.toString((char) 254);
	private final static String UNFORMATTED = " body {a: v; b: v;}\n".replaceAll("\n", LINE_DELIMITER);
	private final static String FORMATTED = "BODY {\n   a: v;\n   b: v;\n}".replaceAll("\n", LINE_DELIMITER);
	private final static String PRE_CODE_UNFORMATTED = "/**<pre>\"Hello\"</pre>*/\n".replaceAll("\n", LINE_DELIMITER);

	private EclipseCssFormatterStepImpl formatter;

	@BeforeEach
	void initialize() throws Exception {
		/*
		 * The instantiation can be repeated for each step, but only with the same configuration
		 * All formatter configuration is stored in
		 * org.eclipse.core.runtime/.settings/org.eclipse.wst.css.core.prefs.
		 */
		Properties properties = new Properties();
		properties.put(INDENTATION_SIZE, "3");
		properties.put(INDENTATION_CHAR, SPACE); //Default is TAB
		properties.put(CLEANUP_CASE_SELECTOR, Integer.toString(UPPER)); //Done by cleanup
		formatter = new EclipseCssFormatterStepImpl(properties);
	}

	@Test
	void format() throws Exception {
		String output = formatter.format(UNFORMATTED);
		assertEquals(FORMATTED,
				output, "Unexpected formatting with default preferences.");
	}

	@Test
	void illegalCharacter() throws Exception {
		String output = formatter.format(ILLEGAL_CHAR + UNFORMATTED);
		assertThat(output).as("Illeagl characters are not handled on best effort basis.").contains("BODY {");
	}

	@Test
	void illegalSyntax() throws Exception {
		String output = formatter.format("{" + UNFORMATTED);
		assertEquals("{" + LINE_DELIMITER + FORMATTED,
				output, "Illeagl syntax is not handled on best effort basis.");
	}

	@Test
	void formatComment() throws Exception {
		String output = formatter.format(PRE_CODE_UNFORMATTED + UNFORMATTED);
		assertEquals(PRE_CODE_UNFORMATTED + FORMATTED,
				output, "Unexpected formatting of cpomments.");
	}

	@Test
	void configurationChange() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> new EclipseCssFormatterStepImpl(new Properties()));
	}

}
