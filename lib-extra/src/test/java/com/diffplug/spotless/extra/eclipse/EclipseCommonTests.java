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
package com.diffplug.spotless.extra.eclipse;

import static org.assertj.core.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;

/**
 * Provides a common set of tests for all Spotless Eclipse Formatter steps provided by
 * lib-extra.
 * <p>
 * The external Spotless Eclipse Formatter Step implementations are responsible
 * to test the functionality of the Spotless adaptations for the Eclipse formatter.
 * This explicitly includes a check of various forms of valid and invalid input
 * to cover all relevant execution paths during the code formatting and internal
 * exception handling.
 * </p>
 * <p>
 * The lib-extra users, like plugin-gradle and plugin-maven are responsible
 * to test the correct provision of user settings to the generic
 * Spotless Eclipse Formatter steps provided by lib-extra.
 * </p>
 */
public abstract class EclipseCommonTests extends ResourceHarness {

	/** Returns the complete set of versions supported by the formatter */
	protected abstract String[] getSupportedVersions();

	/**
	 * Returns the input which shall be used with the formatter version.
	 * The input shall be very simple and supported if possible by all
	 * formatter versions.
	 */
	protected abstract String getTestInput(String version);

	/**
	 * Returns the output which is expected from the formatter step.
	 * If possible, the output shall be equal for all versions,
	 * but since the default formatter preferences are used, this
	 * might not be achieved for all versions.
	 */
	protected abstract String getTestExpectation(String version);

	/** Create formatter step for a specific version */
	protected abstract FormatterStep createStep(String version);

	@Test
	void testSupportedVersions() throws Exception {
		String[] versions = getSupportedVersions();
		for (String version : versions) {
			String input = getTestInput(version);
			String expected = getTestExpectation(version);
			File inputFile = setFile("someInputFile").toContent(input);
			FormatterStep step = null;
			try {
				step = createStep(version);
			} catch (Exception e) {
				fail("Exception occured when instantiating step for version: " + version, e);
			}
			String output = null;
			try {
				output = LineEnding.toUnix(step.format(input, inputFile));
			} catch (Exception e) {
				fail("Exception occured when formatting input with version: " + version, e);
			}
			assertThat(output).as("Formatting output unexpected with version: " + version).isEqualTo(expected);
		}
	}

}
