/*
 * Copyright 2020 DiffPlug
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.jgit.lib.ObjectId;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.Incremental;

import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

public class SpotlessTask extends DefaultTask {
	SpotlessApply applyTask;

	/** Internal use only, allows coordination between check and apply when they are in the same build */
	@Internal
	SpotlessApply getApplyTask() {
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

	protected LineEnding.Policy lineEndingsPolicy;

	@Input
	public LineEnding.Policy getLineEndingsPolicy() {
		return lineEndingsPolicy;
	}

	public void setLineEndingsPolicy(LineEnding.Policy lineEndingsPolicy) {
		this.lineEndingsPolicy = Objects.requireNonNull(lineEndingsPolicy);
	}

	/*** API which performs git up-to-date tasks. */
	@Nullable
	GitRatchetGradle ratchet;
	/** The sha of the tree at repository root, used for determining if an individual *file* is clean according to git. */
	ObjectId rootTreeSha;
	/**
	 * The sha of the tree at the root of *this project*, used to determine if the git baseline has changed within this folder.
	 * Using a more fine-grained tree (rather than the project root) allows Gradle to mark more subprojects as up-to-date
	 * compared to using the project root.
	 */
	private ObjectId subtreeSha = ObjectId.zeroId();

	public void setupRatchet(GitRatchetGradle gitRatchet, String ratchetFrom) {
		ratchet = gitRatchet;
		rootTreeSha = gitRatchet.rootTreeShaOf(getProject(), ratchetFrom);
		subtreeSha = gitRatchet.subtreeShaOf(getProject(), rootTreeSha);
	}

	@Internal
	GitRatchetGradle getRatchet() {
		return ratchet;
	}

	@Internal
	ObjectId getRootTreeSha() {
		return rootTreeSha;
	}

	@Input
	public ObjectId getRatchetSha() {
		return subtreeSha;
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
	@Incremental
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

	protected File outputDirectory = new File(getProject().getBuildDir(), "spotless/" + getName());

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
