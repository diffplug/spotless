/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.maven.npm;

/**
 * Helper class to configure a maven pom to use frontend-maven-plugin.
 * @see <a href="https://github.com/eirslett/frontend-maven-plugin">frontend-maven-plugin on github</a>
 */
public final class NpmFrontendMavenPlugin {

	public static final String GROUP_ID = "com.github.eirslett";
	public static final String ARTIFACT_ID = "frontend-maven-plugin";
	public static final String VERSION = "1.11.3"; // for now, we stick with this version, as it is the last to support maven 3.1 (see pom-test.xml.mustache)

	public static final String GOAL_INSTALL_NODE_AND_NPM = "install-node-and-npm";

	public static final String INSTALL_DIRECTORY = "target";

	private NpmFrontendMavenPlugin() {
		// prevent instantiation
	}

	public static String[] pomPluginLines(String nodeVersion, String npmVersion) {
		return new String[]{
				"<plugin>",
				String.format("  <groupId>%s</groupId>", GROUP_ID),
				String.format("  <artifactId>%s</artifactId>", ARTIFACT_ID),
				String.format("  <version>%s</version>", VERSION),
				"  <executions>",
				"    <execution>",
				"      <id>install node and npm</id>",
				"      <goals>",
				String.format("        <goal>%s</goal>", GOAL_INSTALL_NODE_AND_NPM),
				"      </goals>",
				"    </execution>",
				"  </executions>",
				"  <configuration>",
				(nodeVersion != null ? "    <nodeVersion>" + nodeVersion + "</nodeVersion>" : ""),
				(npmVersion != null ? "     <npmVersion>" + npmVersion + "</npmVersion>" : ""),
				String.format("    <installDirectory>%s</installDirectory>", INSTALL_DIRECTORY),
				"  </configuration>",
				"</plugin>"
		};
	}

	public static String installNpmMavenGoal() {
		return String.format("%s:%s:%s", GROUP_ID, ARTIFACT_ID, GOAL_INSTALL_NODE_AND_NPM);
	}

	public static String installedNpmPath() {
		return String.format("%s/node/npm%s", INSTALL_DIRECTORY, System.getProperty("os.name").toLowerCase().contains("win") ? ".cmd" : "");
	}
}
