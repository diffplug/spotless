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
package com.diffplug.spotless.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.generic.LicenseHeaderStep;

/** The XML extension is discontinued. */
@Deprecated
public class XmlDefaultsTest extends ResourceHarness {

	@Test
	public void testDelimiterExpr() throws Exception {
		final String header = "<!--My tests header-->";
		FormatterStep step = LicenseHeaderStep.createFromHeader(header, XmlDefaults.DELIMITER_EXPR);
		final File dummyFile = setFile("src/main/file.dummy").toContent("");
		for (String testSource : Arrays.asList(
				"<!--XML starts with element-->@\n<a></a>",
				"<!--XML starts with processing instruction -->@\n<?></a>")) {
			String output = null;
			try {
				output = step.format(testSource, dummyFile);
			} catch (IllegalArgumentException e) {
				throw new AssertionError(String.format("No delimiter found in '%s'", testSource), e);
			}
			String expected = testSource.replaceAll("(.*?)\\@", header);
			assertThat(output).isEqualTo(expected).as("Unexpected header insertion for '$s'.", testSource);
		}
	}

}
