/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless;

import java.io.File;

/**
 * Implementation of {@link GitPrePushHookInstaller} specifically for Gradle-based projects.
 * This class installs a Git pre-push hook that uses Gradle's `gradlew` executable to check and apply Spotless formatting.
 */
public class GitPrePushHookInstallerGradle extends GitPrePushHookInstaller {

	/**
	 * The Gradle wrapper file (`gradlew`) located in the root directory of the project.
	 */
	private final File gradlew;

	public GitPrePushHookInstallerGradle(GitPreHookLogger logger, File root) {
		super(logger, root);
		this.gradlew = root.toPath().resolve("gradlew").toFile();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String preHookContent() {
		return preHookTemplate(executor(), "spotlessCheck", "spotlessApply");
	}

	private String executor() {
		if (gradlew.exists()) {
			return gradlew.getAbsolutePath();
		}

		logger.info("Gradle wrapper is not installed, using global gradle");
		return "gradle";
	}
}
