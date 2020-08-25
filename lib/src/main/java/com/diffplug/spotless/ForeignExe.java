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
public class ForeignExe {
	private String name;
	private String versionFlag = "--version";
	private Pattern versionRegex = Pattern.compile("version (\\S*)");
	private @Nullable String fixCantFind, fixWrongVersion;

	/** The name of the executable, used by "where" (win) and "which" (unix). */
	public static ForeignExe named(String exeName) {
		ForeignExe foreign = new ForeignExe();
		foreign.name = exeName;
		return foreign;
	}

	/** The flag which causes the exe to print its version (defaults to --version). */
	public ForeignExe versionFlag(String versionFlag) {
		this.versionFlag = versionFlag;
		return this;
	}

	/** A regex which can parse the version out of the output of the {@link #versionFlag(String)} command (defaults to `version (\\S*)`) */
	public ForeignExe versionRegex(Pattern versionRegex) {
		this.versionRegex = versionRegex;
		return this;
	}

	/** Use {version} anywhere you would like to inject the actual version string. */
	public ForeignExe fixCantFind(String msg) {
		this.fixCantFind = msg;
		return this;
	}

	/** Use {version} or {versionActual} anywhere you would like to inject the actual version strings. */
	public ForeignExe fixWrongVersion(String msg) {
		this.fixWrongVersion = msg;
		return this;
	}

	/**
	 * Searches for the executable and confirms that it has the expected version.
	 * If it can't find the executable, or if it doesn't have the correct version,
	 * throws an exception with a message describing how to fix.
	 */
	public String confirmVersionAndGetPath(String version, @Nullable String pathToExe) throws IOException, InterruptedException {
		try (ProcessRunner runner = new ProcessRunner()) {
			String exeAbsPath;
			if (pathToExe != null) {
				exeAbsPath = pathToExe;
			} else {
				ProcessRunner.Result cmdWhich = runner.shellWinUnix("where " + name, "which " + name);
				if (cmdWhich.exitCode() != 0) {
					throw cantFind("Unable to find " + name + " on path", cmdWhich, version);
				} else {
					exeAbsPath = cmdWhich.assertNoError(Charset.defaultCharset()).trim();
				}
			}
			ProcessRunner.Result cmdVersion = runner.exec(exeAbsPath, versionFlag);
			if (cmdVersion.exitCode() != 0) {
				throw cantFind("Unable to run " + exeAbsPath, cmdVersion, version);
			}
			Matcher versionMatcher = versionRegex.matcher(cmdVersion.assertNoError(Charset.defaultCharset()));
			if (!versionMatcher.find()) {
				throw cantFind("Unable to parse version with /" + versionRegex + "/", cmdVersion, version);
			}
			String versionActual = versionMatcher.group(1);
			if (!versionActual.equals(version)) {
				throw wrongVersion("You specified version " + version + ", but Spotless found " + versionActual, cmdVersion, version, versionActual);
			}
			return exeAbsPath;
		}
	}

	private RuntimeException cantFind(String message, ProcessRunner.Result cmd, String versionExpected) {
		StringBuilder errorMsg = new StringBuilder();
		errorMsg.append(message);
		errorMsg.append('\n');
		if (fixCantFind != null) {
			errorMsg.append(fixCantFind.replace("{version}", versionExpected));
			errorMsg.append('\n');
		}
		errorMsg.append('\n');
		errorMsg.append(cmd.toString());
		throw new RuntimeException(errorMsg.toString());
	}

	private RuntimeException wrongVersion(String message, ProcessRunner.Result cmd, String versionExpected, String versionActual) {
		StringBuilder errorMsg = new StringBuilder();
		errorMsg.append(message);
		errorMsg.append('\n');
		if (fixCantFind != null) {
			errorMsg.append(fixCantFind.replace("{version}", versionExpected).replace("{versionActual}", versionActual));
			errorMsg.append('\n');
		}
		errorMsg.append('\n');
		errorMsg.append(cmd.toString());
		throw new RuntimeException(errorMsg.toString());
	}

}
