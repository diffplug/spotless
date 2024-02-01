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
package com.diffplug.spotless.shell;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ShfmtStep {
	public static String name() {
		return "shfmt";
	}

	public static String defaultVersion() {
		return "3.7.0";
	}

	private final String version;
	private final @Nullable String pathToExe;

	private ShfmtStep(String version, @Nullable String pathToExe) {
		this.version = version;
		this.pathToExe = pathToExe;
	}

	public static ShfmtStep withVersion(String version) {
		return new ShfmtStep(version, null);
	}

	public ShfmtStep withPathToExe(String pathToExe) {
		return new ShfmtStep(version, pathToExe);
	}

	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	private State createState() throws IOException, InterruptedException {
		String howToInstall = "" +
				"You can download shfmt from https://github.com/mvdan/sh and " +
				"then point Spotless to it with {@code pathToExe('/path/to/shfmt')} " +
				"or you can use your platform's package manager:" +
				"\n  win:   choco install shfmt" +
				"\n  mac:   brew install shfmt" +
				"\n  linux: apt install shfmt" +
				"\n    github issue to handle this better: https://github.com/diffplug/spotless/issues/673";
		final ForeignExe exe = ForeignExe.nameAndVersion("shfmt", version)
				.pathToExe(pathToExe)
				.versionRegex(Pattern.compile("(\\S*)"))
				.fixCantFind(howToInstall)
				.fixWrongVersion(
						"You can tell Spotless to use the version you already have with {@code shfmt('{versionFound}')}" +
								"or you can download the currently specified version, {version}.\n" + howToInstall);
		return new State(this, exe);
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class State implements Serializable {
		private static final long serialVersionUID = -1825662356883926318L;
		// used for up-to-date checks and caching
		final String version;
		final transient ForeignExe exe;

		State(ShfmtStep step, ForeignExe pathToExe) {
			this.version = step.version;
			this.exe = Objects.requireNonNull(pathToExe);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.ofDangerous(runner, new ShfmtFilePathPassingFormatterFunc(runner, exe));
		}
	}

	private static class ShfmtFilePathPassingFormatterFunc implements FormatterFunc.NeedsFile {
		// used for executing
		private transient @Nullable List<String> args;
		private final transient ForeignExe exe;
		private final transient ProcessRunner runner;

		ShfmtFilePathPassingFormatterFunc(ProcessRunner runner, ForeignExe exe) {
			this.runner = runner;
			this.exe = exe;
		}

		@Override
		public String applyWithFile(String unix, File file) throws Exception {
			return format(runner, unix, file);
		}

		String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			if (args == null) {
				args = List.of(exe.confirmVersionAndGetAbsolutePath(), file.getAbsolutePath());
			}

			return runner.exec(args).assertExitZero(StandardCharsets.UTF_8);
		}
	}
}
