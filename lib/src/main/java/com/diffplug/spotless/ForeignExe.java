/*
 * Copyright 2020-2021 DiffPlug
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Finds a foreign executable and checks its version.
 * If either part of that fails, it shows you why
 * and helps you fix it.
 *
 * Usage: {@code ForeignExe.nameAndVersion("grep", "2.5.7").confirmVersionAndGetAbsolutePath()}
 * will find grep, confirm that it is version 2.5.7, and then return.
 */
public class ForeignExe {
	private @Nullable String pathToExe;
	private String versionFlag = "--version";
	private Pattern versionRegex = Pattern.compile("version (\\S*)");
	private @Nullable String fixCantFind, fixWrongVersion;

	// MANDATORY
	private String name;
	private String version;

	/** The name of the executable, used by "where" (win) and "which" (unix). */
	public static ForeignExe nameAndVersion(String exeName, String version) {
		ForeignExe foreign = new ForeignExe();
		foreign.name = Objects.requireNonNull(exeName);
		foreign.version = Objects.requireNonNull(version);
		return foreign;
	}

	/** The flag which causes the exe to print its version (defaults to --version). */
	public ForeignExe versionFlag(String versionFlag) {
		this.versionFlag = Objects.requireNonNull(versionFlag);
		return this;
	}

	/** A regex which can parse the version out of the output of the {@link #versionFlag(String)} command (defaults to {@code version (\\S*)}) */
	public ForeignExe versionRegex(Pattern versionRegex) {
		this.versionRegex = Objects.requireNonNull(versionRegex);
		return this;
	}

	/** Use {version} anywhere you would like to inject the actual version string. */
	public ForeignExe fixCantFind(String msg) {
		this.fixCantFind = msg;
		return this;
	}

	/** Use {version} or {versionFound} anywhere you would like to inject the actual version strings. */
	public ForeignExe fixWrongVersion(String msg) {
		this.fixWrongVersion = msg;
		return this;
	}

	/** Path to the executable. If null, will search for the executable on the system path. */
	public ForeignExe pathToExe(@Nullable String pathToExe) {
		this.pathToExe = pathToExe;
		return this;
	}

	/**
	 * Searches for the executable and confirms that it has the expected version.
	 * If it can't find the executable, or if it doesn't have the correct version,
	 * throws an exception with a message describing how to fix.
	 */
	public String confirmVersionAndGetAbsolutePath() throws IOException, InterruptedException {
		try (ProcessRunner runner = new ProcessRunner()) {
			String exeAbsPath;
			if (pathToExe != null) {
				exeAbsPath = pathToExe;
			} else {
				ProcessRunner.Result cmdWhich = runner.shellWinUnix("where " + name, "which " + name);
				if (cmdWhich.exitNotZero()) {
					throw cantFind("Unable to find " + name + " on path", cmdWhich);
				} else {
					exeAbsPath = cmdWhich.assertExitZero(Charset.defaultCharset()).trim();
				}
			}
			ProcessRunner.Result cmdVersion = runner.exec(exeAbsPath, versionFlag);
			if (cmdVersion.exitNotZero()) {
				throw cantFind("Unable to run " + exeAbsPath, cmdVersion);
			}
			Matcher versionMatcher = versionRegex.matcher(cmdVersion.assertExitZero(Charset.defaultCharset()));
			if (!versionMatcher.find()) {
				throw cantFind("Unable to parse version with /" + versionRegex + "/", cmdVersion);
			}
			String versionFound = versionMatcher.group(1);
			if (!versionFound.equals(version)) {
				throw wrongVersion("You specified version " + version + ", but Spotless found " + versionFound, cmdVersion, versionFound);
			}
			return exeAbsPath;
		}
	}

	private RuntimeException cantFind(String message, ProcessRunner.Result cmd) {
		return exceptionFmt(message, cmd, fixCantFind == null ? null : fixCantFind.replace("{version}", version));
	}

	private RuntimeException wrongVersion(String message, ProcessRunner.Result cmd, String versionFound) {
		return exceptionFmt(message, cmd, fixWrongVersion == null ? null : fixWrongVersion.replace("{version}", version).replace("{versionFound}", versionFound));
	}

	private RuntimeException exceptionFmt(String msgPrimary, ProcessRunner.Result cmd, @Nullable String msgFix) {
		StringBuilder errorMsg = new StringBuilder();
		errorMsg.append(msgPrimary);
		errorMsg.append('\n');
		if (msgFix != null) {
			errorMsg.append(msgFix);
			errorMsg.append('\n');
		}
		errorMsg.append(cmd.toString());
		return new RuntimeException(errorMsg.toString());
	}
}
