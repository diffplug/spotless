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
package com.diffplug.spotless.maven.protobuf;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.tag.BufTest;

@BufTest
class BufMavenIntegrationTest extends MavenIntegrationHarness {
	@Test
	void buf() throws Exception {
		writePomWithProtobufSteps("<buf>", "</buf>");
		setFile("buf.proto").toResource("protobuf/buf/buf.proto");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("buf.proto").sameAsResource("protobuf/buf/buf.proto.clean");
	}

	@Test
	void bufLarge() throws Exception {
		writePomWithProtobufSteps("<buf>", "</buf>");
		setFile("buf.proto").toResource("protobuf/buf/buf_large.proto");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("buf.proto").sameAsResource("protobuf/buf/buf_large.proto.clean");
	}

	@Test
	void bufWithLicense() throws Exception {
		writePomWithProtobufSteps(
				"<buf>",
				"</buf>",
				"<licenseHeader>",
				" <content>/* (C) 2022 */</content>",
				"</licenseHeader>");
		setFile("buf.proto").toResource("protobuf/buf/license.proto");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("buf.proto").sameAsResource("protobuf/buf/license.proto.clean");
	}
}
