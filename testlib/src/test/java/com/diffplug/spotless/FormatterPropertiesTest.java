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
package com.diffplug.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.assertj.core.api.AbstractAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.diffplug.spotless.ResourceHarness;

public class FormatterPropertiesTest extends ResourceHarness {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private static final String RESOURCES_ROOT_DIR = "formatter/properties/";

	private static final String[] VALID_SETTINGS_RESOURCES = {
			RESOURCES_ROOT_DIR + "valid_line_oriented.prefs",
			RESOURCES_ROOT_DIR + "valid_line_oriented.properties",
			RESOURCES_ROOT_DIR + "valid_xml_profiles.xml",
			RESOURCES_ROOT_DIR + "valid_xml_properties.xml"
	};

	private static final String[] INVALID_SETTINGS_RESOURCES = {
			RESOURCES_ROOT_DIR + "invalid_xml_profiles.xml",
			RESOURCES_ROOT_DIR + "invalid_xml_profiles_multiple.xml",
			RESOURCES_ROOT_DIR + "invalid_xml_profiles_zero.xml",
			RESOURCES_ROOT_DIR + "invalid_xml_properties.xml"
	};

	private static final String[] VALID_VALUES = {
			"string",
			"true",
			"42",
			null
	};

	@Test
	public void differntPropertyFileTypes() throws IOException {
		for (String settingsResource : VALID_SETTINGS_RESOURCES) {
			File settingsFile = createTestFile(settingsResource);
			FormatterProperties preferences = FormatterProperties.from(settingsFile);
			assertFor(preferences)
					.containsSpecificValuesOf(settingsFile)
					.containsCommonValueOf(settingsFile);
		}
	}

	@Test
	public void multiplePropertyFiles() throws IOException {
		LinkedList<File> settingsFiles = new LinkedList<File>();
		for (String settingsResource : VALID_SETTINGS_RESOURCES) {
			File settingsFile = createTestFile(settingsResource);
			settingsFiles.add(settingsFile);
		}
		FormatterProperties preferences = FormatterProperties.from(settingsFiles);
		/* Settings are loaded / overridden in the sequence they are configured. */
		assertFor(preferences)
				.containsSpecificValuesOf(settingsFiles)
				.containsCommonValueOf(settingsFiles.getLast());
	}

	@Test
	public void invalidPropertyFiles() throws IOException {
		for (String settingsResource : INVALID_SETTINGS_RESOURCES) {
			File settingsFile = createTestFile(settingsResource);
			boolean exceptionCaught = false;
			try {
				FormatterProperties.from(settingsFile);
			} catch (IllegalArgumentException ex) {
				exceptionCaught = true;
				assertThat(ex.getMessage())
						.as("IllegalArgumentException does not contain absolute path of file '%s'", settingsFile.getName())
						.contains(settingsFile.getAbsolutePath());
			}
			assertThat(exceptionCaught)
					.as("No IllegalArgumentException thrown when parsing '%s'", settingsFile.getName())
					.isTrue();
		}
	}

	@Test
	public void nonExisitingFile() throws IOException {
		boolean exceptionCaught = false;
		String filePath = "does/not/exist.properties";
		try {
			FormatterProperties.from(new File(filePath));
		} catch (IllegalArgumentException ex) {
			exceptionCaught = true;
			assertThat(ex.getMessage())
					.as("IllegalArgumentException does not contain path of non-existing file.").contains(filePath);
		}
		assertThat(exceptionCaught)
				.as("No IllegalArgumentException thrown for non-existing file.").isTrue();
	}

	private static class FormatterSettingsAssert extends AbstractAssert<FormatterSettingsAssert, FormatterProperties> {

		public FormatterSettingsAssert(FormatterProperties actual) {
			super(actual, FormatterSettingsAssert.class);
		}

		/** Check that the values form all valid files are part of the settings properties. */
		public FormatterSettingsAssert containsSpecificValuesOf(Collection<File> files) {
			files.forEach(file -> containsSpecificValuesOf(file));
			return this;
		}

		/** Check that the values form a certain valid file are part of the settings properties. */
		public FormatterSettingsAssert containsSpecificValuesOf(File file) {
			isNotNull();

			String fileName = file.getName();
			Properties settingsProps = actual.getProperties();
			for (String expectedValue : VALID_VALUES) {
				// A parsable (valid) file contains keys of the following format
				String validValueName = (null == expectedValue) ? "null" : expectedValue;
				String key = String.format("%s.%s", fileName, validValueName);
				if (!settingsProps.containsKey(key)) {
					failWithMessage("Key <%s> not part of formatter settings.", key);
				}
				String value = settingsProps.getProperty(key);
				if ((null != expectedValue) && (!expectedValue.equals(value))) {
					failWithMessage("Value of key <%s> is '%s' and not '%s' as expected.", key, value, expectedValue);
				}
			}
			return this;
		}

		public FormatterSettingsAssert containsCommonValueOf(final File file) {
			isNotNull();

			String fileName = file.getName();
			Properties settingsProps = actual.getProperties();
			String key = "common";
			if (!settingsProps.containsKey(key)) {
				failWithMessage("Key <%s> not part of formatter settings. Value '%s' had been expected.", key, fileName);
			}
			String value = settingsProps.getProperty(key);
			if (!fileName.equals(value)) {
				failWithMessage("Value of key <%s> is '%s' and not '%s' as expected.", key, value, fileName);
			}

			return this;
		}

	};

	private static FormatterSettingsAssert assertFor(FormatterProperties actual) {
		return new FormatterSettingsAssert(actual);
	}

}
