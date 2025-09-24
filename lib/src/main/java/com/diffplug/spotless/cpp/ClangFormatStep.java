/*
 * Copyright 2020-2025 DiffPlug
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
package com.diffplug.spotless.cpp;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ClangFormatStep {
	public static String name() {
		return "clang";
	}

	public static String defaultVersion() {
		return "10.0.1";
	}

	private final String version;
	private final @Nullable String pathToExe;
	private final @Nullable String style;

	private ClangFormatStep(String version, @Nullable String pathToExe, @Nullable String style) {
		this.version = version;
		this.pathToExe = pathToExe;
		this.style = style;
	}

	public static ClangFormatStep withVersion(String version) {
		return new ClangFormatStep(version, null, null);
	}

	public ClangFormatStep withStyle(String style) {
		return new ClangFormatStep(version, pathToExe, style);
	}

	public ClangFormatStep withPathToExe(String pathToExe) {
		return new ClangFormatStep(version, pathToExe, style);
	}

	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createRoundtrip, RoundtripState::toEquality, EqualityState::toFunc);
	}

	private RoundtripState createRoundtrip() throws IOException, InterruptedException {
		String howToInstall = """
				You can download clang-format from https://releases.llvm.org and \
				then point Spotless to it with {@code pathToExe('/path/to/clang-format')} \
				or you can use your platform's package manager:
				  win:   choco install llvm --version {version}  (try dropping version if it fails)
				  mac:   brew install clang-format (TODO: how to specify version?)
				  linux: apt install clang-format  (try clang-format-{version} with dropped minor versions)
				    github issue to handle this better: https://github.com/diffplug/spotless/issues/673""";
		final ForeignExe exe = ForeignExe.nameAndVersion("clang-format", version)
				.pathToExe(pathToExe)
				.fixCantFind(howToInstall)
				.fixWrongVersion(
						"You can tell Spotless to use the version you already have with {@code clangFormat('{versionFound}')}" +
								"or you can download the currently specified version, {version}.\n" + howToInstall);
		return new RoundtripState(this, exe);
	}

	static class RoundtripState implements Serializable {
		private static final long serialVersionUID = 1L;

		final String version;
		final @Nullable String style;
		final ForeignExe exe;

		RoundtripState(ClangFormatStep step, ForeignExe exe) {
			this.version = step.version;
			this.style = step.style;
			this.exe = exe;
		}

		private EqualityState toEquality() {
			return new EqualityState(version, style, exe);
		}
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class EqualityState implements Serializable {
		private static final long serialVersionUID = -1825662356883926318L;
		// used for up-to-date checks and caching
		final String version;
		final @Nullable String style;
		final transient ForeignExe exe;
		// used for executing
		private transient @Nullable List<String> args;

		EqualityState(String version, @Nullable String style, ForeignExe pathToExe) {
			this.version = version;
			this.style = style;
			this.exe = Objects.requireNonNull(pathToExe);
		}

		String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			if (args == null) {
				final List<String> tmpArgs = new ArrayList<>();
				tmpArgs.add(exe.confirmVersionAndGetAbsolutePath());
				if (style != null) {
					tmpArgs.add("--style=" + style);
				}
				args = tmpArgs;
			}
			final String[] processArgs = args.toArray(new String[args.size() + 1]);
			processArgs[processArgs.length - 1] = "--assume-filename=" + file.getName();
			return runner.exec(input.getBytes(StandardCharsets.UTF_8), processArgs).assertExitZero(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
