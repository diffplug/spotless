/*
 * Copyright 2016-2025 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class ConfigAvoidanceTest extends GradleIntegrationHarness {
	@Test
	void noConfigOnHelp() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"apply plugin: 'java'",
				"spotless {",
				"    java {",
				"        googleJavaFormat()",
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
		assertThat(help).doesNotContain("Canary was configured");
		String check = gradleRunner().withArguments("check").buildAndFail().getOutput();
		assertThat(check).contains("Canary was configured", "Canary ran", "Execution failed for task ':spotlessJavaCheck'");
	}
}
