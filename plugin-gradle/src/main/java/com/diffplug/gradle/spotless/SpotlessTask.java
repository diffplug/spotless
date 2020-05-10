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
import java.io.Serializable;
import java.nio.charset.Charset;
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
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.Iterables;
import com.diffplug.spotless.FileSignatureRelocatable;
import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.PaddedCell;
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

	@Internal
	public LineEnding.Policy getLineEndingsPolicy() {
		return lineEndingsPolicy;
	}

	@Input
	public Object getLineEndingsPolicyRelocatable() throws IOException {
		// allows gradle buildcache to relocate between machines
		return new FileSignatureRelocatable(getProject().getRootProject().getRootDir(), getLineEndingsPolicy());
	}

	public void setLineEndingsPolicy(LineEnding.Policy lineEndingsPolicy) {
		this.lineEndingsPolicy = Objects.requireNonNull(lineEndingsPolicy);
	}

	@Deprecated
	@Internal
	public boolean isPaddedCell() {
		return true;
	}

	@Deprecated
	public void setPaddedCell(boolean paddedCell) {
		getLogger().warn("PaddedCell is now always on, and cannot be turned off.");
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

	@Internal
	public Iterable<File> getTarget() {
		return target;
	}

	public void setTarget(Iterable<File> target) {
		this.target = Objects.requireNonNull(target);
	}

	/** Internal use only. */
	@InputFiles
	@Deprecated
	public Iterable<File> getInternalTargetInput() {
		return getInternalTarget();
	}

	/** Internal use only. */
	@OutputFiles
	@Deprecated
	public Iterable<File> getInternalTargetOutput() {
		return getInternalTarget();
	}

	private Iterable<File> getInternalTarget() {
		// used to combine the special cache file and the real target
		return Iterables.concat(ImmutableList.of(getCacheFile()), target);
	}

	private File getCacheFile() {
		return new File(getProject().getBuildDir(), getName());
	}

	protected List<FormatterStep> steps = new ArrayList<>();

	@Internal
	public List<FormatterStep> getSteps() {
		return Collections.unmodifiableList(steps);
	}

	@Input
	public Object getStepsRelocatable() throws IOException {
		// allows gradle buildcache to relocate between machines
		return new FileSignatureRelocatable(getProject().getRootProject().getRootDir(), getSteps());
	}

	public void setSteps(List<FormatterStep> steps) {
		this.steps = PluginGradlePreconditions.requireElementsNonNull(steps);
	}

	public boolean addStep(FormatterStep step) {
		return this.steps.add(Objects.requireNonNull(step));
	}

	private boolean check = false;
	private boolean apply = false;

	public void setCheck() {
		this.check = true;
	}

	public void setApply() {
		this.apply = true;
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
		if (!check && !apply) {
			throw new GradleException("Don't call " + getName() + " directly, call " + getName() + SpotlessExtension.CHECK + " or " + getName() + SpotlessExtension.APPLY);
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
		// find the outOfDate files
		List<File> outOfDate = new ArrayList<>();
		inputs.outOfDate(inputDetails -> {
			File file = inputDetails.getFile();
			if (shouldInclude.test(file) && file.isFile() && !file.equals(getCacheFile())) {
				outOfDate.add(file);
			}
		});
		// load the files that were changed by the last run
		// because it's possible the user changed them back to their
		// unformatted form, so we need to treat them as dirty
		// (see bug #144)
		if (getCacheFile().exists()) {
			LastApply lastApply = SerializableMisc.fromFile(LastApply.class, getCacheFile());
			for (File file : lastApply.changedFiles) {
				if (shouldInclude.test(file) && !outOfDate.contains(file) && file.exists() && Iterables.contains(target, file)) {
					outOfDate.add(file);
				}
			}
		}

		if (outOfDate.isEmpty()) {
			// no work to do
			return;
		}

		// create the formatter
		try (Formatter formatter = buildFormatter()) {
			if (apply) {
				List<File> changedFiles = applyAnyChanged(formatter, outOfDate);
				if (!changedFiles.isEmpty()) {
					// If any file changed, we need to mark the task as dirty
					// next time to avoid bug #144.
					LastApply lastApply = new LastApply();
					lastApply.timestamp = System.currentTimeMillis();
					lastApply.changedFiles = changedFiles;

					SerializableMisc.toFile(lastApply, getCacheFile());
				}
			}
			if (check) {
				check(formatter, outOfDate);
			}
		}
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

	static class LastApply implements Serializable {
		private static final long serialVersionUID = 6245070824310295090L;

		long timestamp;
		List<File> changedFiles;
	}

	private List<File> applyAnyChanged(Formatter formatter, List<File> outOfDate) throws Exception {
		List<File> changed = new ArrayList<>(outOfDate.size());
		for (File file : outOfDate) {
			getLogger().debug("Applying format to " + file);
			PaddedCell.DirtyState dirtyState = PaddedCell.calculateDirtyState(formatter, file);
			if (dirtyState.isClean()) {
				// do nothing
			} else if (dirtyState.didNotConverge()) {
				getLogger().warn("Skipping '" + file + "' because it does not converge.  Run `spotlessDiagnose` to understand why");
			} else {
				dirtyState.writeCanonicalTo(file);
				changed.add(file);
			}
		}
		return changed;
	}

	private void check(Formatter formatter, List<File> outOfDate) throws Exception {
		List<File> problemFiles = new ArrayList<>();
		for (File file : outOfDate) {
			getLogger().debug("Checking format on " + file);
			PaddedCell.DirtyState dirtyState = PaddedCell.calculateDirtyState(formatter, file);
			if (dirtyState.isClean()) {
				// do nothing
			} else if (dirtyState.didNotConverge()) {
				getLogger().warn("Skipping '" + file + "' because it does not converge.  Run `spotlessDiagnose` to understand why");
			} else {
				problemFiles.add(file);
			}
		}
		if (!problemFiles.isEmpty()) {
			throw formatViolationsFor(formatter, problemFiles);
		}
	}

	/** Returns an exception which indicates problem files nicely. */
	GradleException formatViolationsFor(Formatter formatter, List<File> problemFiles) {
		return new GradleException(DiffMessageFormatter.builder()
				.runToFix("Run 'gradlew spotlessApply' to fix these violations.")
				.formatter(formatter)
				.problemFiles(problemFiles)
				.getMessage());
	}
}
