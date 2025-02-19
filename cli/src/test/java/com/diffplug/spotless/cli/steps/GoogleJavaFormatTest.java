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
package com.diffplug.spotless.cli.steps;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.cli.CLIIntegrationHarness;
import com.diffplug.spotless.cli.SpotlessCLIRunner;

public class GoogleJavaFormatTest extends CLIIntegrationHarness {

	@Test
	void formattingWithGoogleJavaFormatWorks() throws IOException {
		setFile("Java.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

		SpotlessCLIRunner.Result result = cliRunner().withTargets("*.java").withStep(GoogleJavaFormat.class).run();

		System.out.println(result.stdOut());
		System.out.println("-------");
		System.out.println(result.stdErr());
		assertFile("Java.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
	}
}
