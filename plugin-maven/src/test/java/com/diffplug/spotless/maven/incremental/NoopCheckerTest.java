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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;

class NoopCheckerTest extends ResourceHarness {

	private MavenProject project;
	private Path indexFile;
	private Path existingSourceFile;
	private Path nonExistingSourceFile;

	@BeforeEach
	void beforeEach() throws Exception {
		project = buildMavenProject();
		indexFile = project.getBasedir().toPath().resolve(project.getBuild().getDirectory()).resolve("spotless-index");
		existingSourceFile = project.getBasedir().toPath().resolve("existing.txt");
		Files.write(existingSourceFile, "foo".getBytes(UTF_8), CREATE_NEW);
		nonExistingSourceFile = project.getBasedir().toPath().resolve("non-existing.txt");
	}

	@Test
	void deletesExistingIndexFileWhenCreated() {
		Log log = mock(Log.class);
		try (UpToDateChecker realChecker = UpToDateChecker.forProject(project, indexFile, singletonList(dummyFormatter()), log)) {
			realChecker.setUpToDate(existingSourceFile);
		}
		assertThat(indexFile).exists();

		try (UpToDateChecker noopChecker = UpToDateChecker.noop(project, indexFile, log)) {
			assertThat(noopChecker).isNotNull();
		}
		assertThat(indexFile).doesNotExist();
		verify(log).info("Deleted the index file: " + indexFile);
	}

	@Test
	void doesNothingWhenIndexFileDoesNotExist() {
		assertThat(indexFile).doesNotExist();

		Log log = mock(Log.class);
		try (UpToDateChecker noopChecker = UpToDateChecker.noop(project, indexFile, log)) {
			assertThat(noopChecker).isNotNull();
		}
		assertThat(indexFile).doesNotExist();
		verifyNoInteractions(log);
	}

	@Test
	void neverUpToDate() {
		try (UpToDateChecker noopChecker = UpToDateChecker.noop(project, indexFile, mock(Log.class))) {
			assertThat(noopChecker.isUpToDate(existingSourceFile)).isFalse();
			assertThat(noopChecker.isUpToDate(nonExistingSourceFile)).isFalse();
		}
	}

	private MavenProject buildMavenProject() throws IOException {
		File projectDir = newFolder("project");
		File targetDir = new File(projectDir, "target");
		File pomFile = new File(projectDir, "pom.xml");

		assertThat(targetDir.mkdir()).isTrue();
		assertThat(pomFile.createNewFile()).isTrue();

		MavenProject project = new MavenProject();
		project.setFile(pomFile);
		Build build = new Build();
		build.setDirectory(targetDir.getName());
		Plugin spotlessPlugin = new Plugin();
		spotlessPlugin.setGroupId("com.diffplug.spotless");
		spotlessPlugin.setArtifactId("spotless-maven-plugin");
		build.addPlugin(spotlessPlugin);
		project.setBuild(build);
		return project;
	}

	private static Formatter dummyFormatter() {
		return Formatter.builder()
				.rootDir(Paths.get(""))
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.encoding(UTF_8)
				.steps(singletonList(mock(FormatterStep.class, withSettings().serializable())))
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.build();
	}
}
