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
package com.diffplug.spotless.scala;

import java.io.IOException;

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class ScalaFmtStepTest extends ResourceHarness {
	@Test
	public void behavior() throws Exception {
		FormatterStep defaultConfStep = ScalaFmtStep.create(TestProvisioner.mavenCentral());
		StepHarness.forStep(defaultConfStep)
				.testResource("scala/scalafmt/basic.dirty", "scala/scalafmt/basic.clean");
	}

	@Test
	public void equality() throws Exception {
		new StepEqualityTester() {
			String version = "0.5.1";

			@Override
			protected void setupTest(API api) throws IOException {
				// same version == same
				api.assertThisEqualToThis();
				api.areDifferentThan();
				// change the version, and it's different
				version = "0.5.0";
				api.assertThisEqualToThis();
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return ScalaFmtStep.create(version, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
