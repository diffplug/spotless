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
import static org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames.*;
import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class EclipseCssFormatterStepImplTest {

	private final static String ILLEGAL_CHAR = Character.toString((char) 254);
	private final static String UNFORMATTED = " body {a: v; b: v;}\n".replaceAll("\n", LINE_DELIMITER);
	private final static String FORMATTED = "BODY {\n\ta: v;\n\tb: v;\n}".replaceAll("\n", LINE_DELIMITER);
	private final static String PRE_CODE_UNFORMATTED = "/**<pre>\"Hello\"</pre>*/\n".replaceAll("\n", LINE_DELIMITER);

	private EclipseCssFormatterStepImpl formatter;

	@Before
	public void initialize() throws Exception {
		//The instantiation can be repeated for each step, but only with the same configuration
		Properties properties = new Properties();
		properties.put(INDENTATION_CHAR, TAB); //Done be formatter
		properties.put(CLEANUP_CASE_SELECTOR, Integer.toString(UPPER)); //Done by cleanup
		formatter = new EclipseCssFormatterStepImpl(properties);
	}

	@Test
	public void format() throws Exception {
		String output = formatter.format(UNFORMATTED);
		assertEquals("Unexpected formatting with default preferences.",
				FORMATTED, output);
	}

	@Test
	public void illegalCharacter() throws Exception {
		String output = formatter.format(ILLEGAL_CHAR + UNFORMATTED);
		assertThat(output).as("Illeagl characters are not handled on best effort basis.").contains("BODY {");
	}

	@Test
	public void illegalSyntax() throws Exception {
		String output = formatter.format("{" + UNFORMATTED);
		assertEquals("Illeagl syntax is not handled on best effort basis.",
				"{" + LINE_DELIMITER + FORMATTED, output);
	}

	@Test
	public void formatComment() throws Exception {
		String output = formatter.format(PRE_CODE_UNFORMATTED + UNFORMATTED);
		assertEquals("Unexpected formatting of cpomments.",
				PRE_CODE_UNFORMATTED + FORMATTED, output);
	}

}
