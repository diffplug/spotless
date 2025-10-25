/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless.maven.java;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ForbidModuleImportsStepTest extends MavenIntegrationHarness {

	@Test
	void testForbidModuleImports() throws Exception {
		writePomWithJavaSteps("<forbidModuleImports/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/forbidmoduleimports/JavaCodeModuleImportsUnformatted.test");
		var selfie = expectSelfieErrorMsg(mavenRunner().withArguments("spotless:apply").runHasError());
		assertFile(path).sameAsResource("java/forbidmoduleimports/JavaCodeModuleImportsUnformatted.test");
		selfie.toBe("""
				Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:VERSION:apply (default-cli) on project spotless-maven-plugin-tests: There were 2 lint error(s), they must be fixed or suppressed.
				src/main/java/test.java:L1 forbidModuleImports(import module java.base;) Do not use module imports - replace with specific class imports as 'spotlessApply' cannot auto-fix this
				src/main/java/test.java:L2 forbidModuleImports(import module java.sql;) Do not use module imports - replace with specific class imports as 'spotlessApply' cannot auto-fix this
				Resolve these lints or suppress with `<lintSuppressions>`
				""");
	}
}
