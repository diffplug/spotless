/*
 * Copyright 2021-2022 DiffPlug
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
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class DiktatStepTest extends ResourceHarness {

	@Test
	void behavior() throws Exception {
		FormatterStep step = DiktatStep.create(TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResourceException("kotlin/diktat/Unsolvable.kt", assertion -> {
					assertion.isInstanceOf(AssertionError.class);
					assertion.hasMessage("There are 2 unfixed errors:" +
							System.lineSeparator() + "Error on line: 1, column: 1 cannot be fixed automatically" +
							System.lineSeparator() + "[FILE_NAME_INCORRECT] file name is incorrect - it should end with .kt extension and be in PascalCase: " +
							System.lineSeparator() + "Error on line: 1, column: 1 cannot be fixed automatically" +
							System.lineSeparator() + "[FILE_NAME_MATCH_CLASS] file name is incorrect - it should match with the class described in it if there is the only one class declared:  vs Unsolvable");
				});
	}

	@Test
	void behaviorConf() throws Exception {

		String configPath = "src/main/kotlin/diktat-analysis.yml";
		File conf = setFile(configPath).toResource("kotlin/diktat/diktat-analysis.yml");
		FileSignature config = signAsList(conf);

		FormatterStep step = DiktatStep.create("1.0.1", TestProvisioner.mavenCentral(), Collections.emptyMap(), config);
		StepHarness.forStep(step)
				.testResourceException("kotlin/diktat/Unsolvable.kt", assertion -> {
					assertion.isInstanceOf(AssertionError.class);
					assertion.hasMessage("There are 2 unfixed errors:" +
							System.lineSeparator() + "Error on line: 1, column: 1 cannot be fixed automatically" +
							System.lineSeparator() + "[FILE_NAME_INCORRECT] file name is incorrect - it should end with .kt extension and be in PascalCase: " +
							System.lineSeparator() + "Error on line: 1, column: 1 cannot be fixed automatically" +
							System.lineSeparator() + "[FILE_NAME_MATCH_CLASS] file name is incorrect - it should match with the class described in it if there is the only one class declared:  vs Unsolvable");
				});
	}

}
