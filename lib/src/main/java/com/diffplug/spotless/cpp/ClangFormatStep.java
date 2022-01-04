/*
 * Copyright 2020-2022 DiffPlug
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
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	private State createState() throws IOException, InterruptedException {
		String howToInstall = "" +
				"You can download clang-format from https://releases.llvm.org and " +
				"then point Spotless to it with {@code pathToExe('/path/to/clang-format')} " +
				"or you can use your platform's package manager:" +
				"\n  win:   choco install llvm --version {version}  (try dropping version if it fails)" +
				"\n  mac:   brew install clang-format (TODO: how to specify version?)" +
				"\n  linux: apt install clang-format  (try clang-format-{version} with dropped minor versions)" +
				"\n    github issue to handle this better: https://github.com/diffplug/spotless/issues/673";
		String exeAbsPath = ForeignExe.nameAndVersion("clang-format", version)
				.pathToExe(pathToExe)
				.fixCantFind(howToInstall)
				.fixWrongVersion(
						"You can tell Spotless to use the version you already have with {@code clangFormat('{versionFound}')}" +
								"or you can download the currently specified version, {version}.\n" + howToInstall)
				.confirmVersionAndGetAbsolutePath();
		return new State(this, exeAbsPath);
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class State implements Serializable {
		private static final long serialVersionUID = -1825662356883926318L;
		// used for up-to-date checks and caching
		final String version;
		final @Nullable String style;
		// used for executing
		final transient List<String> args;

		State(ClangFormatStep step, String exeAbsPath) {
			this.version = step.version;
			this.style = step.style;
			args = new ArrayList<>(2);
			args.add(exeAbsPath);
			if (style != null) {
				args.add("--style=" + style);
			}
		}

		String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			String[] processArgs = args.toArray(new String[args.size() + 1]);
			// add an argument to the end
			processArgs[args.size()] = "--assume-filename=" + file.getName();
			return runner.exec(input.getBytes(StandardCharsets.UTF_8), processArgs).assertExitZero(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
