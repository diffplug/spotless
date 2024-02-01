/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.shell;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.tag.ShfmtTest;

@ShfmtTest
public class ShfmtStepTest extends ResourceHarness {
	@Test
	void testWithEditorconfig() throws Exception {
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, ShfmtStep.withVersion(ShfmtStep.defaultVersion()).create())) {
			final String fileDir = "shell/shfmt/singlefile/with-config/";
			final String dirtyFile = fileDir + "shfmt.sh";
			final String cleanFile = fileDir + "shfmt.clean";

			setFile(".editorconfig").toResource(fileDir + ".editorconfig");

			harness.testResource(dirtyFile, cleanFile);
		}
	}

	@Test
	void testWithoutEditorconfig() throws Exception {
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, ShfmtStep.withVersion(ShfmtStep.defaultVersion()).create())) {
			final String fileDir = "shell/shfmt/singlefile/without-config/";
			final String dirtyFile = fileDir + "shfmt.sh";
			final String cleanFile = fileDir + "shfmt.clean";

			harness.testResource(dirtyFile, cleanFile);
		}
	}
}
