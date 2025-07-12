package com.diffplug.spotless;

import java.io.File;

/**
 * Implementation of {@link GitPrePushHookInstaller} specifically for Maven-based projects.
 * This class installs a Git pre-push hook that uses Maven to check and apply Spotless formatting.
 */
public class GitPrePushHookInstallerMaven extends GitPrePushHookInstaller {

	public GitPrePushHookInstallerMaven(GitPreHookLogger logger, File root) {
		super(logger, root);
	}

	/**
	 * Confirms that Maven is installed and available for use.
	 *
	 * <p>This method assumes that if this code is running, then Maven is already properly installed and configured,
	 * so it always returns {@code true}.
	 *
	 * @return {@code true}, indicating that Maven is available.
	 */
	@Override
	protected boolean isExecutorInstalled() {
		return true;
	}

	@Override
	protected String preHookContent() {
		return preHookTemplate("mvn", "spotless:check", "spotless:apply");
	}
}
