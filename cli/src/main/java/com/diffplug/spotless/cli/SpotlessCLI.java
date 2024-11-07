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

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ThrowingEx;
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
public class SpotlessCLI implements SpotlessAction, SpotlessCommand {

	@CommandLine.Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit.")
	boolean versionRequested;

	@CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message and exit.")
	boolean usageHelpRequested;

	@CommandLine.Option(names = {"--mode", "-m"}, defaultValue = "APPLY", description = "The mode to run spotless in." + OptionConstants.VALID_VALUES_SUFFIX + OptionConstants.DEFAULT_VALUE_SUFFIX, scope = CommandLine.ScopeType.INHERIT)
	SpotlessMode spotlessMode;

	@CommandLine.Option(names = {"--target", "-t"}, required = true, arity = "1..*", description = "The target files to format.", scope = CommandLine.ScopeType.INHERIT)
	public List<String> targets;

	@CommandLine.Option(names = {"--encoding", "-e"}, defaultValue = "ISO8859-1", description = "The encoding of the files to format." + OptionConstants.DEFAULT_VALUE_SUFFIX, scope = CommandLine.ScopeType.INHERIT)
	public Charset encoding;

	@CommandLine.Option(names = {"--line-ending", "-l"}, defaultValue = "UNIX", description = "The line ending of the files to format." + OptionConstants.VALID_VALUES_SUFFIX + OptionConstants.DEFAULT_VALUE_SUFFIX, scope = CommandLine.ScopeType.INHERIT)
	public LineEnding lineEnding;

	@Override
	public Integer executeSpotlessAction(@Nonnull List<FormatterStep> formatterSteps) {
		TargetResolver targetResolver = new TargetResolver(targets);

		try (Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(lineEnding.createPolicy())
				.encoding(encoding)
				.rootDir(Paths.get(".")) // TODO root dir?
				.steps(formatterSteps)
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.build()) {

			boolean success = targetResolver.resolveTargets()
					.parallel() // needed?
					.map(target -> this.executeFormatter(formatter, target))
					.filter(result -> result.success && result.updated != null)
					.peek(this::writeBack)
					.allMatch(result -> result.success);
			System.out.println("Hello " + getClass().getSimpleName() + ", abc! Files: " + new TargetResolver(targets).resolveTargets().collect(Collectors.toList()));
			System.out.println("success: " + success);
			formatterSteps.forEach(step -> System.out.println("Step: " + step));
			return 0;
		}
	}

	private Result executeFormatter(Formatter formatter, Path target) {
		System.out.println("Formatting file: " + target + " in Thread " + Thread.currentThread().getName());
		String targetContent = ThrowingEx.get(() -> Files.readString(target, Charset.defaultCharset())); // TODO charset!

		String computed = formatter.compute(targetContent, target.toFile());
		// computed is null if file already up to date
		return new Result(target, true, computed);
	}

	private void writeBack(Result result) {
		if (result.updated != null) {
			ThrowingEx.run(() -> Files.writeString(result.target, result.updated, Charset.defaultCharset())); // TODO charset!
		}
		//		System.out.println("Writing back to file:" + result.target + " with content:\n" + result.updated);
	}

	public static void main(String... args) {
		if (args.length == 0) {
			//		args = new String[]{"--version"};
			//					args = new String[]{"license-header", "--header-file", "CHANGES.md", "--delimiter-for", "java", "license-header", "--header", "abc"};

			args = new String[]{"--target", "src/poc/java/**/*.java", "--encoding=UTF-8", "license-header", "--header", "abc", "--delimiter-for", "java", "license-header", "--header-file", "TestHeader.txt"};
			//			args = new String[]{"--version"};
		}
		int exitCode = new CommandLine(new SpotlessCLI())
				.setExecutionStrategy(new SpotlessExecutionStrategy())
				.setCaseInsensitiveEnumValuesAllowed(true)
				.execute(args);
		System.exit(exitCode);
	}

	private enum SpotlessMode {
		CHECK, APPLY
	}

	private static final class Result {
		private final Path target;
		private final boolean success;
		private final String updated;

		public Result(Path target, boolean success, String updated) {
			this.target = target;
			this.success = success;
			this.updated = updated;
		}
	}
}
