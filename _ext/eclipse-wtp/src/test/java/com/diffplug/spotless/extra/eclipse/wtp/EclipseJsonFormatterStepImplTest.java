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
import static org.eclipse.wst.json.core.preferences.JSONCorePreferenceNames.*;
import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class EclipseJsonFormatterStepImplTest {
	private final static String ILLEGAL_CHAR = Character.toString((char) 254);
	private final static String UNFORMATTED_OBJECT = "{\n \"x\": { \"a\" : \"v\",\"properties\" : \"v\" }}".replaceAll("\n", LINE_DELIMITER);
	private final static String FORMATTED_OBJECT = "{\n   \"x\": {\n      \"a\": \"v\",\n      \"properties\": \"v\"\n   }\n}".replaceAll("\n", LINE_DELIMITER);
	private final static String UNFORMATTED_ARRAY = "[\n { \"a\" : \"v\",\"properties\" : \"v\" }]".replaceAll("\n", LINE_DELIMITER);
	private final static String FORMATTED_ARRAY = "[\n   {\n      \"a\": \"v\",\n      \"properties\": \"v\"\n   }\n]".replaceAll("\n", LINE_DELIMITER);

	private EclipseJsonFormatterStepImpl formatter;

	@Before
	public void initialize() throws Exception {
		/*
		 * The instantiation can be repeated for each step, but only with the same configuration
		 * All formatter configuration is stored in
		 * org.eclipse.core.runtime/.settings/org.eclipse.wst.json.core.prefs.
		 * So a simple test of one configuration item change is considered sufficient.
		 */
		Properties properties = new Properties();
		properties.put(INDENTATION_SIZE, "3"); //Default is 1
		properties.put(INDENTATION_CHAR, SPACE); //Default is TAB
		properties.put(CASE_PROPERTY_NAME, Integer.toString(UPPER)); //Dead code, ignored
		formatter = new EclipseJsonFormatterStepImpl(properties);
	}

	@Test
	public void formatObject() throws Exception {
		String output = formatter.format(UNFORMATTED_OBJECT);
		assertEquals("Unexpected formatting with default preferences.",
				FORMATTED_OBJECT, output);
	}

	@Test
	public void formatArray() throws Exception {
		String output = formatter.format(UNFORMATTED_ARRAY);
		assertEquals("Unexpected formatting with default preferences.",
				FORMATTED_ARRAY, output);
	}

	@Test
	public void illegalCharacter() throws Exception {
		String output = formatter.format(ILLEGAL_CHAR + UNFORMATTED_OBJECT);
		assertEquals("Illeagl characteds are not ignored.",
				ILLEGAL_CHAR + FORMATTED_OBJECT, output);
	}

	@Test
	public void illegalSyntax() throws Exception {
		String output = formatter.format("{" + UNFORMATTED_OBJECT);
		assertEquals("Illeagl syntax is not handled on best effort basis.",
				FORMATTED_OBJECT, output);
	}

	@Test(expected = IllegalArgumentException.class)
	public void configurationChange() throws Exception {
		new EclipseJsonFormatterStepImpl(new Properties());
	}
}
