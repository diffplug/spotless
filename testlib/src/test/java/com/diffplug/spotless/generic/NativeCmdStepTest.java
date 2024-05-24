/*
 * Copyright 2021-2024 DiffPlug
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
package com.diffplug.spotless.generic;

import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;

public class NativeCmdStepTest extends ResourceHarness {

	@Test
	public void testWithSed() {
		File sed = new File("/usr/bin/sed");
		assumeThat(sed).exists();
		FormatterStep step = NativeCmdStep.create("format-native", sed, List.of("s/placeholder/replaced/g"));
		StepHarnessWithFile.forStep(this, step)
				.testResource("native_cmd/dirty.txt", "native_cmd/clean.txt");
	}
}
