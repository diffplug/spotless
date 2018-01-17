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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.java.EclipseFormatterStep;

@Mojo(name = "apply")
public class SpotlessMojo extends AbstractSpotlessMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<File> filesToFormat = collectFilesToFormat();

		Formatter formatter = createFormatter(filesToFormat);

		formatAll(filesToFormat, formatter);
	}

	private List<File> collectFilesToFormat() throws MojoExecutionException {
		List<File> filesToFormat = new ArrayList<>();
		for (Path root : getAllSourceRoots()) {
			try (Stream<Path> entries = Files.walk(root)) {
				entries.filter(Files::isRegularFile)
						.filter(file -> file.getFileName().toString().endsWith(".java"))
						.map(Path::toFile)
						.forEach(filesToFormat::add);
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to walk the file tree", e);
			}
		}
		return filesToFormat;
	}

	private Formatter createFormatter(List<File> filesToFormat) {
		return Formatter.builder()
				.encoding(getEncoding())
				.lineEndingsPolicy(getLineEndingsPolicy(filesToFormat))
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.steps(singletonList(createEclipseFormatterStep()))
				.rootDir(getRootDir().toPath())
				.build();
	}

	private FormatterStep createEclipseFormatterStep() {
		ArtifactResolver artifactResolver = createArtifactResolver();
		Provisioner provisioner = MavenProvisioner.create(artifactResolver);
		Set<File> settingFiles = singleton(getJavaConfig().getEclipseConfigFile());
		return EclipseFormatterStep.create(EclipseFormatterStep.defaultVersion(), settingFiles, provisioner);
	}

	private static void formatAll(List<File> files, Formatter formatter) throws MojoExecutionException {
		for (File file : files) {
			try {
				formatter.applyTo(file);
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to format file " + file, e);
			}
		}
	}
}
