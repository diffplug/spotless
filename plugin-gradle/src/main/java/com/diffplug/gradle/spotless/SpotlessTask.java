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

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.Iterables;
import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.PaddedCellBulk;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

public class SpotlessTask extends DefaultTask {
	private static final String FILES_PROPERTY = "spotlessFiles";

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

	protected FormatExceptionPolicy exceptionPolicy = new FormatExceptionPolicyStrict();

	public void setExceptionPolicy(FormatExceptionPolicy exceptionPolicy) {
		this.exceptionPolicy = Objects.requireNonNull(exceptionPolicy);
	}

	@Input
	public FormatExceptionPolicy getExceptionPolicy() {
		return exceptionPolicy;
	}

	protected Iterable<File> target;

	public Iterable<File> getTarget() {
		return target;
	}

	public void setTarget(Iterable<File> target) {
		this.target = requireElementsNonNull(target);
	}

	/** Internal use only. */
	@InputFiles
	@Deprecated
	public Iterable<File> getInternalTarget() {
		// used to combine the special cache file and the real target
		return Iterables.concat(ImmutableList.of(getCacheFile()), target);
	}

	private File getCacheFile() {
		return new File(getProject().getBuildDir(), getName());
	}

	protected List<FormatterStep> steps = new ArrayList<>();

	@Input
	public List<FormatterStep> getSteps() {
		return Collections.unmodifiableList(steps);
	}

	public void setSteps(List<FormatterStep> steps) {
		this.steps = requireElementsNonNull(steps);
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
		if (name.startsWith(SpotlessPlugin.EXTENSION)) {
			return name.substring(SpotlessPlugin.EXTENSION.length()).toLowerCase(Locale.ROOT);
		} else {
			return name;
		}
	}

	@TaskAction
	public void performAction(IncrementalTaskInputs inputs) throws Exception {
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> toFormat'");
		}
		if (!check && !apply) {
			throw new GradleException("Don't call " + getName() + " directly, call " + getName() + SpotlessPlugin.CHECK + " or " + getName() + SpotlessPlugin.APPLY);
		}

		// create the formatter
		try (Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy)
				.encoding(Charset.forName(encoding))
				.rootDir(getProject().getRootDir().toPath())
				.steps(steps)
				.exceptionPolicy(exceptionPolicy)
				.build()) {
			// determine if a list of files has been passed in
			Predicate<File> shouldInclude;
			Project project = getProject();
			if (project.hasProperty(FILES_PROPERTY) && project.property(FILES_PROPERTY) instanceof String) {
				Object rawIncludePatterns = project.property(FILES_PROPERTY);
				assert rawIncludePatterns != null;
				final String[] includePatterns = ((String) rawIncludePatterns).split(",");
				shouldInclude = file -> Arrays.stream(includePatterns)
						.anyMatch(filePattern -> file.getAbsolutePath().matches(filePattern));
			} else {
				shouldInclude = file -> true;
			}
			// find the outOfDate files
			List<File> outOfDate = new ArrayList<>();
			inputs.outOfDate(inputDetails -> {
				File file = inputDetails.getFile();
				if (file.isFile() && !file.equals(getCacheFile()) && shouldInclude.test(file)) {
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
					if (!outOfDate.contains(file) && file.exists() && Iterables.contains(target, file) && shouldInclude.test(file)) {
						outOfDate.add(file);
					}
				}
			}

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

	static class LastApply implements Serializable {
		private static final long serialVersionUID = 6245070824310295090L;

		long timestamp;
		List<File> changedFiles;
	}

	private List<File> applyAnyChanged(Formatter formatter, List<File> outOfDate) throws Exception {
		List<File> changed = new ArrayList<>(outOfDate.size());
		if (isPaddedCell()) {
			for (File file : outOfDate) {
				getLogger().debug("Applying format to " + file);
				if (PaddedCellBulk.applyAnyChanged(formatter, file)) {
					changed.add(file);
				}
			}
		} else {
			boolean anyMisbehave = false;
			for (File file : outOfDate) {
				getLogger().debug("Applying format to " + file);
				String unixResultIfDirty = formatter.applyToAndReturnResultIfDirty(file);
				if (unixResultIfDirty != null) {
					changed.add(file);
				}
				// because apply will count as up-to-date, it's important
				// that every call to apply will get a PaddedCell check
				if (!anyMisbehave && unixResultIfDirty != null) {
					String onceMore = formatter.compute(unixResultIfDirty, file);
					//  f(f(input) == f(input) for an idempotent function
					if (!onceMore.equals(unixResultIfDirty)) {
						// it's not idempotent.  but, if it converges, then it's likely a glitch that won't reoccur,
						// so there's no need to make a bunch of noise for the user
						PaddedCell result = PaddedCell.check(formatter, file, onceMore);
						if (result.type() == PaddedCell.Type.CONVERGE) {
							String finalResult = formatter.computeLineEndings(result.canonical(), file);
							Files.write(file.toPath(), finalResult.getBytes(formatter.getEncoding()), StandardOpenOption.TRUNCATE_EXISTING);
						} else {
							// it didn't converge, so the user is going to need padded cell mode
							anyMisbehave = true;
						}
					}
				}
			}
			if (anyMisbehave) {
				throw PaddedCellGradle.youShouldTurnOnPaddedCell(this);
			}
		}
		return changed;
	}

	private void check(Formatter formatter, List<File> outOfDate) throws Exception {
		List<File> problemFiles = new ArrayList<>();
		for (File file : outOfDate) {
			getLogger().debug("Checking format on " + file);
			if (!formatter.isClean(file)) {
				problemFiles.add(file);
			}
		}
		if (paddedCell) {
			PaddedCellGradle.check(this, formatter, problemFiles);
		} else {
			if (!problemFiles.isEmpty()) {
				// if we're not in paddedCell mode, we'll check if maybe we should be
				if (PaddedCellBulk.anyMisbehave(formatter, problemFiles)) {
					throw PaddedCellGradle.youShouldTurnOnPaddedCell(this);
				} else {
					throw formatViolationsFor(formatter, problemFiles);
				}
			}
		}
	}

	/** Returns an exception which indicates problem files nicely. */
	GradleException formatViolationsFor(Formatter formatter, List<File> problemFiles) {
		return new GradleException(DiffMessageFormatter.builder()
				.runToFix("Run 'gradlew spotlessApply' to fix these violations.")
				.isPaddedCell(paddedCell)
				.formatter(formatter)
				.problemFiles(problemFiles)
				.getMessage());
	}
}
