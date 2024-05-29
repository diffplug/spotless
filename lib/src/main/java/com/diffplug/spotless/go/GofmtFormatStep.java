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
package com.diffplug.spotless.go;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Note: gofmt doesn't have a version flag, because it's part of standard Go distribution.
 * So `go` executable can be used to determine base path and version, and path to gofmt can be built from it.
 */
public class GofmtFormatStep {
	public static String name() {
		return "gofmt";
	}

	public static String defaultVersion() {
		return "go1.20.0";
	}

	private final String version;
	private final @Nullable String pathToExe;

	private GofmtFormatStep(String version, String pathToExe) {
		this.version = version;
		this.pathToExe = pathToExe;
	}

	public static GofmtFormatStep withVersion(String version) {
		return new GofmtFormatStep(version, null);
	}

	public GofmtFormatStep withGoExecutable(String pathToExe) {
		return new GofmtFormatStep(version, pathToExe);
	}

	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createRountrip, RoundtripState::toEquality, EqualityState::toFunc);
	}

	private RoundtripState createRountrip() throws IOException, InterruptedException {
		String howToInstall = "gofmt is a part of standard go distribution. If spotless can't discover it automatically, " +
				"you can point Spotless to the go binary with {@code pathToExe('/path/to/go')}";
		final ForeignExe exe = ForeignExe.nameAndVersion("go", version)
				.pathToExe(pathToExe)
				.versionFlag("version")
				.fixCantFind(howToInstall)
				.fixWrongVersion(
						"You can tell Spotless to use the version you already have with {@code gofmt('{versionFound}')}" +
								"or you can install the currently specified Go version, {version}.\n" + howToInstall);
		return new RoundtripState(version, exe);
	}

	static class RoundtripState implements Serializable {
		private static final long serialVersionUID = 1L;

		final String version;
		final ForeignExe exe;

		RoundtripState(String version, ForeignExe exe) {
			this.version = version;
			this.exe = exe;
		}

		private EqualityState toEquality() {
			return new EqualityState(version, exe);
		}
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class EqualityState implements Serializable {
		private static final long serialVersionUID = -1825662355363926318L;
		// used for up-to-date checks and caching
		final String version;
		final transient ForeignExe exe;

		public EqualityState(String version, ForeignExe goExecutable) {
			this.version = version;
			this.exe = Objects.requireNonNull(goExecutable);
		}

		String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			final List<String> processArgs = new ArrayList<>();
			String pathToGoBinary = exe.confirmVersionAndGetAbsolutePath();
			Path goBasePath = Path.of(pathToGoBinary).getParent();
			if (goBasePath == null) {
				throw new IllegalStateException("Unable to resolve base path of Go installation directory");
			}
			String pathToGoFmt = goBasePath.resolve("gofmt").toString();
			processArgs.add(pathToGoFmt);
			return runner.exec(input.getBytes(StandardCharsets.UTF_8), processArgs).assertExitZero(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
