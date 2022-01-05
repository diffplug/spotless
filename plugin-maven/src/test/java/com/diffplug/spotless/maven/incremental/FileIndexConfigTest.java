/*
 * Copyright 2021-2022 DiffPlug
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
package com.diffplug.spotless.maven.incremental;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

class FileIndexConfigTest {

	@Test
	void returnsCorrectProjectDir() {
		MavenProject project = mavenProject();
		FileIndexConfig config = new FileIndexConfig(project, getIndexFile(project), PluginFingerprint.from("foo"));

		assertThat(config.getProjectDir()).isEqualTo(Paths.get("projectDir"));
	}

	@Test
	void returnsCorrectIndexFile() {
		MavenProject project = mavenProject();
		FileIndexConfig config = new FileIndexConfig(project, getIndexFile(project), PluginFingerprint.from("foo"));

		assertThat(config.getIndexFile())
				.isEqualTo(Paths.get("projectDir", "target", "spotless-index"));
	}

	@Test
	void returnsCorrectPluginFingerprint() {
		MavenProject project = mavenProject();
		FileIndexConfig config = new FileIndexConfig(project, getIndexFile(project), PluginFingerprint.from("foo"));

		assertThat(config.getPluginFingerprint()).isEqualTo(PluginFingerprint.from("foo"));
	}

	@Test
	void returnsEmptyPluginFingerprint() {
		MavenProject project = mavenProject();
		FileIndexConfig config = new FileIndexConfig(project, getIndexFile(project));

		assertThat(config.getPluginFingerprint()).isEqualTo(PluginFingerprint.from(""));
	}

	private static MavenProject mavenProject() {
		MavenProject project = new MavenProject();
		project.setFile(new File("projectDir", "pom.xml"));
		Build build = new Build();
		build.setDirectory("target");
		project.setBuild(build);
		return project;
	}

	private static Path getIndexFile(MavenProject project) {
		return project.getBasedir().toPath().resolve(project.getBuild().getDirectory()).resolve("spotless-index");
	}
}
