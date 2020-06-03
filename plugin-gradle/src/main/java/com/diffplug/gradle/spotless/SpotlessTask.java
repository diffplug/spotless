/*
 * Copyright 2016-2020 DiffPlug
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.ObjectId;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Throwing;
import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.PaddedCell;

@CacheableTask
public class SpotlessTask extends DefaultTask {
	SpotlessApply applyTask;

	/** @deprecated internal use only, allows coordination between check and apply when they are in the same build */
	@Internal
	@Deprecated
	public SpotlessApply getApplyTask() {
		return applyTask;
	}

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

	ObjectId treeSha = ObjectId.zeroId();

	@Input
	public ObjectId getRatchetSha() {
		return treeSha;
	}

	@Deprecated
	@Internal
	public boolean isPaddedCell() {
		return true;
	}

	@Deprecated
	public void setPaddedCell(boolean paddedCell) {
		getLogger().warn("Spotless warning: Padded Cell is now always on, and cannot be turned off.  Find `paddedCell(` and remove all invocations.");
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

	protected FileCollection target;

	@PathSensitive(PathSensitivity.RELATIVE)
	@InputFiles
	public FileCollection getTarget() {
		return target;
	}

	public void setTarget(Iterable<File> target) {
		if (target instanceof FileCollection) {
			this.target = (FileCollection) target;
		} else {
			this.target = getProject().files(target);
		}
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
			Files.createDirectories(outputDirectory.toPath());
		}

		Throwing.Specific.Predicate<File, IOException> shouldInclude;
		if (this.filePatterns.isEmpty()) {
			shouldInclude = file -> true;
		} else {
			Preconditions.checkArgument(treeSha == ObjectId.zeroId(),
					"Cannot use 'ratchetFrom' and '-PspotlessFiles' at the same time");

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

		try (Formatter formatter = buildFormatter()) {
			inputs.outOfDate(inputDetails -> {
				File input = inputDetails.getFile();
				try {
					if (shouldInclude.test(input) && input.isFile()) {
						processInputFile(formatter, input);
					}
				} catch (IOException e) {
					throw Errors.asRuntime(e);
				}
			});
		}

		inputs.removed(removedDetails -> {
			File input = removedDetails.getFile();
			try {
				if (shouldInclude.test(input)) {
					deletePreviousResult(input);
				}
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		});
	}

	private void processInputFile(Formatter formatter, File input) throws IOException {
		File output = getOutputFile(input);
		getLogger().debug("Applying format to " + input + " and writing to " + output);
		PaddedCell.DirtyState dirtyState;
		if (treeSha != ObjectId.zeroId() && GitRatchet.isClean(getProject(), treeSha, input)) {
			dirtyState = PaddedCell.isClean();
		} else {
			dirtyState = PaddedCell.calculateDirtyState(formatter, input);
		}
		if (dirtyState.isClean()) {
			// Remove previous output if it exists
			Files.deleteIfExists(output.toPath());
		} else if (dirtyState.didNotConverge()) {
			getLogger().warn("Skipping '" + input + "' because it does not converge.  Run `spotlessDiagnose` to understand why");
		} else {
			Path parentDir = output.toPath().getParent();
			if (parentDir == null) {
				throw new IllegalStateException("Every file has a parent folder.");
			}
			Files.createDirectories(parentDir);
			dirtyState.writeCanonicalTo(output);
		}
	}

	private void deletePreviousResult(File input) throws IOException {
		File output = getOutputFile(input);
		if (output.isDirectory()) {
			Files.walk(output.toPath())
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		} else {
			Files.deleteIfExists(output.toPath());
		}
	}

	private File getOutputFile(File input) {
		String outputFileName = FormatExtension.relativize(getProject().getProjectDir(), input);
		if (outputFileName == null) {
			throw new IllegalArgumentException(StringPrinter.buildString(printer -> {
				printer.println("Spotless error! All target files must be within the project root. In project " + getProject().getPath());
				printer.println("  root dir: " + getProject().getProjectDir().getAbsolutePath());
				printer.println("    target: " + input.getAbsolutePath());
			}));
		}
		return new File(outputDirectory, outputFileName);
	}

	Formatter buildFormatter() {
		return Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy)
				.encoding(Charset.forName(encoding))
				.rootDir(getProject().getRootDir().toPath())
				.steps(steps)
				.exceptionPolicy(exceptionPolicy)
				.build();
	}
}
