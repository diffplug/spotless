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
package com.diffplug.spotless.python;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class BlackStep {
	public static String name() {
		return "black";
	}

	public static String defaultVersion() {
		return "22.3.0";
	}

	private final String version;
	private final @Nullable String pathToExe;

	private BlackStep(String version, @Nullable String pathToExe) {
		this.version = version;
		this.pathToExe = pathToExe;
	}

	public static BlackStep withVersion(String version) {
		return new BlackStep(version, null);
	}

	public BlackStep withPathToExe(String pathToExe) {
		return new BlackStep(version, pathToExe);
	}

	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	private State createState() throws IOException, InterruptedException {
		String trackingIssue = "\n  github issue to handle this better: https://github.com/diffplug/spotless/issues/674";
		ForeignExe exeAbsPath = ForeignExe.nameAndVersion("black", version)
				.pathToExe(pathToExe)
				.versionRegex(Pattern.compile("(?:black, version|black,|version) (\\S*)"))
				.fixCantFind("Try running {@code pip install black=={version}}, or else tell Spotless where it is with {@code black().pathToExe('path/to/executable')}" + trackingIssue)
				.fixWrongVersion("Try running {@code pip install --force-reinstall black=={version}}, or else specify {@code black('{versionFound}')} to Spotless" + trackingIssue);
		return new State(this, exeAbsPath);
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class State implements Serializable {
		private static final long serialVersionUID = -1825662356883926318L;
		// used for up-to-date checks and caching
		final String version;
		final transient ForeignExe exe;
		// used for executing
		private transient @Nullable String[] args;

		State(BlackStep step, ForeignExe exeAbsPath) {
			this.version = step.version;
			this.exe = Objects.requireNonNull(exeAbsPath);
		}

		String format(ProcessRunner runner, String input) throws IOException, InterruptedException {
			if (args == null) {
				args = new String[]{exe.confirmVersionAndGetAbsolutePath(), "-"};
			}
			return runner.exec(input.getBytes(StandardCharsets.UTF_8), args).assertExitZero(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
