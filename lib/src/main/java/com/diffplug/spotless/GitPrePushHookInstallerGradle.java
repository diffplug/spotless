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
	 * Checks if the Gradle wrapper (`gradlew`) is present in the root directory.
	 * This ensures that the executor used for formatting (`spotlessCheck` and `spotlessApply`) is available.
	 *
	 * @return {@code true} if the Gradle wrapper is found, {@code false} otherwise.
	 *         An error is logged if the wrapper is not found.
	 */
	@Override
	protected boolean isExecutorInstalled() {
		if (gradlew.exists()) {
			return true;
		}

		logger.error("Failed to find gradlew in root directory");
		return false;
	}

	@Override
	protected String preHookContent() {
		return preHookTemplate(gradlew.getAbsolutePath(), "spotlessCheck", "spotlessApply");
	}
}
