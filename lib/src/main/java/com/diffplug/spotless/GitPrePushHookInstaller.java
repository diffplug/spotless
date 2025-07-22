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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Abstract class responsible for installing a Git pre-push hook in a repository.
 * This class ensures that specific checks and logic are run before a push operation in Git.
 *
 * Subclasses should define specific behavior for hook installation by implementing the required abstract methods.
 */
public abstract class GitPrePushHookInstaller {

	private static final String HOOK_HEADER = "##### SPOTLESS HOOK START #####";
	private static final String HOOK_FOOTER = "##### SPOTLESS HOOK END #####";

	private static final Object LOCK = new Object();

	private static volatile boolean installing = false;

	/**
	 * Logger for recording informational and error messages during the installation process.
	 */
	protected final GitPreHookLogger logger;

	/**
	 * The root directory of the Git repository where the hook will be installed.
	 */
	protected final File root;

	/**
	 * Constructor to initialize the GitPrePushHookInstaller with a logger and repository root path.
	 *
	 * @param logger The logger for recording messages.
	 * @param root   The root directory of the Git repository.
	 */
	public GitPrePushHookInstaller(GitPreHookLogger logger, File root) {
		this.logger = requireNonNull(logger, "logger can not be null");
		this.root = requireNonNull(root, "root file can not be null");
	}

	/**
	 * Installs the Git pre-push hook while ensuring thread safety and preventing parallel installations.
	 *
	 * The method:
	 * 1. Uses a thread-safe mechanism to prevent concurrent installations
	 * 2. If a parallel installation is detected, logs a warning and skips the installation
	 * 3. Uses a synchronized block with a static lock object to ensure thread safety
	 *
	 * The installation process sets a flag during installation and resets it afterwards,
	 * using the {@link #doInstall()} method to perform the actual installation.
	 *
	 * @throws Exception if any error occurs during installation
	 */
	@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "This is a safe usage of a static lock object")
	public void install() throws Exception {
		if (installing) {
			logger.warn("Parallel Spotless Git pre-push hook installation detected, skipping installation");
			return;
		}

		synchronized (LOCK) {
			if (installing) {
				logger.warn("Parallel Spotless Git pre-push hook installation detected, skipping installation");
				return;
			}

			try {
				installing = true;
				doInstall();
			} finally {
				installing = false;
			}
		}
	}

	/**
	 * Installs the Git pre-push hook into the repository.
	 *
	 * <p>This method checks for the following:
	 * <ul>
	 *   <li>Ensures Git is installed and the `.git/config` file exists.</li>
	 *   <li>Checks if an executor required by the hook is available.</li>
	 *   <li>Creates and writes the pre-push hook file if it does not exist.</li>
	 *   <li>Skips installation if the hook is already installed.</li>
	 * </ul>
	 * If an issue occurs during installation, error messages are logged.
	 *
	 * @throws Exception if any error occurs during installation.
	 */
	private void doInstall() throws Exception {
		logger.info("Installing git pre-push hook");

		if (!isGitInstalled()) {
			logger.error("Git not found in root directory");
			return;
		}

		var hookContent = "";
		final var gitHookFile = root.toPath().resolve(".git/hooks/pre-push").toFile();
		if (!gitHookFile.exists()) {
			logger.info("Git pre-push hook not found, creating it");
			if (!gitHookFile.getParentFile().exists() && !gitHookFile.getParentFile().mkdirs()) {
				logger.error("Failed to create pre-push hook directory");
				return;
			}

			if (!gitHookFile.createNewFile()) {
				logger.error("Failed to create pre-push hook file");
				return;
			}

			if (!gitHookFile.setExecutable(true, false)) {
				logger.error("Can not make file executable");
				return;
			}

			hookContent += "#!/bin/sh\n";
		}

		if (isGitHookInstalled(gitHookFile)) {
			logger.info("Git pre-push hook already installed, reinstalling it");
			uninstall(gitHookFile);
		}

		hookContent += preHookContent();
		writeFile(gitHookFile, hookContent, true);

		logger.info("Git pre-push hook installed successfully to the file %s", gitHookFile.getAbsolutePath());
	}

	/**
	 * Uninstalls the Spotless Git pre-push hook from the specified hook file by removing
	 * the custom hook content between the defined hook markers.
	 *
	 * <p>This method:
	 * <ul>
	 *   <li>Reads the entire content of the pre-push hook file</li>
	 *   <li>Identifies the Spotless hook section using predefined markers</li>
	 *   <li>Removes the Spotless hook content while preserving other hook content</li>
	 *   <li>Writes the modified content back to the hook file</li>
	 * </ul>
	 *
	 * @param gitHookFile The Git pre-push hook file from which to remove the Spotless hook
	 * @throws Exception if any error occurs during the uninstallation process,
	 *                  such as file reading or writing errors
	 */
	private void uninstall(File gitHookFile) throws Exception {
		final var hook = Files.readString(gitHookFile.toPath(), UTF_8);
		final int hookStart = hook.indexOf(HOOK_HEADER);
		final int hookEnd = hook.indexOf(HOOK_FOOTER) + HOOK_FOOTER.length(); // hookEnd exclusive, so must be last symbol \n

		/* Detailed explanation:
		 * 1. hook.indexOf(HOOK_FOOTER) - finds the starting position of footer "##### SPOTLESS HOOK END #####"
		 * 2. + HOOK_FOOTER.length() is needed because String.substring(startIndex, endIndex) treats endIndex as exclusive
		 *
		 * For example, if file content is:
		 * #!/bin/sh
		 * ##### SPOTLESS HOOK START #####
		 * ... hook code ...
		 * ##### SPOTLESS HOOK END #####
		 * other content
		 *
		 * When we later use this in: hook.substring(hookStart, hookEnd)
		 * - Since substring's endIndex is exclusive (it stops BEFORE that index)
		 * - We need hookEnd to point to the position AFTER the last '#'
		 * - This ensures the entire footer "##### SPOTLESS HOOK END #####" is included in the substring
		 *
		 * This exclusive behavior is why in the subsequent code:
		 * if (hook.charAt(hookEnd) == '\n') {
		 *     hookScript += "\n";
		 * }
		 *
		 * We can directly use hookEnd to check the next character after the footer
		 * - Since hookEnd is already pointing to the position AFTER the footer
		 * - No need for hookEnd + 1 in charAt()
		 * - This makes the code more consistent with the substring's exclusive nature
		 */

		var hookScript = hook.substring(hookStart, hookEnd);
		if (hookStart >= 1 && hook.charAt(hookStart - 1) == '\n') {
			hookScript = "\n" + hookScript;
		}

		if (hookStart >= 2 && hook.charAt(hookStart - 2) == '\n') {
			hookScript = "\n" + hookScript;
		}

		if (hook.charAt(hookEnd) == '\n') {
			hookScript += "\n";
		}

		final var uninstalledHook = hook.replace(hookScript, "");

		writeFile(gitHookFile, uninstalledHook, false);
	}

	/**
	 * Provides the content of the hook that should be inserted into the pre-push script.
	 *
	 * @return A string representing the content to include in the pre-push script.
	 */
	protected abstract String preHookContent();

	/**
	 * Generates a pre-push template script that defines the commands to check and apply changes
	 * using an executor and Spotless.
	 *
	 * @param executor      The tool to execute the check and apply commands.
	 * @param commandCheck  The command to check for issues.
	 * @param commandApply  The command to apply corrections.
	 * @return A string template representing the Spotless Git pre-push hook content.
	 */
	protected String preHookTemplate(Executor executor, String commandCheck, String commandApply) {
		var spotlessHook = "";

		spotlessHook += "\n";
		spotlessHook += "\n" + HOOK_HEADER;
		spotlessHook += "\nSPOTLESS_EXECUTOR=" + executorPath(executor);
		spotlessHook += "\nif ! $SPOTLESS_EXECUTOR " + commandCheck + " ; then";
		spotlessHook += "\n    echo 1>&2 \"spotless found problems, running " + commandApply + "; commit the result and re-push\"";
		spotlessHook += "\n    $SPOTLESS_EXECUTOR " + commandApply;
		spotlessHook += "\n    exit 1";
		spotlessHook += "\nfi";
		spotlessHook += "\n" + HOOK_FOOTER;
		spotlessHook += "\n";

		return spotlessHook;
	}

	/**
	 * Determines the path to the build tool executor (Maven or Gradle).
	 * This method first checks for the existence of a wrapper script in the project root.
	 * If the wrapper exists, returns a relative path to it, otherwise returns the global command.
	 *
	 * @param executor The build tool executor (GRADLE or MAVEN)
	 * @return The path to the executor - either the wrapper script path (e.g., "./gradlew")
	 *         or the global command (e.g., "gradle")
	 */
	private String executorPath(Executor executor) {
		final var wrapper = executorWrapperFile(executor);
		if (wrapper.exists()) {
			return "./" + wrapper.getName();
		}

		logger.info("Local %s wrapper (%s) not found, falling back to global command '%s'",
				executor.name().toLowerCase(Locale.ROOT), executor.wrapper, executor.global);

		return executor.global;
	}

	/**
	 * Resolves the wrapper script file for the specified build tool executor.
	 * On Windows systems, checks for both .bat and .cmd extensions.
	 * On non-Windows systems, uses the wrapper name without extension.
	 *
	 * @param executor The build tool executor (GRADLE or MAVEN)
	 * @return The File object representing the wrapper script
	 */
	private File executorWrapperFile(Executor executor) {
		if (isWindows()) {
			final var bat = root.toPath().resolve(executor.wrapper + ".bat").toFile();
			if (bat.exists()) {
				return bat;
			}

			return root.toPath().resolve(executor.wrapper + ".cmd").toFile();
		}

		return root.toPath().resolve(executor.wrapper).toFile();
	}

	/**
	 * Checks if the current operating system is Windows.
	 * This is determined by checking if the "os.name" system property contains "win".
	 *
	 * @return true if the current OS is Windows, false otherwise
	 */
	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win");
	}

	/**
	 * Checks if Git is installed by validating the existence of `.git/config` in the repository root.
	 *
	 * @return {@code true} if Git is installed, {@code false} otherwise.
	 */
	private boolean isGitInstalled() {
		return root.toPath().resolve(".git/config").toFile().exists();
	}

	/**
	 * Verifies if the pre-push hook file already contains the custom Spotless hook content.
	 *
	 * @param gitHookFile The file representing the Git hook.
	 * @return {@code true} if the hook is already installed, {@code false} otherwise.
	 * @throws Exception if an error occurs when reading the file.
	 */
	private boolean isGitHookInstalled(File gitHookFile) throws Exception {
		final var hook = Files.readString(gitHookFile.toPath(), UTF_8);
		return hook.contains(HOOK_HEADER) && hook.contains(HOOK_FOOTER);
	}

	/**
	 * Writes the specified content into a file.
	 *
	 * @param file    The file to which the content should be written.
	 * @param content The content to write into the file.
	 * @throws IOException if an error occurs while writing to the file.
	 */
	private void writeFile(File file, String content, boolean append) throws IOException {
		try (final var writer = new FileWriter(file, UTF_8, append)) {
			writer.write(content);
		}
	}

	public enum Executor {
		GRADLE("gradlew", "gradle"), MAVEN("mvnw", "mvn"),;

		public final String wrapper;
		public final String global;

		Executor(String wrapper, String global) {
			this.wrapper = wrapper;
			this.global = global;
		}
	}

	public interface GitPreHookLogger {
		void info(String format, Object... arguments);

		void warn(String format, Object... arguments);

		void error(String format, Object... arguments);
	}
}
