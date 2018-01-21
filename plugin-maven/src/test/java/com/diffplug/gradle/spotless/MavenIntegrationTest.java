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
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;

import com.diffplug.spotless.ResourceHarness;

public class MavenIntegrationTest extends ResourceHarness {
	/**
	 * Each test gets its own temp folder, and we create a maven
	 * build there and run it.
	 *
	 * Because those test folders don't have a .gitattributes file,
	 * git on windows will default to \r\n. So now if you read a
	 * test file from the spotless test resources, and compare it
	 * to a build result, the line endings won't match.
	 *
	 * By sticking this .gitattributes file into the test directory,
	 * we ensure that the default Spotless line endings policy of
	 * GIT_ATTRIBUTES will use \n, so that tests match the test
	 * resources on win and linux.
	 */
	@Before
	public void gitAttributes() throws IOException {
		write(".gitattributes", "* text eol=lf");
	}

	private static final String POM_HEADER = "" +
			"<project>\n" +
			"  <modelVersion>4.0.0</modelVersion>\n" +
			"  <repositories>\n" +
			"    <repository>\n" +
			"      <id>central</id>\n" +
			"      <name>Central Repository</name>\n" +
			"      <url>http://repo.maven.apache.org/maven2</url>\n" +
			"      <layout>default</layout>\n" +
			"      <snapshots>\n" +
			"        <enabled>false</enabled>\n" +
			"      </snapshots>\n" +
			"    </repository>\n" +
			"  </repositories>\n" +
			"  <pluginRepositories>\n" + // TODO: setup test so that the plugin gets compiled first, and put into this repository
			"    <pluginRepository>\n" +
			"      <id>central</id>\n" +
			"      <name>Central Repository</name>\n" +
			"      <url>http://repo.maven.apache.org/maven2</url>\n" +
			"      <layout>default</layout>\n" +
			"      <snapshots>\n" +
			"        <enabled>false</enabled>\n" +
			"      </snapshots>\n" +
			"      <releases>\n" +
			"        <updatePolicy>never</updatePolicy>\n" +
			"      </releases>\n" +
			"    </pluginRepository>\n" +
			"  </pluginRepositories>\n" +
			"  <build>\n" +
			"    <plugins>\n" +
			"      <plugin>\n" +
			"        <groupId>com.diffplug.spotless</groupId>\n" +
			"        <artifactId>spotless-maven-plugin</artifactId>\n" +
			"        <version>+</version>\n" +
			"        <configuration>\n";

	private static final String POM_FOOTER = "" +
			"        </configuration>\n" +
			"      </plugin>\n" +
			"    </plugins>\n" +
			"  </build>\n" +
			"</project>\n";

	protected void writePomJavaSteps(String... steps) throws IOException {
		write("pom.xml",
				POM_HEADER,
				"<java><steps>",
				Arrays.stream(steps).collect(Collectors.joining("\n")),
				"</steps></java>",
				POM_FOOTER);
	}

	protected MavenRunner mavenRunner() throws IOException {
		return MavenRunner.create()
				.withProjectDir(rootFolder());
	}
}
