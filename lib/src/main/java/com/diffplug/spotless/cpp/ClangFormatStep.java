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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
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
		final ForeignExe exe = ForeignExe.nameAndVersion("clang-format", version)
				.pathToExe(pathToExe)
				.fixCantFind(howToInstall)
				.fixWrongVersion(
						"You can tell Spotless to use the version you already have with {@code clangFormat('{versionFound}')}" +
								"or you can download the currently specified version, {version}.\n" + howToInstall);
		return new State(this, exe);
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	static class State implements Serializable {
		private static final long serialVersionUID = -1825662356883926318L;
		private static final String DOT_FILE_NAME  = ".clang-format";
		// used for up-to-date checks and caching
		final String version;
		final @Nullable String style;
		final transient ForeignExe exe;
		final Set<File>     dotFiles;
		FileSignature dotFileSig;
		// used for executing
		private transient @Nullable List<String> args;

		State(ClangFormatStep step, ForeignExe pathToExe, @Nullable FileSignature sig) {
			this.version = step.version;
			this.style = step.style;
			this.exe = Objects.requireNonNull(pathToExe);
			this.dotFiles = new HashSet<>();
			this.dotFileSig = sig;
		}

		State(ClangFormatStep step, ForeignExe pathToExe) {
			this(step, pathToExe, null);
		}

		/**
		 * If relevant, locates the `.clang-format` file that will be used as config
		 * for clang-format and stores its signature in dotFile.
		 * @param targetFile file to be formatted.
		 */
		private void resolveDotFile(File targetFile) throws IOException
		{
			// The dot file is irrelevant if a specific style other than "file" is supplied.
			if (style != null && !style.equals("file")) {
				return;
			}

			File directory = targetFile.getParentFile();
			Optional<File> dotFile = Optional.empty();
			while (dotFile.isEmpty() && readableDirectory(directory)) {
				dotFile = Arrays.stream(directory.listFiles()).filter(file -> file.getName().equals(DOT_FILE_NAME)).findAny();
				directory = directory.getParentFile();
			}

			System.out.println("dotFile: " + dotFile);
			// Every target file can have a different .clang-format file (in theory).
			// Keep track of the ones we've covered and build the sig as we go.
			if (dotFile.isPresent() && !dotFiles.contains(dotFile.get())) {
				dotFiles.add(dotFile.get());
				dotFileSig = FileSignature.signAsSet(dotFiles);
			}
			System.out.println("Signature" + dotFileSig);
		}


		private static boolean readableDirectory(File directory)
		{
			return directory != null && directory.exists() && directory.isDirectory() && directory.canRead();
		}


		String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			resolveDotFile(file);
			if (args == null) {
				final List<String> tmpArgs = new ArrayList<>();
				tmpArgs.add(exe.confirmVersionAndGetAbsolutePath());
				if (style != null) {
					tmpArgs.add("--style=" + style);
				}
				args = tmpArgs;
			}
			final String[] processArgs = args.toArray(new String[args.size() + 1]);
			processArgs[processArgs.length - 1] = "--assume-filename=" + file.getAbsolutePath();
			System.out.println(String.join(", ", processArgs));
			return runner.exec(input.getBytes(StandardCharsets.UTF_8), processArgs).assertExitZero(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			ProcessRunner runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
