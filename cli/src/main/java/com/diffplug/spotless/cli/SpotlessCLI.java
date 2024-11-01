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

import com.diffplug.spotless.cli.execution.SpotlessExecutionStrategy;
import com.diffplug.spotless.cli.subcommands.SpotlessApply;
import com.diffplug.spotless.cli.subcommands.SpotlessCheck;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "spotless cli", mixinStandardHelpOptions = true, version = "spotless ${version}", // https://picocli.info/#_dynamic_version_information
		description = "Runs spotless", subcommands = {SpotlessCheck.class, SpotlessApply.class})
public class SpotlessCLI implements SpotlessCommand {

	@CommandLine.Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit")
	boolean versionRequested;

	@CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
	boolean usageHelpRequested;

	public static void main(String... args) {
		//		args = new String[]{"--version"};
		args = new String[]{"apply", "license-header", "--header-file", "CHANGES.md", "--delimiter-for", "java", "license-header", "--header", "abc"};
		int exitCode = new CommandLine(new SpotlessCLI())
				.setExecutionStrategy(new SpotlessExecutionStrategy())
				.setCaseInsensitiveEnumValuesAllowed(true)
				.execute(args);
		System.exit(exitCode);
	}
}
