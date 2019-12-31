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

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import com.diffplug.spotless.ResourceHarness;

public class DefaultRepoConfig extends ResourceHarness {
	@Test
	public void integration() throws IOException {
		setFile("build.gradle").toLines(
				"repositories {",
				"    mavenCentral()",
				"    jcenter()",
				"    gradlePluginPortal()",
				"}",
				"",
				"println('buildscriptSize=' + buildscript.repositories.size())",
				"for (repo in buildscript.repositories) {",
				"  println(repo.toString())",
				"  println(repo.class)",
				"}",
				"println('projectSize=' + repositories.size())",
				"for (repo in repositories) {",
				"  println(repo.name)",
				"  println(repo.url)",
				"  println(repo.artifactUrls)",
				"}");

		String output = GradleRunner.create()
				.withGradleVersion("6.0")
				.withProjectDir(rootFolder())
				.build().getOutput();
		Assertions.assertThat(output.replace("\r\n", "\n")).startsWith("\n" +
				"> Configure project :\n" +
				"buildscriptSize=0\n" +
				"projectSize=3\n" +
				"MavenRepo\n" +
				"https://repo.maven.apache.org/maven2/\n" +
				"[]\n" +
				"BintrayJCenter\n" +
				"https://jcenter.bintray.com/\n" +
				"[]\n" +
				"Gradle Central Plugin Repository\n" +
				"https://plugins.gradle.org/m2\n" +
				"[]\n" +
				"");
	}
}
