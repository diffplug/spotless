/*
 * Copyright 2020-2025 DiffPlug
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.jgit.lib.ObjectId;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.work.Incremental;

import com.diffplug.spotless.ConfigurationCacheHackList;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.LintSuppression;
import com.diffplug.spotless.extra.GitRatchet;

@DisableCachingByDefault(because = "abstract definition")
public abstract class SpotlessTask extends DefaultTask {
	@Internal
	abstract Property<SpotlessTaskService> getTaskService();

	// set by SpotlessExtension, but possibly overridden by FormatExtension
	protected String encoding = "UTF-8";

	@Input
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = Objects.requireNonNull(encoding);
	}

	protected Provider<LineEnding.Policy> lineEndingsPolicy = null;

	@Input
	public Provider<LineEnding.Policy> getLineEndingsPolicy() {
		return lineEndingsPolicy;
	}

	public void setLineEndingsPolicy(Provider<LineEnding.Policy> lineEndingsPolicy) {
		this.lineEndingsPolicy = lineEndingsPolicy;
	}

	/**
	 * The sha of the tree at repository root, used for determining if an individual
	 * *file* is clean according to git.
	 */
	private transient ObjectId rootTreeSha;
	/**
	 * The sha of the tree at the root of *this project*, used to determine if the
	 * git baseline has changed within this folder.
	 * Using a more fine-grained tree (rather than the project root) allows Gradle
	 * to mark more subprojects as up-to-date
	 * compared to using the project root.
	 */
	private transient ObjectId subtreeSha = ObjectId.zeroId();
	/**
	 * Stored so that the configuration cache can recreate the GitRatchetGradle
	 * state.
	 */
	protected String ratchetFrom;

	public void setupRatchet(String ratchetFrom) {
		this.ratchetFrom = ratchetFrom;
		if (!ratchetFrom.isEmpty()) {
			GitRatchet ratchet = getTaskService().get().getRatchet();
			File projectDir = getProjectDir().get().getAsFile();
			rootTreeSha = ratchet.rootTreeShaOf(projectDir, ratchetFrom);
			subtreeSha = ratchet.subtreeShaOf(projectDir, rootTreeSha);
		} else {
			subtreeSha = ObjectId.zeroId();
		}
	}

	@Internal
	abstract DirectoryProperty getProjectDir();

	@Internal
	GitRatchetGradle getRatchet() {
		return ObjectId.zeroId().equals(getRatchetSha()) ? null : getTaskService().get().getRatchet();
	}

	@Internal
	ObjectId getRootTreeSha() {
		return rootTreeSha;
	}

	@Input
	public ObjectId getRatchetSha() {
		if (subtreeSha == null) {
			setupRatchet(ratchetFrom);
		}
		return subtreeSha;
	}

	protected List<LintSuppression> lintSuppressions = new ArrayList<>();

	public void setLintSuppressions(List<LintSuppression> lintSuppressions) {
		this.lintSuppressions = Objects.requireNonNull(lintSuppressions);
	}

	@Input
	public List<LintSuppression> getLintSuppressions() {
		return lintSuppressions;
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

	protected File cleanDirectory = new File(getProject().getLayout().getBuildDirectory().getAsFile().get(),
			"spotless-clean/" + getName());

	@OutputDirectory
	public File getCleanDirectory() {
		return cleanDirectory;
	}

	protected File lintsDirectory = new File(getProject().getLayout().getBuildDirectory().getAsFile().get(),
			"spotless-lints/" + getName());

	@OutputDirectory
	public File getLintsDirectory() {
		return lintsDirectory;
	}

	private final ConfigurationCacheHackList stepsInternalRoundtrip = ConfigurationCacheHackList.forRoundtrip();
	private final ConfigurationCacheHackList stepsInternalEquality = ConfigurationCacheHackList.forEquality();

	@Internal
	public ConfigurationCacheHackList getStepsInternalRoundtrip() {
		return stepsInternalRoundtrip;
	}

	@Input
	public ConfigurationCacheHackList getStepsInternalEquality() {
		return stepsInternalEquality;
	}

	public void setSteps(List<FormatterStep> steps) {
		PluginGradlePreconditions.requireElementsNonNull(steps);
		this.stepsInternalRoundtrip.clear();
		this.stepsInternalEquality.clear();
		this.stepsInternalRoundtrip.addAll(steps);
		this.stepsInternalEquality.addAll(steps);
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
				.lineEndingsPolicy(getLineEndingsPolicy().get())
				.encoding(Charset.forName(encoding))
				.steps(stepsInternalRoundtrip.getSteps())
				.build();
	}
}
