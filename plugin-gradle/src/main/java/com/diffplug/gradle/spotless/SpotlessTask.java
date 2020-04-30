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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.PaddedCellBulk;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

public class SpotlessTask extends DefaultTask {
	// set by SpotlessExtension, but possibly overridden by FormatExtension
	protected String encoding = "UTF-8";

	@Input
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = Objects.requireNonNull(encoding);
	}

	protected LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX.createPolicy();

	@Input
	public LineEnding.Policy getLineEndingsPolicy() {
		return lineEndingsPolicy;
	}

	public void setLineEndingsPolicy(LineEnding.Policy lineEndingsPolicy) {
		this.lineEndingsPolicy = Objects.requireNonNull(lineEndingsPolicy);
	}

	// set by FormatExtension
	protected boolean paddedCell = false;

	@Input
	public boolean isPaddedCell() {
		return paddedCell;
	}

	public void setPaddedCell(boolean paddedCell) {
		this.paddedCell = paddedCell;
	}

	protected String filePatterns = "";

	@Input
	public String getFilePatterns() {
		return filePatterns;
	}

	public void setFilePatterns(String filePatterns) {
		this.filePatterns = Objects.requireNonNull(filePatterns);
	}

	protected FormatExceptionPolicy exceptionPolicy = new FormatExceptionPolicyStrict();

	public void setExceptionPolicy(FormatExceptionPolicy exceptionPolicy) {
		this.exceptionPolicy = Objects.requireNonNull(exceptionPolicy);
	}

	@Input
	public FormatExceptionPolicy getExceptionPolicy() {
		return exceptionPolicy;
	}

	protected Iterable<File> target;

	@InputFiles
	public Iterable<File> getTarget() {
		return target;
	}

	public void setTarget(Iterable<File> target) {
		this.target = Objects.requireNonNull(target);
	}

	private File outputDirectory = new File(getProject().getBuildDir(), "spotless/" + getName());

	@OutputDirectory
	public File getOutputDirectory() {
		return outputDirectory;
	}

	protected List<FormatterStep> steps = new ArrayList<>();

	@Input
	public List<FormatterStep> getSteps() {
		return Collections.unmodifiableList(steps);
	}

	public void setSteps(List<FormatterStep> steps) {
		this.steps = PluginGradlePreconditions.requireElementsNonNull(steps);
	}

	public boolean addStep(FormatterStep step) {
		return this.steps.add(Objects.requireNonNull(step));
	}

	/** Returns the name of this format. */
	String formatName() {
		String name = getName();
		if (name.startsWith(SpotlessExtension.EXTENSION)) {
			return name.substring(SpotlessExtension.EXTENSION.length()).toLowerCase(Locale.ROOT);
		} else {
			return name;
		}
	}

	@TaskAction
	public void performAction(IncrementalTaskInputs inputs) throws Exception {
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> target'");
		}

		if (!inputs.isIncremental()) {
			getLogger().info("Not incremental: removing prior outputs");
			getProject().delete(outputDirectory);
		}

		Predicate<File> shouldInclude;
		if (this.filePatterns.isEmpty()) {
			shouldInclude = file -> true;
		} else {
			// a list of files has been passed in via project property
			final String[] includePatterns = this.filePatterns.split(",");
			final List<Pattern> compiledIncludePatterns = Arrays.stream(includePatterns)
					.map(Pattern::compile)
					.collect(Collectors.toList());
			shouldInclude = file -> compiledIncludePatterns
					.stream()
					.anyMatch(filePattern -> filePattern.matcher(file.getAbsolutePath())
							.matches());
		}

		inputs.removed(removedDetails -> {
			File input = removedDetails.getFile();
			if (shouldInclude.test(input)) {
				try {
					deletePreviousResult(input);
				} catch (IOException e) {
					throw Errors.asRuntime(e);
				}
			}
		});
		// accumulate the outOfDate files
		try (Formatter formatter = buildFormatter()) {
			inputs.outOfDate(inputDetails -> {
				File input = inputDetails.getFile();
				if (shouldInclude.test(input) && input.isFile()) {
					try {
						getLogger().debug("Applying format to " + input);
						byte[] canonical = PaddedCellBulk.getCanonicalOrNullIfClean(formatter, input);
						if (canonical == null) {
							deletePreviousResult(input);
						} else {
							writeResult(input, canonical);
						}
					} catch (IOException e) {
						throw Errors.asRuntime(e);
					}
				}
			});
		}
	}

	private void writeResult(File input, byte[] result) throws IOException {
		File output = getOutputFile(input);
		Files.createDirectories(output.getParentFile().toPath());
		Files.write(output.toPath(), result);
	}

	private void deletePreviousResult(File input) throws IOException {
		File output = getOutputFile(input);
		Files.deleteIfExists(output.toPath());
	}

	private Formatter buildFormatter() {
		return Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy)
				.encoding(Charset.forName(encoding))
				.rootDir(getProject().getRootDir().toPath())
				.steps(steps)
				.exceptionPolicy(exceptionPolicy)
				.build();
	}

	private File getOutputFile(File input) {
		String outputFileName = FormatExtension.relativize(getProject().getProjectDir(), input);
		if (outputFileName == null) {
			outputFileName = input.getAbsolutePath();
		}
		return new File(outputDirectory, outputFileName);
	}

	/** Returns an exception which indicates problem files nicely. */
	GradleException formatViolationsFor(List<File> problemFiles, Formatter formatter) {
		return new GradleException(DiffMessageFormatter.builder()
				.runToFix("Run 'gradlew spotlessApply' to fix these violations.")
				.isPaddedCell(paddedCell)
				.formatter(formatter)
				.problemFiles(problemFiles)
				.getMessage());
	}
}
