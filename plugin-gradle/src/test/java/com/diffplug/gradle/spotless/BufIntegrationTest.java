/*
 * Copyright 2022-2024 DiffPlug
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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.tag.BufTest;

@BufTest
class BufIntegrationTest extends GradleIntegrationHarness {
	@Test
	void bufLarge() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  protobuf {",
				"    buf()",
				"  }",
				"}");
		setFile("buf.proto").toResource("protobuf/buf/buf_large.proto");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("buf.proto").sameAsResource("protobuf/buf/buf_large.proto.clean");
	}

	@Test
	void buf() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  protobuf {",
				"    buf()",
				"  }",
				"}");
		setFile("buf.proto").toResource("protobuf/buf/buf.proto");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("buf.proto").sameAsResource("protobuf/buf/buf.proto.clean");
	}

	@Test
	void bufWithLicense() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  protobuf {",
				"    buf()",
				"    licenseHeader '/* (C) 2022 */'",
				"  }",
				"}");
		setFile("license.proto").toResource("protobuf/buf/license.proto");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("license.proto").sameAsResource("protobuf/buf/license.proto.clean");
	}
}
