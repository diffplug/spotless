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
package com.diffplug.spotless.cli;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;

import com.diffplug.spotless.ResourceHarness;

public abstract class CLIIntegrationHarness extends ResourceHarness {

	/**
	 * Each test gets its own temp folder, and we create a gradle
	 * build there and run it.
	 * <p>
	 * Because those test folders don't have a .gitattributes file,
	 * git (on windows) will default to \r\n. So now if you read a
	 * test file from the spotless test resources, and compare it
	 * to a build result, the line endings won't match.
	 * <p>
	 * By sticking this .gitattributes file into the test directory,
	 * we ensure that the default Spotless line endings policy of
	 * GIT_ATTRIBUTES will use \n, so that tests match the test
	 * resources on win and linux.
	 */
	@BeforeEach
	void gitAttributes() throws IOException {
		setFile(".gitattributes").toContent("* text eol=lf");
	}

	protected SpotlessCLIRunner cliRunner() {
		return SpotlessCLIRunner.create()
				.withWorkingDir(rootFolder());
	}
}
