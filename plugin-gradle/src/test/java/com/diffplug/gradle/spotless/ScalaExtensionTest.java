/*
 * Copyright 2016 DiffPlug
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

import org.junit.Assert;
import org.junit.Test;

public class ScalaExtensionTest extends GradleIntegrationTest {
	@Test
	public void integration() throws IOException {
		write("build.gradle",
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"apply plugin: 'scala'",
				"spotless {",
				"    scala {",
				"        scalafmt().configFile('scalafmt.conf')",
				"    }",
				"}");
		write("src/main/scala/basic.scala", getTestResource("scala/scalafmt/basic.dirty"));
		write("scalafmt.conf", getTestResource("scala/scalafmt/scalafmt.conf"));
		gradleRunner().withArguments("spotlessApply").build();
		String result = read("src/main/scala/basic.scala");
		String formatted = getTestResource("scala/scalafmt/basic.clean");
		Assert.assertEquals(formatted, result);
	}
}
