/*
 * Copyright 2024-2025 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.ThrowingEx;

public final class IdeaStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdeaStep.class);

	public static final String NAME = "IDEA";

	public static final String IDEA_EXECUTABLE_DEFAULT = "idea";

	public static final String IDEA_CONFIG_PATH_PROPERTY = "idea.config.path";
	public static final String IDEA_SYSTEM_PATH_PROPERTY = "idea.system.path";

	/** Default batch size for formatting multiple files in a single IDEA invocation */
	public static final int DEFAULT_BATCH_SIZE = 100;

	@Nonnull
	private final IdeaStepBuilder builder;

	private IdeaStep(@Nonnull IdeaStepBuilder builder) {
		this.builder = builder;
	}

	public static IdeaStepBuilder newBuilder(@Nonnull File buildDir) {
		return new IdeaStepBuilder(Objects.requireNonNull(buildDir));
	}

	private static FormatterStep create(IdeaStepBuilder builder) {
		return new IdeaStep(builder).createFormatterStep();
	}

	private FormatterStep createFormatterStep() {
		return FormatterStep.createLazy(NAME, this::createState, State::toFunc);
	}

	private State createState() {
		return new State(Objects.requireNonNull(builder));
	}

	public static final class IdeaStepBuilder {

		private boolean useDefaults = true;
		@Nonnull
		private String binaryPath = IDEA_EXECUTABLE_DEFAULT;
		@Nullable private String codeStyleSettingsPath;
		private final Map<String, String> ideaProperties = new HashMap<>();
		private int batchSize = DEFAULT_BATCH_SIZE;

		@Nonnull
		private final File buildDir;

		private IdeaStepBuilder(@Nonnull File buildDir) {
			this.buildDir = Objects.requireNonNull(buildDir);
		}

		public IdeaStepBuilder setUseDefaults(boolean useDefaults) {
			this.useDefaults = useDefaults;
			return this;
		}

		public IdeaStepBuilder setBinaryPath(@Nonnull String binaryPath) {
			this.binaryPath = Objects.requireNonNull(binaryPath);
			return this;
		}

		public IdeaStepBuilder setCodeStyleSettingsPath(@Nullable String codeStyleSettingsPath) {
			this.codeStyleSettingsPath = codeStyleSettingsPath;
			return this;
		}

		public IdeaStepBuilder setIdeaProperties(@Nonnull Map<String, String> ideaProperties) {
			if (ideaProperties.containsKey(IDEA_CONFIG_PATH_PROPERTY) || ideaProperties.containsKey(IDEA_SYSTEM_PATH_PROPERTY)) {
				throw new IllegalArgumentException("Cannot override IDEA config or system path");
			}
			this.ideaProperties.putAll(ideaProperties);
			return this;
		}

		/**
		 * Sets the batch size for formatting multiple files in a single IDEA invocation.
		 * Default is {@link #DEFAULT_BATCH_SIZE}.
		 *
		 * @param batchSize the maximum number of files to format in a single batch (must be >= 1)
		 * @return this builder
		 */
		public IdeaStepBuilder setBatchSize(int batchSize) {
			if (batchSize < 1) {
				throw new IllegalArgumentException("Batch size must be at least 1, got: " + batchSize);
			}
			this.batchSize = batchSize;
			return this;
		}

		public FormatterStep build() {
			return create(this);
		}

		@Override
		public String toString() {
			return "IdeaStepBuilder[useDefaults=%s, binaryPath=%s, codeStyleSettingsPath=%s, ideaProperties=%s, buildDir=%s, batchSize=%d]".formatted(
					this.useDefaults,
					this.binaryPath,
					this.codeStyleSettingsPath,
					this.ideaProperties,
					this.buildDir,
					this.batchSize);
		}
	}

	private static final class State implements Serializable {

		private static final long serialVersionUID = -1426311255869303398L;

		private final File uniqueBuildFolder;
		private final String binaryPath;
		@Nullable private final String codeStyleSettingsPath;
		private final boolean withDefaults;
		private final TreeMap<String, String> ideaProperties;
		private final int batchSize;

		private State(@Nonnull IdeaStepBuilder builder) {
			LOGGER.debug("Creating {} state with configuration {}", NAME, builder);
			this.uniqueBuildFolder = new File(builder.buildDir, UUID.randomUUID().toString());
			this.withDefaults = builder.useDefaults;
			this.codeStyleSettingsPath = builder.codeStyleSettingsPath;
			this.ideaProperties = new TreeMap<>(builder.ideaProperties);
			this.binaryPath = resolveFullBinaryPathAndCheckVersion(builder.binaryPath);
			this.batchSize = builder.batchSize;
		}

		private static String resolveFullBinaryPathAndCheckVersion(String binaryPath) {
			var exe = ForeignExe
					.nameAndVersion(binaryPath, "IntelliJ IDEA")
					.pathToExe(pathToExe(binaryPath))
					.versionRegex(Pattern.compile("(IntelliJ IDEA) .*"))
					.fixCantFind(
							"IDEA executable cannot be found on your machine, "
									+ "please install it and put idea binary to PATH, provide a valid path to the executable or report the problem")
					.fixWrongVersion("Provided binary is not IDEA, "
							+ "please check it and fix the problem; or report the problem");
			try {
				return exe.confirmVersionAndGetAbsolutePath();
			} catch (IOException e) {
				throw new IllegalArgumentException("binary cannot be found", e);
			} catch (InterruptedException e) {
				throw new IllegalArgumentException(
						"binary cannot be found, process was interrupted", e);
			}
		}

		@CheckForNull
		private static String pathToExe(String binaryPath) {
			String testEnvBinaryPath = TestEnvVars.read().get("%s.%s".formatted(IdeaStep.class.getName(), "binaryPath"));
			if (testEnvBinaryPath != null) {
				return testEnvBinaryPath;
			}
			if (isMacOs()) {
				return macOsFix(binaryPath);
			}
			if (new File(binaryPath).exists()) {
				return binaryPath;
			}
			return null; // search in PATH
		}

		private static String macOsFix(String binaryPath) {
			// on macOS, the binary is located in the .app bundle which might be invisible to the user
			// we try need to append the path to the binary
			File binary = new File(binaryPath);
			if (!binary.exists()) {
				// maybe it is bundle path without .app? (might be hidden by os)
				binary = new File(binaryPath + ".app");
				if (!binary.exists()) {
					return binaryPath; // fallback: do nothing
				}
			}
			if (binaryPath.endsWith(".app") || binary.isDirectory()) {
				binary = new File(binary, "Contents/MacOS/idea");
			}
			if (binary.isFile() && binary.canExecute()) {
				return binary.getPath();
			}
			return binaryPath; // fallback: do nothing
		}

		private static boolean isMacOs() {
			return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
		}

		/**
		 * Represents a file to be formatted, tracking both the temporary file and the original file reference.
		 */
		private static class FileToFormat {
			final File tempFile;
			final File originalFile;

			FileToFormat(File tempFile, File originalFile) {
				this.tempFile = tempFile;
				this.originalFile = originalFile;
			}
		}

		private String format(IdeaStepFormatterCleanupResources ideaStepFormatterCleanupResources, String unix, File file) throws Exception {
			// Delegate to the batch formatter in the cleanup resources
			return ideaStepFormatterCleanupResources.formatFile(this, unix, file);
		}

		/**
		 * Formats multiple files in a single IDEA invocation.
		 *
		 * @param ideaStepFormatterCleanupResources the cleanup resources containing the process runner
		 * @param filesToFormat the list of files to format
		 * @throws Exception if formatting fails
		 */
		private void formatBatch(IdeaStepFormatterCleanupResources ideaStepFormatterCleanupResources, List<FileToFormat> filesToFormat) throws Exception {
			if (filesToFormat.isEmpty()) {
				return;
			}

			LOGGER.info("Formatting batch of {} files with IDEA", filesToFormat.size());

			List<String> params = getParamsForBatch(filesToFormat);
			Map<String, String> env = createEnv();

			LOGGER.debug("Launching IDEA formatter with params: {} and env: {}", params, env);
			var result = ideaStepFormatterCleanupResources.runner.exec(null, env, null, params);
			LOGGER.debug("Batch command finished with exit code: {}", result.exitCode());
			LOGGER.debug("Batch command finished with stdout: {}", result.assertExitZero(StandardCharsets.UTF_8));

			// Read back the formatted content for each file
			for (FileToFormat fileToFormat : filesToFormat) {
				String formatted = Files.readString(fileToFormat.tempFile.toPath());
				ideaStepFormatterCleanupResources.cacheFormattedResult(fileToFormat.originalFile, formatted);
			}
		}

		private Map<String, String> createEnv() {
			File ideaProps = createIdeaPropertiesFile();
			return Map.ofEntries(
					Map.entry("IDEA_PROPERTIES", ThrowingEx.get(ideaProps::getCanonicalPath)));
		}

		private File createIdeaPropertiesFile() {
			Path ideaProps = this.uniqueBuildFolder.toPath().resolve("idea.properties");

			if (Files.exists(ideaProps)) {
				return ideaProps.toFile(); // only create if it does not exist
			}

			Path parent = ideaProps.getParent();
			if (parent == null) {
				throw new IllegalStateException("Parent directory for IDEA properties file %s cannot be null".formatted(ideaProps));
			}
			ThrowingEx.run(() -> Files.createDirectories(parent));

			Path configPath = parent.resolve("config");
			Path systemPath = parent.resolve("system");

			Properties properties = new Properties();
			properties.putAll(ideaProperties);
			properties.put(IDEA_CONFIG_PATH_PROPERTY, ThrowingEx.get(configPath.toFile()::getCanonicalPath));
			properties.put(IDEA_SYSTEM_PATH_PROPERTY, ThrowingEx.get(systemPath.toFile()::getCanonicalPath));

			LOGGER.debug("Creating IDEA properties file at {} with content: {}", ideaProps, properties);
			try (FileOutputStream out = new FileOutputStream(ideaProps.toFile())) {
				properties.store(out, "Generated by spotless");
			} catch (IOException e) {
				throw new IllegalStateException("Failed to create IDEA properties file", e);
			}
			return ideaProps.toFile();
		}

		/**
		 * Builds command-line parameters for formatting multiple files in a single invocation.
		 */
		private List<String> getParamsForBatch(List<FileToFormat> filesToFormat) {
			/* https://www.jetbrains.com/help/idea/command-line-formatter.html */
			var builder = Stream.<String> builder();
			builder.add(binaryPath);
			builder.add("format");
			if (withDefaults) {
				builder.add("-allowDefaults");
			}
			if (codeStyleSettingsPath != null) {
				builder.add("-s");
				builder.add(codeStyleSettingsPath);
			}
			builder.add("-charset");
			builder.add("UTF-8");

			// Add all file paths
			for (FileToFormat fileToFormat : filesToFormat) {
				builder.add(ThrowingEx.get(fileToFormat.tempFile::getCanonicalPath));
			}
			return builder.build().toList();
		}

		private FormatterFunc.Closeable toFunc() {
			IdeaStepFormatterCleanupResources ideaStepFormatterCleanupResources = new IdeaStepFormatterCleanupResources(uniqueBuildFolder, new ProcessRunner(), batchSize);
			return FormatterFunc.Closeable.of(ideaStepFormatterCleanupResources, this::format);
		}
	}

	private static class IdeaStepFormatterCleanupResources implements AutoCloseable {
		@Nonnull
		private final File uniqueBuildFolder;
		@Nonnull
		private final ProcessRunner runner;
		private final int batchSize;

		// Batch processing state (transient - not serialized)
		private final Map<File, String> formattedCache = new LinkedHashMap<>();
		private final List<State.FileToFormat> pendingBatch = new ArrayList<>();
		private final Lock batchLock = new ReentrantLock();
		private State currentState;

		public IdeaStepFormatterCleanupResources(@Nonnull File uniqueBuildFolder, @Nonnull ProcessRunner runner, int batchSize) {
			this.uniqueBuildFolder = uniqueBuildFolder;
			this.runner = runner;
			this.batchSize = batchSize;
		}

		/**
		 * Formats a single file, using batch processing for efficiency.
		 * Files are accumulated and formatted in batches to minimize IDEA process startups.
		 */
		String formatFile(State state, String unix, File file) throws Exception {
			batchLock.lock();
			try {
				// Store the state reference for batch processing
				if (currentState == null) {
					currentState = state;
				}

				// Check if we already have the formatted result cached
				if (formattedCache.containsKey(file)) {
					String result = formattedCache.remove(file);
					LOGGER.debug("Returning cached formatted result for file: {}", file);
					return result;
				}

				// Create a temporary file for this content
				File tempFile = Files.createTempFile(uniqueBuildFolder.toPath(), "spotless", file.getName()).toFile();
				Files.write(tempFile.toPath(), unix.getBytes(StandardCharsets.UTF_8));

				// Add to pending batch
				pendingBatch.add(new State.FileToFormat(tempFile, file));
				LOGGER.debug("Added file {} to pending batch (size: {})", file, pendingBatch.size());

				// If batch is full, process it
				if (pendingBatch.size() >= batchSize) {
					LOGGER.info("Batch size reached ({}/{}), processing batch", pendingBatch.size(), batchSize);
					processPendingBatch();
				}

				// Check cache again after potential batch processing
				if (formattedCache.containsKey(file)) {
					return formattedCache.remove(file);
				}

				// If still not in cache, we need to process immediately (shouldn't happen normally)
				// This is a safety fallback
				LOGGER.warn("File {} not found in cache after batch processing, forcing immediate format", file);
				List<State.FileToFormat> singleFileBatch = new ArrayList<>();
				singleFileBatch.add(new State.FileToFormat(tempFile, file));
				currentState.formatBatch(this, singleFileBatch);
				return formattedCache.remove(file);

			} finally {
				batchLock.unlock();
			}
		}

		/**
		 * Caches a formatted result for a file.
		 */
		void cacheFormattedResult(File originalFile, String formatted) {
			formattedCache.put(originalFile, formatted);
		}

		/**
		 * Processes all pending files in the current batch.
		 */
		private void processPendingBatch() throws Exception {
			if (pendingBatch.isEmpty() || currentState == null) {
				return;
			}

			List<State.FileToFormat> batchToProcess = new ArrayList<>(pendingBatch);
			pendingBatch.clear();

			try {
				currentState.formatBatch(this, batchToProcess);
			} finally {
				// Clean up temp files
				for (State.FileToFormat fileToFormat : batchToProcess) {
					try {
						Files.deleteIfExists(fileToFormat.tempFile.toPath());
					} catch (IOException e) {
						LOGGER.warn("Failed to delete temporary file: {}", fileToFormat.tempFile, e);
					}
				}
			}
		}

		@Override
		public void close() throws Exception {
			batchLock.lock();
			try {
				// Process any remaining files in the batch
				if (!pendingBatch.isEmpty()) {
					LOGGER.info("Processing remaining {} files in batch on close", pendingBatch.size());
					processPendingBatch();
				}
			} finally {
				batchLock.unlock();
			}

			// close the runner
			runner.close();
			// delete the unique build folder
			if (uniqueBuildFolder.exists()) {
				// delete the unique build folder recursively
				try (Stream<Path> paths = Files.walk(uniqueBuildFolder.toPath())) {
					paths.sorted((o1, o2) -> o2.compareTo(o1)) // delete files first
							.forEach(path -> {
								try {
									Files.delete(path);
								} catch (IOException e) {
									LOGGER.warn("Failed to delete file: {}", path, e);
								}
							});
				}
			}
		}

	}
}
