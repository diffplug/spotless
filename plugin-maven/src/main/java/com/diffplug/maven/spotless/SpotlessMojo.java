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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

@Mojo(name = "apply")
public class SpotlessMojo extends AbstractSpotlessMojo {

	private static final String FILE_EXTENSION_SEPARATOR = ".";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<FormatterFactory> formatterFactories = singletonList(getJava());

		for (FormatterFactory formatterFactory : formatterFactories) {
			execute(formatterFactory);
		}
	}

	private void execute(FormatterFactory formatterFactory) throws MojoExecutionException {
		MojoConfig mojoConfig = getMojoConfig();
		List<File> filesToFormat = collectFilesToFormat(formatterFactory.fileExtension());

		Formatter formatter = formatterFactory.newFormatter(filesToFormat, mojoConfig);

		boolean check = false;
		if (!check) {
			formatAll(filesToFormat, formatter);
		} else {
			checkAll(filesToFormat, formatter);
		}
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

	private static void checkAll(List<File> files, Formatter formatter) throws MojoExecutionException {
		List<File> problemFiles = new ArrayList<>();
		for (File file : files) {
			try {
				if (!formatter.isClean(file)) {
					problemFiles.add(file);
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to format file " + file, e);
			}
		}
		if (!problemFiles.isEmpty()) {
			throw new MojoExecutionException(DiffMessageFormatter.builder()
					.runToFix("Run 'gradlew spotless:apply' to fix these violations.")
					.rootDir(null)
					.isPaddedCell(false)
					.formatter(formatter)
					.problemFiles(problemFiles)
					.getMessage());
		}
	}

	private List<File> collectFilesToFormat(String extension) throws MojoExecutionException {
		try {
			return getAllSourceRoots().stream()
					.flatMap(root -> collectFilesToFormat(root, extension).stream())
					.map(Path::toFile)
					.collect(toList());
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to collect files to format", e);
		}
	}

	private static List<Path> collectFilesToFormat(Path root, String extension) {
		try (Stream<Path> entries = Files.walk(root)) {
			return entries.filter(Files::isRegularFile)
					.filter(file -> hasExtension(file, extension))
					.collect(toList());
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to walk the file tree rooted at " + root, e);
		}
	}

	private static boolean hasExtension(Path file, String extension) {
		Path fileName = file.getFileName();
		if (fileName == null) {
			return false;
		} else {
			String fileNameString = fileName.toString();
			return fileNameString.endsWith(FILE_EXTENSION_SEPARATOR + extension);
		}
	}
}
