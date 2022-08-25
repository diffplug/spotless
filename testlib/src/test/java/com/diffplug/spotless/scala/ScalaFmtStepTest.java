/*
 * Copyright 2016-2022 DiffPlug
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
package com.diffplug.spotless.scala;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class ScalaFmtStepTest extends ResourceHarness {
	@Test
	void behaviorDefaultConfig() throws Exception {
		StepHarness.forStep(ScalaFmtStep.create("3.0.0", TestProvisioner.mavenCentral(), null))
				.testResource("scala/scalafmt/basic.dirty", "scala/scalafmt/basic.clean_3.0.0");
	}

	@Test
	void behaviorCustomConfig() throws Exception {
		StepHarness.forStep(ScalaFmtStep.create("3.0.0", TestProvisioner.mavenCentral(), createTestFile("scala/scalafmt/scalafmt.conf")))
				.testResource("scala/scalafmt/basic.dirty", "scala/scalafmt/basic.cleanWithCustomConf_3.0.0");
	}

	@Test
	void behaviorDefaultConfigVersion_3_0_0() throws Exception {
		FormatterStep step = ScalaFmtStep.create("3.0.0", TestProvisioner.mavenCentral(), null);
		StepHarness.forStep(step)
				.testResource("scala/scalafmt/basic.dirty", "scala/scalafmt/basicPost3.0.0.clean");
	}

	@Test
	void behaviorCustomConfigVersion_3_0_0() throws Exception {
		FormatterStep step = ScalaFmtStep.create("3.0.0", TestProvisioner.mavenCentral(), createTestFile("scala/scalafmt/scalafmt.conf"));
		StepHarness.forStep(step)
				.testResource("scala/scalafmt/basic.dirty", "scala/scalafmt/basicPost3.0.0.cleanWithCustomConf");
	}

	@Test
	void equality() throws Exception {
		new SerializableEqualityTester() {
			String version = "3.5.9";
			File configFile = null;

			@Override
			protected void setupTest(API api) throws IOException {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "3.5.8";
				api.areDifferentThan();
				// add a config file, and its different
				configFile = createTestFile("scala/scalafmt/scalafmt.conf");
				api.areDifferentThan();
				// change the config file and its different
				configFile = createTestFile("scala/scalafmt/scalafmt2.conf");
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return ScalaFmtStep.create(version, TestProvisioner.mavenCentral(), configFile);
			}
		}.testEquals();
	}

	@Test
	void invalidConfiguration() throws Exception {
		File invalidConfFile = createTestFile("scala/scalafmt/scalafmt.invalid.conf");
		Provisioner provisioner = TestProvisioner.mavenCentral();

		InvocationTargetException exception = assertThrows(InvocationTargetException.class,
				() -> StepHarness.forStep(ScalaFmtStep.create("3.0.0", provisioner, invalidConfFile)).test("", ""));
		assertThat(exception.getCause().getMessage()).contains("found option 'invalidScalaFmtConfigField' which wasn't expected");
	}
}
