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
package com.diffplug.spotless.cli;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.cli.core.FileResolver;
import com.diffplug.spotless.cli.core.SpotlessActionContext;
import com.diffplug.spotless.cli.core.TargetFileTypeInferer;
import com.diffplug.spotless.cli.core.TargetResolver;
import com.diffplug.spotless.cli.execution.SpotlessExecutionStrategy;
import com.diffplug.spotless.cli.help.OptionConstants;
import com.diffplug.spotless.cli.steps.generic.LicenseHeader;
import com.diffplug.spotless.cli.steps.generic.RemoveMeLaterSubCommand;
import com.diffplug.spotless.cli.version.SpotlessCLIVersionProvider;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "spotless", mixinStandardHelpOptions = true, versionProvider = SpotlessCLIVersionProvider.class, description = "Runs spotless", subcommandsRepeatable = true, subcommands = {
		LicenseHeader.class,
		RemoveMeLaterSubCommand.class})
public class SpotlessCLI implements SpotlessAction, SpotlessCommand, SpotlessActionContextProvider {

	@CommandLine.Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit.")
	boolean versionRequested;

	@CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message and exit.")
	boolean usageHelpRequested;

	@CommandLine.Option(names = {"--mode", "-m"}, defaultValue = "APPLY", description = "The mode to run spotless in." + OptionConstants.VALID_VALUES_SUFFIX + OptionConstants.DEFAULT_VALUE_SUFFIX, scope = CommandLine.ScopeType.INHERIT)
	SpotlessMode spotlessMode;

	@CommandLine.Option(names = {"--basedir"}, hidden = true, description = "The base directory to run spotless in. Intended for testing purposes only.")
	Path baseDir;

	@CommandLine.Option(names = {"--target", "-t"}, required = true, arity = "1..*", description = "The target files to format.", scope = CommandLine.ScopeType.INHERIT)
	public List<String> targets;

	@CommandLine.Option(names = {"--encoding", "-e"}, defaultValue = "ISO8859-1", description = "The encoding of the files to format." + OptionConstants.DEFAULT_VALUE_SUFFIX, scope = CommandLine.ScopeType.INHERIT)
	public Charset encoding;

	@CommandLine.Option(names = {"--line-ending", "-l"}, defaultValue = "UNIX", description = "The line ending of the files to format." + OptionConstants.VALID_VALUES_SUFFIX + OptionConstants.DEFAULT_VALUE_SUFFIX, scope = CommandLine.ScopeType.INHERIT)
	public LineEnding lineEnding;

	@Override
	public Integer executeSpotlessAction(@Nonnull List<FormatterStep> formatterSteps) {
		TargetResolver targetResolver = targetResolver();

		try (Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(lineEnding.createPolicy())
				.encoding(encoding)
				.steps(formatterSteps)
				.build()) {

			ResultType resultType = targetResolver.resolveTargets()
					.parallel() // needed?
					.map(path -> ThrowingEx.get(() -> new Result(path, LintState.of(formatter, path.toFile())))) // TODO handle suppressions, see SpotlessTaskImpl
					.map(result -> this.handleResult(formatter, result))
					.reduce(ResultType.CLEAN, ResultType::combineWith);
			return spotlessMode.translateResultTypeToExitCode(resultType);
		}
	}

	private ResultType handleResult(Formatter formatter, Result result) {
		if (result.lintState.isClean()) {
			//			System.out.println("File is clean: " + result.target.toFile().getName());
			return ResultType.CLEAN;
		}
		if (result.lintState.getDirtyState().didNotConverge()) {
			System.err.println("File did not converge: " + result.target.toFile().getName()); // TODO: where to print the output to?
			return ResultType.DID_NOT_CONVERGE;
		}
		return this.spotlessMode.handleResult(formatter, result);
	}

	private TargetResolver targetResolver() {
		return new TargetResolver(baseDir == null ? Path.of(File.separator) : baseDir, targets);
	}

	private Path baseDir() {
		return baseDir == null ? Path.of(File.separator) : baseDir;
	}

	@Override
	public SpotlessActionContext spotlessActionContext() {
		TargetResolver targetResolver = targetResolver();
		TargetFileTypeInferer targetFileTypeInferer = new TargetFileTypeInferer(targetResolver);
		return new SpotlessActionContext(targetFileTypeInferer.inferTargetFileType(), new FileResolver(baseDir()));
	}

	public static void main(String... args) {
		if (args.length == 0) {
			//		args = new String[]{"--version"};
			//					args = new String[]{"license-header", "--header-file", "CHANGES.md", "--delimiter-for", "java", "license-header", "--header", "abc"};

			args = new String[]{"--mode=CHECK", "--target", "src/poc/java/**/*.java", "--encoding=UTF-8", "license-header", "--header", "abc", "license-header", "--header-file", "TestHeader.txt"};
			//			args = new String[]{"--version"};
		}
		int exitCode = createCommandLine(createInstance())
				.execute(args);
		System.exit(exitCode);
	}

	static SpotlessCLI createInstance() {
		return new SpotlessCLI();
	}

	static CommandLine createCommandLine(SpotlessCLI spotlessCLI) {
		return new CommandLine(spotlessCLI)
				.setExecutionStrategy(new SpotlessExecutionStrategy())
				.setCaseInsensitiveEnumValuesAllowed(true);
	}

	private enum SpotlessMode {
		CHECK {
			@Override
			ResultType handleResult(Formatter formatter, Result result) {
				if (result.lintState.isHasLints()) {
					result.lintState.asStringOneLine(result.target.toFile(), formatter);
				} else {
					System.out.println(String.format("%s is violating formatting rules.", result.target));
				}
				return ResultType.DIRTY;
			}

			@Override
			Integer translateResultTypeToExitCode(ResultType resultType) {
				if (resultType == ResultType.CLEAN) {
					return 0;
				}
				if (resultType == ResultType.DIRTY) {
					return 1;
				}
				if (resultType == ResultType.DID_NOT_CONVERGE) {
					return -1;
				}
				throw new IllegalStateException("Unexpected result type: " + resultType);
			}
		},
		APPLY {
			@Override
			ResultType handleResult(Formatter formatter, Result result) {
				if (result.lintState.isHasLints()) {
					// something went wrong, we should not apply the changes
					System.err.println("File has lints: " + result.target.toFile().getName());
					return ResultType.DIRTY;
				}
				ThrowingEx.run(() -> result.lintState.getDirtyState().writeCanonicalTo(result.target.toFile()));
				return ResultType.CLEAN;
			}

			@Override
			Integer translateResultTypeToExitCode(ResultType resultType) {
				if (resultType == ResultType.CLEAN) {
					return 0;
				}
				if (resultType == ResultType.DIRTY) {
					return 0;
				}
				if (resultType == ResultType.DID_NOT_CONVERGE) {
					return -1;
				}
				throw new IllegalStateException("Unexpected result type: " + resultType);
			}
		};

		abstract ResultType handleResult(Formatter formatter, Result result);

		abstract Integer translateResultTypeToExitCode(ResultType resultType);

	}

	private enum ResultType {
		CLEAN, DIRTY, DID_NOT_CONVERGE;

		ResultType combineWith(ResultType other) {
			if (this == other) {
				return this;
			}
			if (this == DID_NOT_CONVERGE || other == DID_NOT_CONVERGE) {
				return DID_NOT_CONVERGE;
			}
			if (this == DIRTY || other == DIRTY) {
				return DIRTY;
			}
			throw new IllegalStateException("Unexpected combination of result types: " + this + " and " + other);
		}
	}

	private static final class Result {
		private final Path target;
		private final LintState lintState;

		public Result(Path target, LintState lintState) {
			this.target = target;
			this.lintState = lintState;
		}
	}
}
