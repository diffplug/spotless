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
import static org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Eclipse WST wrapper integration tests */
class EclipseXmlFormatterStepImplTest {

	private final static String INCOMPLETE = "<c>";
	private final static String ILLEGAL_CHAR = "\0";

	private TestData testData = null;
	private EclipseXmlFormatterStepImpl formatter;

	@BeforeEach
	void initialize() throws Exception {
		testData = TestData.getTestDataOnFileSystem("xml");
		/*
		 * The instantiation can be repeated for each step, but only with the same configuration
		 * All formatter configuration is stored in
		 * org.eclipse.core.runtime/.settings/org.eclipse.wst.xml.core.prefs.
		 * So a simple test of one configuration item change is considered sufficient.
		 */
		Properties properties = new Properties();
		properties.put(INDENTATION_SIZE, "2");
		properties.put(INDENTATION_CHAR, SPACE); //Default is TAB
		formatter = new EclipseXmlFormatterStepImpl(properties);
	}

	@Test
	void simpleDefaultFormat() throws Throwable {
		String[] input = testData.input("xml_space.test");
		String output = formatter.format(input[0], input[1]);
		assertEquals(testData.expected("xml_space.test"),
				output, "Unexpected formatting with default preferences.");
	}

	@Test
	void invalidXmlFormat() throws Throwable {
		String[] input = testData.input("xml_space.test");
		input[0] += INCOMPLETE;
		String output = formatter.format(input[0], input[1]);
		String expected = testData.expected("xml_space.test") + LINE_DELIMITER + INCOMPLETE;
		assertEquals(expected,
				output, "Incomplete XML not formatted on best effort basis.");
	}

	@Test
	void illegalXmlCharater() throws Throwable {
		String[] input = testData.input("xml_space.test");
		input[0] = ILLEGAL_CHAR + input[0];
		String output = formatter.format(input[0], input[1]);
		String expected = LINE_DELIMITER + LINE_DELIMITER + testData.expected("xml_space.test");
		assertEquals(expected, output, "Illegal character not replaced by line delimiter.");
	}

	@Test
	void dtdRelativePath() throws Throwable {
		String[] input = testData.input("dtd_relative.test");
		String output = formatter.format(input[0], input[1]);
		assertEquals(testData.expected("dtd_relative.test"),
				output, "Relative DTD not resolved. Restrictions are not applied by formatter.");
	}

	@Test
	void dtdExternalPath() throws Throwable {
		String[] input = testData.input("dtd_external.test");
		String output = formatter.format(input[0], input[1]);
		assertNotEquals(testData.expected("dtd_external.test"),
				output, "External DTD resolved by default. Restrictions are applied by formatter.");
	}

	@Test
	void xsdRelativePath() throws Throwable {
		String[] input = testData.input("xsd_relative.test");
		String output = formatter.format(input[0], input[1]);
		assertEquals(testData.expected("xsd_relative.test"),
				output, "Relative XSD not resolved. Restrictions are not applied by formatter.");
	}

	@Test
	void xsdExternalPath() throws Throwable {
		String[] input = testData.input("xsd_external.test");
		String output = formatter.format(input[0], input[1]);
		assertNotEquals(testData.expected("xsd_external.test"),
				output, "External XSD resolved per default. Restrictions are applied by formatter.");
	}

	@Test
	void xsdNotFound() throws Throwable {
		String[] input = testData.input("xsd_not_found.test");
		String output = formatter.format(input[0], input[1]);
		assertEquals(testData.expected("xsd_not_found.test"),
				output, "Unresolved XSD/DTD not silently ignored.");
	}

	@Test
	void configurationChange() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> new EclipseXmlFormatterStepImpl(new Properties()));
	}
}
