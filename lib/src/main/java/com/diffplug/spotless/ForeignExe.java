/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.spotless;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Finds a foreign executable and checks its version.
 * If either part of that fails, it shows you why
 * and helps you fix it.
 */
public abstract class ForeignExe {
	public static ForeignExe named(String exeName) {
		return new ForeignExe() {
			@Override
			protected String name() {
				return exeName;
			}
		};
	}

	protected abstract String name();

	protected String versionFlag() {
		return "--version";
	}

	protected Pattern versionRegex() {
		return Pattern.compile("version (\\S*)");
	}

	public String confirmVersionAndGetPath(String versionExpected, @Nullable String pathToExe) throws IOException, InterruptedException, SetupException {
		try (ProcessRunner runner = new ProcessRunner()) {
			String exeAbsPath;
			if (pathToExe != null) {
				exeAbsPath = pathToExe;
			} else {
				ProcessRunner.Result which = runner.shellWinUnix("where " + name(), "which " + name());
				if (which.exitCode() != 0) {
					throw new SetupException(ErrorKind.CANT_FIND, "Unable to find " + name() + " on path", which);
				} else {
					exeAbsPath = which.assertNoError(Charset.defaultCharset()).trim();
				}
			}
			ProcessRunner.Result version = runner.exec(exeAbsPath, versionFlag());
			if (version.exitCode() != 0) {
				throw new SetupException(ErrorKind.CANT_FIND, "Unable to run " + exeAbsPath, version);
			}
			Matcher versionMatcher = versionRegex().matcher(version.assertNoError(Charset.defaultCharset()));
			if (!versionMatcher.find()) {
				throw new SetupException(ErrorKind.CANT_FIND, "Unable to parse version with /" + versionRegex() + "/", version);
			}
			String versionParsed = versionMatcher.group(1);
			if (!versionParsed.equals(versionExpected)) {
				throw new SetupException(ErrorKind.WRONG_VERSION, "You specified version " + versionExpected + ", but your system has " + versionParsed, version);
			}
			return exeAbsPath;
		}
	}

	public enum ErrorKind {
		CANT_FIND, WRONG_VERSION
	}

	public class SetupException extends Exception {
		private static final long serialVersionUID = -3515370807495069599L;

		private final ErrorKind kind;
		private final String msg;
		private final ProcessRunner.Result result;

		SetupException(ErrorKind kind, String msg, ProcessRunner.Result result) {
			this.kind = kind;
			this.msg = msg;
			this.result = result;
		}

		public ErrorKind getKind() {
			return kind;
		}

		public ProcessRunner.Result getProcessResult() {
			return result;
		}

		@Override
		public String toString() {
			return msg;
		}
	}
}
