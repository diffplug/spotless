/*
 * Copyright 2024-2025 DiffPlug
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
package com.diffplug.spotless.maven.cpp;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.tag.ClangTest;

@ClangTest
class ClangMavenIntegrationTest extends MavenIntegrationHarness {

	@Test
	@ClangTest
	void csharp() throws Exception {
		writePomWithCppSteps("<includes>", "<include>", "src/**/*.cs", "</include>", "</includes>",
				"<clangFormat>", "<version>", "14.0.0-1ubuntu1.1", "</version>", "</clangFormat>");
		setFile("src/test.cs").toResource("clang/example.cs");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("src/test.cs").sameAsResource("clang/example.cs.clean");
	}

	@Test
	@ClangTest
	void proto() throws Exception {
		writePomWithCppSteps("<includes>", "<include>", "**/*.proto", "</include>", "</includes>",
				"<clangFormat>", "<version>", "14.0.0-1ubuntu1.1", "</version>", "</clangFormat>");
		setFile("buf.proto").toResource("protobuf/buf/buf.proto");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("buf.proto").sameAsResource("protobuf/buf/buf.proto.clean");
	}

}
