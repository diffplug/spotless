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
import static org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames.*;
import static org.junit.Assert.*;

import java.util.Properties;
import java.util.function.Consumer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.diffplug.spotless.extra.eclipse.wtp.sse.SpotlessPreferences;

/** Eclipse WST wrapper integration tests */
public class EclipseXmlFormatterStepImplTest {

	private final static String INCOMPLETE = "<c>";
	private final static String ILLEGAL_CHAR = "\0";
	private static TestData TEST_DATA = null;

	@BeforeClass
	public static void initializeStatic() throws Exception {
		TEST_DATA = TestData.getTestDataOnFileSystem("xml");
	}

	@Test
	public void simpleDefaultFormat() throws Throwable {
		String output = format(TEST_DATA.input("xml_space.test"), config -> {});
		assertEquals("Unexpected formatting with default preferences.",
				TEST_DATA.expected("xml_space.test"), output);
	}

	@Test
	public void invalidXmlFormat() throws Throwable {
		String[] input = TEST_DATA.input("xml_space.test");
		input[0] += INCOMPLETE;
		String output = format(input, config -> {});
		String expected = TEST_DATA.expected("xml_space.test") + LINE_DELIMITER + INCOMPLETE;
		assertEquals("Incomplete XML not formatted on best effort basis.",
				expected, output);
	}

	@Test
	public void illegalXmlCharater() throws Throwable {
		String[] input = TEST_DATA.input("xml_space.test");
		input[0] = ILLEGAL_CHAR + input[0];
		String output = format(input, config -> {});
		String expected = LINE_DELIMITER + LINE_DELIMITER + TEST_DATA.expected("xml_space.test");
		assertEquals("Illegal character not replaced by line delimiter.", expected, output);
	}

	@Test
	public void multipleConfigurations() throws Throwable {
		String output = format(TEST_DATA.input("xml_space.test"), config -> {
			config.setProperty(INDENTATION_SIZE, "2");
			config.setProperty(INDENTATION_CHAR, SPACE);
		});
		String expected = TEST_DATA.expected("xml_space.test").replace("\t", "  ");
		assertEquals("Custom indentation configuration not applied.", expected, output);

		output = format(TEST_DATA.input("xml_space.test"), config -> {
			config.setProperty(SPLIT_MULTI_ATTRS, Boolean.toString(true));
		});
		expected = TEST_DATA.expected("xml_space.test");
		expected = expected.replace(" a=", LINE_DELIMITER + "\ta=");
		expected = expected.replace(" b=", LINE_DELIMITER + "\tb=");
		assertEquals("Custom indentation configuration not reverted or custom multiple argument configuration not applied.", expected, output);
	}

	@Test
	public void invalidConfiguration() throws Throwable {
		String output = format(TEST_DATA.input("xml_space.test"), config -> {
			config.setProperty(INDENTATION_SIZE, "Not an integer");
			config.setProperty(INDENTATION_CHAR, SPACE);
		});
		assertEquals("Invalid indentation configuration not replaced by default value (0 spaces)",
				TEST_DATA.expected("xml_space.test").replace("\t", ""), output);
	}

	@Test
	public void dtdRelativePath() throws Throwable {
		String output = format(TEST_DATA.input("dtd_relative.test"), config -> {});
		assertEquals("Relative DTD not resolved. Restrictions are not applied by formatter.",
				TEST_DATA.expected("dtd_relative.test"), output);
	}

	@Test
	public void dtdExternalPath() throws Throwable {
		String output = format(TEST_DATA.input("dtd_external.test"), config -> {});
		assertNotEquals("External DTD resolved by default. Restrictions are applied by formatter.",
				TEST_DATA.expected("dtd_external.test"), output);
	}
	
	@Test
	public void xsdRelativePath() throws Throwable {
		String output = format(TEST_DATA.input("xsd_relative.test"), config -> {});
		assertEquals("Relative XSD not resolved. Restrictions are not applied by formatter.",
				TEST_DATA.expected("xsd_relative.test"), output);
	}

	@Test
	public void xsdExternalPath() throws Throwable {
		String output = format(TEST_DATA.input("xsd_external.test"), config -> {});
		assertNotEquals("External XSD resolved per default. Restrictions are applied by formatter.",
				TEST_DATA.expected("xsd_external.test"), output);
	}

	@Test
	public void xsdNotFound() throws Throwable {
		String output = format(TEST_DATA.input("xsd_not_found.test"), config -> {});
		assertEquals("Unresolved XSD/DTD not silently ignored.",
				TEST_DATA.expected("xsd_not_found.test"), output);
	}

	@Test
	public void catalogLookup() throws Throwable {
		String output = format(TEST_DATA.input("xsd_not_found.test"), config -> {
			config.setProperty(
					SpotlessPreferences.USER_CATALOG,
					TEST_DATA.getRestrictionsPath("catalog.xml").toString());
		});
		assertEquals("XSD not resolved by catalog. Restrictions are not applied by formatter.",
				TEST_DATA.expected("xsd_not_found.test").replace(" remove spaces ", "remove spaces"), output);
	}

	private static String format(final String[] input, final Consumer<Properties> config) throws Exception {
		return format(input[0], input[1], config);
	}

	private static String format(final String input, final String location, final Consumer<Properties> config) throws Exception {
		Properties properties = new Properties();
		config.accept(properties);
		EclipseXmlFormatterStepImpl formatter = new EclipseXmlFormatterStepImpl(properties);
		return formatter.format(input, location);
	}

}
