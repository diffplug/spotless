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
import org.junit.Ignore;
import org.junit.Test;

/**
 * This test is ignored because it doesn't work.
 * To make it work, we'll need something like the following:
 *
 * build.gradle
 *     task publishTestToLocalRepo()
 *     test.dependsOn(publishTestToLocalRepo)
 *
 * Then in MavenIntegratonTest.POM_HEADER, set <pluginRepositories> appropriately
 *
 * Would also be good if we had mvnw setup, so that
 * the test harness used mvnw to control the verison
 * of maven that was used.
 */
@Ignore
public class EclipseFormatStepTest extends MavenIntegrationTest {
	@Test
	public void testEclipse() throws IOException, InterruptedException {
		// write the pom
		writePomJavaSteps(
				"<eclipse>",
				"  <file>${basedir}/eclipse-fmt.xml</file>",
				"  <version>4.7.1</version>",
				"</eclipse>");
		write("src/main/java/test.java", getTestResource("java/eclipse/format/JavaCodeUnformatted.test"));
		mavenRunner().withArguments("spotless:apply").runNoError();

		String actual = read("src/main/java/test.java");
		Assertions.assertThat(actual).isEqualTo(getTestResource("java/eclipse/format/JavaCodeFormatted.test"));
	}
}
