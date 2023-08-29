/*
 * Copyright 2022-2023 DiffPlug
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
package com.diffplug.spotless.protobuf;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class BufStep {
	public static String name() {
		return "buf";
	}

	public static String defaultVersion() {
		return "1.24.0";
	}

	private final String version;
	private final @Nullable String pathToExe;

	private BufStep(String version, @Nullable String pathToExe) {
		this.version = version;
		this.pathToExe = pathToExe;
	}

	public static BufStep withVersion(String version) {
		return new BufStep(version, null);
	}

	public BufStep withPathToExe(String pathToExe) {
		return new BufStep(version, pathToExe);
	}

	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	private State createState() {
		String instructions = "https://docs.buf.build/installation";
		ForeignExe exeAbsPath = ForeignExe.nameAndVersion("buf", version)
				.pathToExe(pathToExe)
				.versionRegex(Pattern.compile("(\\S*)"))
				.fixCantFind("Try following the instructions at " + instructions + ", or else tell Spotless where it is with {@code buf().pathToExe('path/to/executable')}");
		return new State(this, exeAbsPath);
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class State implements Serializable {
		private static final long serialVersionUID = -1825662356883926318L;
		// used for up-to-date checks and caching
		final String version;
		final transient ForeignExe exe;
		// used for executing
		private transient @Nullable List<String> args;

		State(BufStep step, ForeignExe exeAbsPath) {
			this.version = step.version;
			this.exe = Objects.requireNonNull(exeAbsPath);
		}

		String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			if (args == null) {
				args = Arrays.asList(
						exe.confirmVersionAndGetAbsolutePath(),
						"format",
						file.getAbsolutePath());
			}
			return runner.exec(input.getBytes(StandardCharsets.UTF_8), args).assertExitZero(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
