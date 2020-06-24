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
package com.diffplug.spotless.kotlin;

import org.junit.Ignore;
import org.junit.Test;

import com.diffplug.spotless.*;

@Ignore
public class KtfmtStepTest extends ResourceHarness {
	@Test
	public void behavior() throws Exception {
		if (JreVersion.thisVm() == JreVersion._8) {
			// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
			return;
		}
		FormatterStep step = KtfmtStep.create(TestProvisioner.mavenCentral());
		StepHarness.forStep(step).testResource("kotlin/ktfmt/basic.dirty", "kotlin/ktfmt/basic.clean");
	}

	@Test
	public void equality() throws Exception {
		new SerializableEqualityTester() {
			String version = "0.13";

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "0.12";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				String finalVersion = this.version;
				return KtfmtStep.create(finalVersion, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
