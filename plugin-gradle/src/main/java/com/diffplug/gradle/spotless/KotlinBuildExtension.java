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
import java.util.Objects;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.internal.file.UnionFileCollection;

import com.diffplug.spotless.kotlin.KtLintStep;

public class KotlinBuildExtension extends FormatExtension {
	private static final String GRADLE_KOTLIN_DSL_FILE_EXTENSION = ".gradle.kts";

	static final String NAME = "kotlinBuild";

	/**
	 * Attempt to find companion build files that are not just the {@link ProjectDescriptor#getBuildFileName()}.
	 * If this is enabled then {@link #includeSubprojects} will be treated as if it is <code>true</code> if your project
	 * has additional <code>.gradle.kts</code> files under the current project's directory.
	 */
	private boolean findCompanionScripts = true;

	/**
	 * Search subprojects for their build files.
	 */
	private boolean includeSubprojects = true;

	public KotlinBuildExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	public void licenseHeader(String licenseHeader) {
		licenseHeader(licenseHeader, JavaExtension.LICENSE_HEADER_DELIMITER);
	}

	public void licenseHeaderFile(Object licenseHeaderFile) {
		licenseHeaderFile(licenseHeaderFile, JavaExtension.LICENSE_HEADER_DELIMITER);
	}

	/**
	 * @see #includeSubprojects
	 */
	public boolean isIncludeSubprojects() {
		return includeSubprojects;
	}

	/**
	 * @see #includeSubprojects
	 */
	public void setIncludeSubprojects(final boolean includeSubprojects) {
		this.includeSubprojects = includeSubprojects;
	}

	/**
	 * @see #findCompanionScripts
	 */
	public boolean isFindCompanionScripts() {
		return findCompanionScripts;
	}

	/**
	 * @see #findCompanionScripts
	 */
	public void setFindCompanionScripts(final boolean findCompanionScripts) {
		this.findCompanionScripts = findCompanionScripts;
	}

	/** Adds the specified version of [ktlint](https://github.com/shyiko/ktlint). */
	public void ktlint(String version) {
		Objects.requireNonNull(version);
		addStep(KtLintStep.create(version, GradleProvisioner.fromProject(getProject())));
	}

	public void ktlint() {
		ktlint(KtLintStep.defaultVersion());
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = findFilesForProject(getProject());
		}
		super.setupTask(task);
	}

	/**
	 * Collects the build files under this project using the various config flags above.
	 * @param project The project to search for build files.
	 */
	private FileCollection findFilesForProject(final Project project) {
		final UnionFileCollection union = new UnionFileCollection();
		if (findCompanionScripts) {
			union.add(findAllCompanionBuildScripts(project.getProjectDir()));
		} else {
			final File projectBuildFileName = project.getBuildscript().getSourceFile();
			if (projectBuildFileName.getName().endsWith(GRADLE_KOTLIN_DSL_FILE_EXTENSION)) {
				union.add(project.files(projectBuildFileName));
			}
		}
		if (includeSubprojects) {
			project.getSubprojects().stream().map(this::findFilesForProject).forEach(union::add);
		}
		return union;
	}

	/**
	 * @param directory The directory to begin the search for <code>.gradle.kts</code> files.
	 * @return All Gradle Kotlin DSL files under the given directory.
	 */
	private FileCollection findAllCompanionBuildScripts(final File directory) {
		final UnionFileCollection union = new UnionFileCollection();
		union.add(getProject().files((Object[]) directory.listFiles(file -> file.getName().endsWith(GRADLE_KOTLIN_DSL_FILE_EXTENSION))));
		Stream.of(directory.listFiles(File::isDirectory))
				.map(this::findAllCompanionBuildScripts)
				.forEach(union::add);
		return union;
	}
}
