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

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import com.diffplug.spotless.extra.eclipse.wtp.sse.SpotlessPreferences;

/** Test configuration allowExternalURI=false */
public class EclipseXmlFormatterStepImplAllowExternalURIsTest {
	private static TestData TEST_DATA = null;

	@BeforeClass
	public static void initializeStatic() throws Exception {
		TEST_DATA = TestData.getTestDataOnFileSystem("xml");
	}

	@Test
	public void dtdExternalPath() throws Throwable {
		String output = format(TEST_DATA.input("dtd_external.test"));
		assertEquals("External DTD not resolved. Restrictions are not applied by formatter.",
				TEST_DATA.expected("dtd_external.test"), output);
	}

	@Test
	public void xsdExternalPath() throws Throwable {
		String output = format(TEST_DATA.input("xsd_external.test"));
		assertEquals("External XSD not resolved. Restrictions are not applied by formatter.",
				TEST_DATA.expected("xsd_external.test"), output);
	}

	private static String format(final String[] input) throws Exception {
		return format(input[0], input[1]);
	}

	private static String format(final String input, final String location) throws Exception {
		Properties properties = new Properties();
		properties.put(SpotlessPreferences.RESOLVE_EXTERNAL_URI, "TRUE");
		EclipseXmlFormatterStepImpl formatter = new EclipseXmlFormatterStepImpl(properties);
		return formatter.format(input, location);
	}
}
