/*
 * Copyright 2021-2023 DiffPlug
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

import static com.diffplug.spotless.FileSignature.signAsList;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;

class DiktatStepTest extends ResourceHarness {

	@Test
	void behavior() {
		FormatterStep step = DiktatStep.create(TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step).testResourceExceptionMsg("kotlin/diktat/Unsolvable.kt").isEqualTo("There are 2 unfixed errors:" +
				System.lineSeparator() + "Error on line: 12, column: 9 cannot be fixed automatically" +
				System.lineSeparator() + "[DEBUG_PRINT] use a dedicated logging library: found println()" +
				System.lineSeparator() + "Error on line: 13, column: 9 cannot be fixed automatically" +
				System.lineSeparator() + "[DEBUG_PRINT] use a dedicated logging library: found println()");
	}

	@Test
	void behaviorConf() throws Exception {
		String configPath = "src/main/kotlin/diktat-analysis.yml";
		File conf = setFile(configPath).toResource("kotlin/diktat/diktat-analysis.yml");
		FileSignature config = signAsList(conf);

		FormatterStep step = DiktatStep.create("1.2.1", TestProvisioner.mavenCentral(), config);
		StepHarnessWithFile.forStep(this, step).testResourceExceptionMsg("kotlin/diktat/Unsolvable.kt").isEqualTo("There are 2 unfixed errors:" +
				System.lineSeparator() + "Error on line: 1, column: 1 cannot be fixed automatically" +
				System.lineSeparator() + "[DEBUG_PRINT] use a dedicated logging library: found println()" +
				System.lineSeparator() + "Error on line: 13, column: 9 cannot be fixed automatically" +
				System.lineSeparator() + "[DEBUG_PRINT] use a dedicated logging library: found println()");
	}

	@Test
	void notSupportedVersion() {
		final IllegalStateException notSupportedException = Assertions.assertThrows(IllegalStateException.class,
				() -> DiktatStep.create("1.1.0", TestProvisioner.mavenCentral()));
		Assertions.assertTrue(
				notSupportedException.getMessage().contains("Minimum required Diktat version is 1.2.1, you tried 1.1.0 which is too old"));

		Assertions.assertDoesNotThrow(() -> DiktatStep.create("1.2.1", TestProvisioner.mavenCentral()));
		Assertions.assertDoesNotThrow(() -> DiktatStep.create("2.0.0", TestProvisioner.mavenCentral()));
	}
}
