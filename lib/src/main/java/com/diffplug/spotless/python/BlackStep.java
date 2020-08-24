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
package com.diffplug.spotless.python;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

public class BlackStep {
	public static String name() {
		return "black";
	}

	public static String defaultVersion() {
		return "19.10b0";
	}

	private final String version;
	private final @Nullable String pathToBlack;

	private BlackStep(String version, @Nullable String pathToBlack) {
		this.version = version;
		this.pathToBlack = pathToBlack;
	}

	public static BlackStep withVersion(String version) {
		return new BlackStep(version, null);
	}

	public BlackStep withPathToBlack(String pathToBlack) {
		return new BlackStep(version, pathToBlack);
	}

	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	private State createState() throws BlackException, IOException, InterruptedException {
		try (ProcessRunner runner = new ProcessRunner()) {
			String blackExe;
			if (pathToBlack != null) {
				blackExe = pathToBlack;
			} else {
				ProcessRunner.Result which = runner.shellWinUnix("where black", "which black");
				if (which.exitCode() != 0) {
					throw new BlackException(ErrorKind.CANT_FIND_BLACK, which);
				} else {
					blackExe = which.assertNoError(Charset.defaultCharset()).trim();
				}
			}
			ProcessRunner.Result blackVersion = runner.exec(blackExe, "--version");
			if (blackVersion.exitCode() != 0) {
				throw new BlackException(ErrorKind.CANT_FIND_BLACK, blackVersion);
			}
			String versionString = blackVersion.assertNoError(Charset.defaultCharset());
			Matcher versionMatcher = Pattern.compile("black, version (.*)").matcher(versionString);
			if (!versionMatcher.find()) {
				throw new BlackException(ErrorKind.CANT_FIND_BLACK, blackVersion);
			}
			String versionParsed = versionMatcher.group(1);
			if (!versionParsed.equals(version)) {
				throw new BlackException(ErrorKind.BLACK_WRONG_VERSION, blackVersion);
			}
			return new State(version, blackExe);
		}
	}

	public enum ErrorKind {
		CANT_FIND_BLACK, BLACK_WRONG_VERSION
	}

	public static class BlackException extends Exception {
		private static final long serialVersionUID = -1310199343691600283L;

		private ErrorKind kind;
		private ProcessRunner.Result result;

		BlackException(ErrorKind kind, ProcessRunner.Result result) {
			this.kind = kind;
			this.result = result;
		}
	}

	static class State implements Serializable {
		private static final long serialVersionUID = -1825662356883926318L;
		// used for up-to-date checks and caching
		final String version;
		// used for executing
		final transient String exeAbsPath;
		final transient ProcessRunner runner = new ProcessRunner();

		State(String version, String exeAbsPath) {
			this.version = version;
			this.exeAbsPath = exeAbsPath;
		}

		String format(String input) throws IOException, InterruptedException {
			return runner.exec(input.getBytes(StandardCharsets.UTF_8), exeAbsPath, "-").assertNoError(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}

	/** Either returns the "black" on the current path, or throws an exception. */
	public static File findBlackOnPath() throws IOException, InterruptedException {
		try (ProcessRunner runner = new ProcessRunner()) {
			ProcessRunner.Result result = runner.shellWinUnix("where black", "which black");
			return new File(result.assertNoError(Charset.defaultCharset()));
		} catch (Exception e) {
			throw new RuntimeException("Could not find 'black' on the path, try `pip install black` or `pip3 install black`", e);
		}
	}
}
