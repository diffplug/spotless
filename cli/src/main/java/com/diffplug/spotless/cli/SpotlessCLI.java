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
import java.util.List;

import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.cli.execution.SpotlessExecutionStrategy;
import com.diffplug.spotless.cli.help.OptionConstants;
import com.diffplug.spotless.cli.subcommands.SpotlessApply;
import com.diffplug.spotless.cli.subcommands.SpotlessCheck;
import com.diffplug.spotless.cli.version.SpotlessCLIVersionProvider;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "spotless", mixinStandardHelpOptions = true, versionProvider = SpotlessCLIVersionProvider.class, description = "Runs spotless", subcommands = {SpotlessCheck.class, SpotlessApply.class})
public class SpotlessCLI implements SpotlessCommand {

	@CommandLine.Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit")
	boolean versionRequested;

	@CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
	boolean usageHelpRequested;

	@CommandLine.Option(names = {"--target", "-t"}, required = true, arity = "1..*", description = "The target files to format", scope = CommandLine.ScopeType.INHERIT)
	public List<String> targets;

	@CommandLine.Option(names = {"--encoding", "-e"}, defaultValue = "ISO8859-1", description = "The encoding of the files to format." + OptionConstants.DEFAULT_VALUE_SUFFIX, scope = CommandLine.ScopeType.INHERIT)
	public Charset encoding;

	@CommandLine.Option(names = {"--line-ending", "-l"}, defaultValue = "UNIX", description = "The line ending of the files to format." + OptionConstants.VALID_VALUES_SUFFIX + OptionConstants.DEFAULT_VALUE_SUFFIX, scope = CommandLine.ScopeType.INHERIT)
	public LineEnding lineEnding;

	public static void main(String... args) {
		if (args.length == 0) {
			//		args = new String[]{"--version"};
			//		args = new String[]{"apply", "license-header", "--header-file", "CHANGES.md", "--delimiter-for", "java", "license-header", "--header", "abc"};
			args = new String[]{"--version"};
		}
		//		args = new String[]{"apply", "--target", "src/poc/java/**/*.java", "--encoding=UTF-8", "license-header", "--header", "abc", "--delimiter-for", "java", "license-header", "--header-file", "TestHeader.txt"};
		int exitCode = new CommandLine(new SpotlessCLI())
				.setExecutionStrategy(new SpotlessExecutionStrategy())
				.setCaseInsensitiveEnumValuesAllowed(true)
				.execute(args);
		System.exit(exitCode);
	}
}
