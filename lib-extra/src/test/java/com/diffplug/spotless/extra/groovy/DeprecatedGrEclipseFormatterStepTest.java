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
package com.diffplug.spotless.extra.groovy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

@Deprecated
public class DeprecatedGrEclipseFormatterStepTest extends ResourceHarness {

	private static final String RESOURCE_PATH = "groovy/greclipse/format/";
	private static final String CONFIG_FILE = RESOURCE_PATH + "greclipse.properties";
	private static final String DEPRECATED_VERSION = "4.6.3";

	//String is hard-coded in the GrEclipseFormatter
	private static final String FORMATTER_FILENAME_REPALCEMENT = "Hello.groovy";

	private static Provisioner provisioner() {
		return TestProvisioner.mavenCentral();
	}

	@Test
	public void nominal() throws Throwable {
		List<File> config = createTestFiles(CONFIG_FILE);
		StepHarness.forStep(GrEclipseFormatterStep.create(DEPRECATED_VERSION, config, provisioner()))
				.testResource(RESOURCE_PATH + "unformatted.test", RESOURCE_PATH + "formatted.test");
	}

	@Test
	public void formatterException() throws Throwable {
		List<File> config = createTestFiles(CONFIG_FILE);
		StepHarness.forStep(GrEclipseFormatterStep.create(DEPRECATED_VERSION, config, provisioner()))
				.testException(RESOURCE_PATH + "exception.test", assertion -> {
					assertion.isInstanceOf(IllegalArgumentException.class);
					assertion.hasMessageContaining(FORMATTER_FILENAME_REPALCEMENT);
				});
	}

	@Test
	public void configurationException() throws Throwable {
		String configFileName = "greclipse.exception";
		List<File> config = createTestFiles(RESOURCE_PATH + configFileName);
		StepHarness.forStep(GrEclipseFormatterStep.create(DEPRECATED_VERSION, config, provisioner()))
				.testException(RESOURCE_PATH + "unformatted.test", assertion -> {
					assertion.isInstanceOf(IllegalArgumentException.class);
					assertion.hasMessageContaining(configFileName);
				});
	}

	@Test
	public void equality() throws IOException {
		List<File> configFile = createTestFiles(CONFIG_FILE);
		new SerializableEqualityTester() {

			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return GrEclipseFormatterStep.create(DEPRECATED_VERSION, configFile, provisioner());
			}
		}.testEquals();
	}

}
