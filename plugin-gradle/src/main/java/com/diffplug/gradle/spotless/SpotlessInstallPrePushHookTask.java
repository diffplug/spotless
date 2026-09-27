/*
 * Copyright 2025-2026 DiffPlug
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import org.gradle.work.DisableCachingByDefault;
import org.jspecify.annotations.NonNull;

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
public abstract class SpotlessInstallPrePushHookTask extends DefaultTask {

	@Internal
	abstract Property<File> getRootDir();

	/**
	 * Determines whether this task is being executed from the root project.
	 */
	@Internal
	abstract Property<Boolean> getIsRootExecution();

	@Inject
	protected abstract ExecOperations getExecOperations();

	/**
	 * Installs the Spotless pre-push hook for a supported Git repository.
	 *
	 * <p>The task runs only for the root project. It asks Git to resolve the
	 * effective hook path, validates the destination, and delegates the actual
	 * installation to {@link GitPrePushHookInstallerGradle}. Unsupported
	 * repositories or destinations are logged and skipped.
	 *
	 * @throws Exception if resolving the hook path or installing the hook fails
	 */
	@TaskAction
	public void performAction() throws Exception {
		// if is not root project, skip it
		if (!getIsRootExecution().get()) {
			getLogger().debug("Skipping Spotless pre-push hook installation because it is not being executed from the root project.");
			return;
		}

		File rootDir = getRootDir().get();

		final boolean isWorkTree;
		try {
			isWorkTree = isGitWorkTree(rootDir);
		} catch (org.gradle.api.GradleException exception) {
			getLogger().warn("Skipping Spotless pre-push hook installation - could not run Git to check repository config {}: {}",
					rootDir, exception.getMessage());
			return;
		}

		if (!isWorkTree || !isSupportedGitRepository(rootDir)) {
			getLogger().warn("Skipping Spotless pre-push hook installation - directory {} is not a supported Git repository", rootDir);
			return;
		}

		File prePushHookFile = resolvePrePushHookFile(rootDir);
		File prePushHookRootDir = prePushHookFile.getParentFile();

		if (!isSupportedDestinationDir(rootDir, prePushHookRootDir, prePushHookFile)) {
			getLogger().warn(
					"Skipping Spotless pre-push hook installation - not supported destination dir {}.", prePushHookRootDir);
			return;
		}

		getLogger().debug("Found pre push hook root from config: {}", prePushHookRootDir);

		createInstaller(rootDir, prePushHookRootDir).install();
	}

	private boolean isSupportedGitRepository(File root) {
		return Files.isRegularFile(root.toPath().resolve(".git/config"));
	}

	/**
	 * Checks whether the resolved hook destination is safe for this installer.
	 *
	 * <p>The hooks directory must be inside the project and must either exist
	 * as a directory or not exist yet. The pre-push hook must either not exist
	 * or be a regular file; symbolic links are rejected to avoid writing
	 * through them.
	 *
	 * @throws IOException if a canonical path cannot be resolved
	 */
	private boolean isSupportedDestinationDir(File rootDir, File hooksDir, File hookFile) throws IOException {
		return hooksDir.getCanonicalFile().toPath().startsWith(rootDir.getCanonicalFile().toPath())
				&& (!hooksDir.exists() || hooksDir.isDirectory())
				&& !Files.isSymbolicLink(hookFile.toPath())
				&& (!hookFile.exists() || hookFile.isFile());
	}

	private boolean isGitWorkTree(File root) {
		var stdout = new ByteArrayOutputStream();
		var stderr = new ByteArrayOutputStream();

		var result = getExecOperations().exec(spec -> {
			spec.setWorkingDir(root);
			spec.commandLine("git", "rev-parse", "--is-inside-work-tree");
			spec.setStandardOutput(stdout);
			spec.setErrorOutput(stderr);
			spec.setIgnoreExitValue(true);
		});

		return result.getExitValue() == 0
				&& stdout.toString(StandardCharsets.UTF_8).trim().equals("true");
	}

	private File resolvePrePushHookFile(File root) {
		var output = new ByteArrayOutputStream();

		getExecOperations().exec(spec -> {
			spec.setWorkingDir(root);
			spec.commandLine("git", "rev-parse", "--git-path", "hooks/pre-push");
			spec.setStandardOutput(output);
		});

		String pathOutput = output.toString(StandardCharsets.UTF_8);
		var path = Path.of(normalizePath(pathOutput));
		return (path.isAbsolute() ? path : root.toPath().resolve(path)).normalize().toFile();
	}

	private static @NonNull String normalizePath(String pathOutput) {
		if (pathOutput.endsWith("\n")) {
			pathOutput = pathOutput.substring(0, pathOutput.length() - 1);
			if (pathOutput.endsWith("\r")) {
				pathOutput = pathOutput.substring(0, pathOutput.length() - 1);
			}
		}
		return pathOutput;
	}

	private @NonNull GitPrePushHookInstallerGradle createInstaller(File rootDir, File prePushHookRootDir) {
		final var logger = new GitPreHookLogger() {
			@Override
			public void info(String format, Object... arguments) {
				getLogger().lifecycle(format.formatted(arguments));
			}

			@Override
			public void warn(String format, Object... arguments) {
				getLogger().warn(format.formatted(arguments));
			}

			@Override
			public void error(String format, Object... arguments) {
				getLogger().error(format.formatted(arguments));
			}
		};

		return new GitPrePushHookInstallerGradle(logger, rootDir, prePushHookRootDir);
	}
}
