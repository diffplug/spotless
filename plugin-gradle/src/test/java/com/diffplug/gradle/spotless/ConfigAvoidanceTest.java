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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ConfigAvoidanceTest extends GradleIntegrationHarness {
	@Test
	public void noConfigOnHelp() throws IOException {
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"apply plugin: 'java'",
				"spotless {",
				"    java {",
				"        googleJavaFormat('1.2')",
				"    }",
				"}",
				"",
				"class ConfigureCanary extends DefaultTask {",
				"    ConfigureCanary() {",
				"        println('Canary was configured')",
				"    }",
				"",
				"    @TaskAction",
				"    def action() {",
				"        println('Canary ran')",
				"    }",
				"}",
				"def canary = tasks.register('canary', ConfigureCanary) {}",
				"tasks.named('check').configure {",
				"   dependsOn(canary)",
				"}");
		setFile("src/main/java/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

		String help = gradleRunner().withArguments("help").build().getOutput();
		Assertions.assertThat(help).doesNotContain("Canary was configured");
		String check = gradleRunner().withArguments("check").buildAndFail().getOutput();
		Assertions.assertThat(check).contains("Canary was configured", "Canary ran", "Execution failed for task ':spotlessJavaCheck'");
	}
}
