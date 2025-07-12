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
package com.diffplug.gradle.spotless;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import com.diffplug.spotless.GitPrePushHookInstaller.GitPreHookLogger;
import com.diffplug.spotless.GitPrePushHookInstallerGradle;

/**
 * A Gradle task responsible for installing a Git pre-push hook for the Spotless plugin.
 * This hook ensures that Spotless formatting rules are automatically checked and applied
 * before performing a Git push operation.
 *
 * <p>The task leverages {@link GitPrePushHookInstallerGradle} to implement the installation process.
 */
@DisableCachingByDefault(because = "not worth caching")
public class SpotlessInstallPrePushHookTask extends DefaultTask {

	/**
	 * Executes the task to install the Git pre-push hook.
	 *
	 * <p>This method creates an instance of {@link GitPrePushHookInstallerGradle},
	 * providing a logger to record informational and error messages during the installation process.
	 * The installer then installs the hook in the root directory of the Gradle project.
	 *
	 * @throws Exception if an error occurs during the hook installation process.
	 */
	@TaskAction
	public void performAction() throws Exception {
		final var logger = new GitPreHookLogger() {
			@Override
			public void info(String format, Object... arguments) {
				getLogger().lifecycle(String.format(format, arguments));
			}

			@Override
			public void error(String format, Object... arguments) {
				getLogger().error(String.format(format, arguments));
			}
		};

		final var installer = new GitPrePushHookInstallerGradle(logger, getProject().getRootDir());
		installer.install();
	}
}
