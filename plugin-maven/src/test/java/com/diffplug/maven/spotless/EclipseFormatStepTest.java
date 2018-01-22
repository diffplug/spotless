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
package com.diffplug.maven.spotless;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Would also be good if we had mvnw setup, so that
 * the test harness used mvnw to control the verison
 * of maven that was used.
 */
public class EclipseFormatStepTest extends MavenIntegrationTest {

	@Test
	public void testEclipse() throws Exception {
		// write the pom
		writePomWithJavaSteps(
				"<eclipse>",
				"  <file>${basedir}/formatter.xml</file>",
				"  <version>4.7.1</version>",
				"</eclipse>");

		write("src/main/java/test.java", getTestResource("java/eclipse/format/JavaCodeUnformatted.test"));
		write("formatter.xml", getTestResource("java/eclipse/format/formatter.xml"));
		mavenRunner().withArguments("spotless:apply").runNoError();

		String actual = read("src/main/java/test.java");
		Assertions.assertThat(actual).isEqualTo(getTestResource("java/eclipse/format/JavaCodeFormatted.test"));
	}
}
