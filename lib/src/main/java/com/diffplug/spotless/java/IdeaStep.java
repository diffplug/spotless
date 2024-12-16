/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.java;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
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

	private IdeaStep() {}

	public static FormatterStep create() {
		return create(true);
	}

	public static FormatterStep create(boolean withDefaults) {
		return create(withDefaults, null);
	}

	public static FormatterStep create(boolean withDefaults,
			@Nullable String binaryPath) {
		return create(withDefaults, binaryPath, null);
	}

	public static FormatterStep create(boolean withDefaults,
			@Nullable String binaryPath, @Nullable String codeStyleSettingsPath) {
		return FormatterStep.createLazy("IDEA",
				() -> createState(withDefaults, binaryPath, codeStyleSettingsPath),
				state -> state);
	}

	private static State createState(boolean withDefaults,
			@Nullable String binaryPath, @Nullable String codeStyleSettingsPath) {
		return new State(withDefaults, binaryPath, codeStyleSettingsPath);
	}

	private static class State
			implements FormatterFunc.NeedsFile, Serializable {

		private static final long serialVersionUID = -1825662355363926318L;
		private static final String DEFAULT_IDEA = "idea";

		private String binaryPath;
		@Nullable
		private String codeStyleSettingsPath;
		private boolean withDefaults;

		private State(boolean withDefaults, @Nullable String binaryPath,
				@Nullable String codeStyleSettingsPath) {
			this.withDefaults = withDefaults;
			this.codeStyleSettingsPath = codeStyleSettingsPath;
			this.binaryPath = Objects.requireNonNullElse(binaryPath, DEFAULT_IDEA);
			resolveFullBinaryPathAndCheckVersion();
		}

		private void resolveFullBinaryPathAndCheckVersion() {
			var exe = ForeignExe
					.nameAndVersion(this.binaryPath, "IntelliJ IDEA")
					.pathToExe(pathToExe())
					.versionRegex(Pattern.compile("(IntelliJ IDEA) .*"))
					.fixCantFind(
							"IDEA executable cannot be found on your machine, "
									+ "please install it and put idea binary to PATH, provide a valid path to the executable or report the problem")
					.fixWrongVersion("Provided binary is not IDEA, "
							+ "please check it and fix the problem; or report the problem");
			try {
				this.binaryPath = exe.confirmVersionAndGetAbsolutePath();
			} catch (IOException e) {
				throw new IllegalArgumentException("binary cannot be found", e);
			} catch (InterruptedException e) {
				throw new IllegalArgumentException(
						"binary cannot be found, process was interrupted", e);
			}
		}

		@CheckForNull
		private String pathToExe() {
			if (binaryPath == null) {
				throw new IllegalStateException("binaryPath is not set");
			}
			if (new File(binaryPath).exists()) {
				return binaryPath;
			}
			return null; // search in PATH
		}

		@Override
		public String applyWithFile(String unix, File file) throws Exception {
			// since we cannot directly work with the file, we need to write the unix string to a temporary file
			File tempFile = File.createTempFile("spotless", file.getName());
			try {
				Files.write(tempFile.toPath(), unix.getBytes(StandardCharsets.UTF_8));
				List<String> params = getParams(tempFile);

				try (ProcessRunner runner = new ProcessRunner()) {
					var result = runner.exec(params);
					LOGGER.debug("command finished with stdout: {}",
							result.assertExitZero(StandardCharsets.UTF_8));

					return Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);
				}
			} finally {
				Files.delete(tempFile.toPath());
			}
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
			builder.add(ThrowingEx.get(file::getCanonicalPath));
			return builder.build().collect(Collectors.toList());
		}
	}
}
