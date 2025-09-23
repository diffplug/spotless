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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

		public FormatterStep build() {
			return create(this);
		}

		@Override
		public String toString() {
			return String.format(
					"IdeaStepBuilder[useDefaults=%s, binaryPath=%s, codeStyleSettingsPath=%s, ideaProperties=%s, buildDir=%s]",
					this.useDefaults,
					this.binaryPath,
					this.codeStyleSettingsPath,
					this.ideaProperties,
					this.buildDir);
		}
	}

	private static class State implements Serializable {

		private static final long serialVersionUID = -1426311255869303398L;

		private final File uniqueBuildFolder;
		private final String binaryPath;
		@Nullable private final String codeStyleSettingsPath;
		private final boolean withDefaults;
		private final TreeMap<String, String> ideaProperties;

		private State(@Nonnull IdeaStepBuilder builder) {
			LOGGER.debug("Creating {} state with configuration {}", NAME, builder);
			this.uniqueBuildFolder = new File(builder.buildDir, UUID.randomUUID().toString());
			this.withDefaults = builder.useDefaults;
			this.codeStyleSettingsPath = builder.codeStyleSettingsPath;
			this.ideaProperties = new TreeMap<>(builder.ideaProperties);
			this.binaryPath = resolveFullBinaryPathAndCheckVersion(builder.binaryPath);
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
			String testEnvBinaryPath = TestEnvVars.read().get(String.format("%s.%s", IdeaStep.class.getName(), "binaryPath"));
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

		private String format(IdeaStepFormatterCleanupResources ideaStepFormatterCleanupResources, String unix, File file) throws Exception {
			// since we cannot directly work with the file, we need to write the unix string to a temporary file
			File tempFile = File.createTempFile("spotless", file.getName());
			try {
				Files.write(tempFile.toPath(), unix.getBytes(StandardCharsets.UTF_8));
				List<String> params = getParams(tempFile);

				Map<String, String> env = createEnv();
				LOGGER.info("Launching IDEA formatter for orig file {} with params: {} and env: {}", file, params, env);
				var result = ideaStepFormatterCleanupResources.runner.exec(null, env, null, params);
				LOGGER.debug("command finished with exit code: {}", result.exitCode());
				LOGGER.debug("command finished with stdout: {}",
						result.assertExitZero(StandardCharsets.UTF_8));
				return Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);
			} finally {
				Files.delete(tempFile.toPath());
			}
		}

		private Map<String, String> createEnv() {
			File ideaProps = createIdeaPropertiesFile();
			Map<String, String> env = Map.ofEntries(
					Map.entry("IDEA_PROPERTIES", ThrowingEx.get(ideaProps::getCanonicalPath)));
			return env;
		}

		private File createIdeaPropertiesFile() {
			Path ideaProps = this.uniqueBuildFolder.toPath().resolve("idea.properties");

			if (Files.exists(ideaProps)) {
				return ideaProps.toFile(); // only create if it does not exist
			}

			Path parent = ideaProps.getParent();
			if (parent == null) {
				throw new IllegalStateException(String.format("Parent directory for IDEA properties file %s cannot be null", ideaProps));
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

		private List<String> getParams(File file) {
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
			builder.add("-charset").add("UTF-8");
			builder.add(ThrowingEx.get(file::getCanonicalPath));
			return builder.build().collect(Collectors.toList());
		}

		private FormatterFunc.Closeable toFunc() {
			IdeaStepFormatterCleanupResources ideaStepFormatterCleanupResources = new IdeaStepFormatterCleanupResources(uniqueBuildFolder, new ProcessRunner());
			return FormatterFunc.Closeable.of(ideaStepFormatterCleanupResources, this::format);
		}
	}

	private static class IdeaStepFormatterCleanupResources implements AutoCloseable {
		@Nonnull
		private final File uniqueBuildFolder;
		@Nonnull
		private final ProcessRunner runner;

		public IdeaStepFormatterCleanupResources(@Nonnull File uniqueBuildFolder, @Nonnull ProcessRunner runner) {
			this.uniqueBuildFolder = uniqueBuildFolder;
			this.runner = runner;
		}

		@Override
		public void close() throws Exception {
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
