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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.ProcessRunner;

public final class IdeaFormatterFunc implements FormatterFunc.NeedsFile {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdeaStep.class);

	private static final String DEFAULT_IDEA = "idea";

	private String binaryPath;
	@Nullable
	private String configPath;
	private boolean withDefaults;

	private IdeaFormatterFunc(boolean withDefaults, @Nullable String binaryPath,
			@Nullable String configPath) {
		this.withDefaults = withDefaults;
		this.configPath = configPath;
		this.binaryPath = Objects.requireNonNullElse(binaryPath, DEFAULT_IDEA);
		resolveFullBinaryPathAndCheckVersion();
	}

	private void resolveFullBinaryPathAndCheckVersion() {
		var exe = ForeignExe.nameAndVersion(this.binaryPath, "IntelliJ IDEA")
				.versionRegex(Pattern.compile("(IntelliJ IDEA) .*"))
				.fixCantFind("IDEA executable cannot be found on your machine, "
						+ "please install it and put idea binary to PATH; or report the problem")
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

	public static IdeaFormatterFunc allowingDefaultsWithCustomBinary(
			@Nullable String binaryPath, @Nullable String configPath) {
		return new IdeaFormatterFunc(true, binaryPath, configPath);
	}

	public static IdeaFormatterFunc noDefaultsWithCustomBinary(
			@Nullable String binaryPath, @Nullable String configPath) {
		return new IdeaFormatterFunc(false, binaryPath, configPath);
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		List<String> params = getParams(file);

		try (ProcessRunner runner = new ProcessRunner()) {
			var result = runner.exec(params);

			LOGGER.debug("command finished with stdout: {}",
					result.assertExitZero(StandardCharsets.UTF_8));

			return Files.readString(file.toPath());
		}
	}

	private List<String> getParams(File file) {
		var builder = Stream.<String> builder();
		builder.add(binaryPath);
		builder.add("format");
		if (withDefaults) {
			builder.add("-allowDefaults");
		}
		if (configPath != null) {
			builder.add("-s");
			builder.add(configPath);
		}
		builder.add(file.toString());
		return builder.build().collect(Collectors.toList());
	}

}
