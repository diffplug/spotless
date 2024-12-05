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
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
public class PrettierTest extends CLIIntegrationHarness {

	// TODO

	@Test
	void itRunsPrettierForTsFilesWithOptions() throws IOException {
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");

		SpotlessCLIRunner.Result result = cliRunner()
				.withTargets("test.ts")
				.withStep(Prettier.class)
				.withOption("--prettier-config-option", "printWidth=20")
				.withOption("--prettier-config-option", "parser=typescript")
				.run();

		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
	}

	@Test
	void itRunsPrettierForTsFilesWithOptionFile() throws Exception {
		setFile(".prettierrc.yml").toResource("npm/prettier/config/.prettierrc.yml");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");

		SpotlessCLIRunner.Result result = cliRunner()
				.withTargets("test.ts")
				.withStep(Prettier.class)
				.withOption("--prettier-config-path", ".prettierrc.yml")
				.run();

		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
	}

}
