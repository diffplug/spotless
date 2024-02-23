/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless.markdown;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class FreshMarkStepTest {
	@Test
	void behavior() {
		HashMap<String, String> map = new HashMap<>();
		map.put("lib", "MyLib");
		map.put("author", "Me");
		StepHarness.forStep(FreshMarkStep.create(map, TestProvisioner.mavenCentral()))
				.testResource("freshmark/FreshMarkUnformatted.test", "freshmark/FreshMarkFormatted.test");
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			String version = "1.3.1";
			final Map<String, Object> props = new HashMap<>();

			@Override
			protected void setupTest(API api) {
				// same version and props == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "1.3.0";
				api.areDifferentThan();
				// change the props, and it's different
				props.put("1", "2");
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				String finalVersion = this.version;
				Map<String, ?> finalProps = new HashMap<>(props);
				return FreshMarkStep.create(finalVersion, finalProps, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
