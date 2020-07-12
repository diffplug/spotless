/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.maven.kotlin;

import org.junit.Test;

import com.diffplug.spotless.JreVersion;
import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class KtfmtTest extends MavenIntegrationHarness {
	@Test
	public void testKtfmt() throws Exception {
		if (JreVersion.thisVm() == JreVersion._8) {
			// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
			return;
		}

		writePomWithKotlinSteps("<ktfmt/>");

		String path1 = "src/main/kotlin/main1.kt";
		String path2 = "src/main/kotlin/main2.kt";

		setFile(path1).toResource("kotlin/ktfmt/basic.dirty");
		setFile(path2).toResource("kotlin/ktfmt/basic.dirty");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(path1).sameAsResource("kotlin/ktfmt/basic.clean");
		assertFile(path2).sameAsResource("kotlin/ktfmt/basic.clean");
	}
}
