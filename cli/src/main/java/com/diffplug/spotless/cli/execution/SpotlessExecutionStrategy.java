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
package com.diffplug.spotless.cli.execution;

import static picocli.CommandLine.executeHelpRequest;

import java.util.List;
import java.util.stream.Collectors;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.cli.subcommands.SpotlessActionCommand;
import com.diffplug.spotless.cli.subcommands.steps.SpotlessCLIFormatterStep;

import picocli.CommandLine;

public class SpotlessExecutionStrategy implements CommandLine.IExecutionStrategy {

	public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException {
		Integer helpResult = executeHelpRequest(parseResult);
		if (helpResult != null) {
			return helpResult;
		}
		return runSpotlessActions(parseResult);
	}

	private Integer runSpotlessActions(CommandLine.ParseResult parseResult) {
		// 1. run setup (for combining steps handled as subcommands)
		List<FormatterStep> steps = prepareFormatterSteps(parseResult);

		// 2. run spotless steps
		return executeSpotlessAction(parseResult, steps);
	}

	private List<FormatterStep> prepareFormatterSteps(CommandLine.ParseResult parseResult) {
		return parseResult.asCommandLineList().stream()
				.map(CommandLine::getCommand)
				.filter(command -> command instanceof SpotlessCLIFormatterStep)
				.map(SpotlessCLIFormatterStep.class::cast)
				.flatMap(step -> step.prepareFormatterSteps().stream())
				.collect(Collectors.toList());
	}

	private Integer executeSpotlessAction(CommandLine.ParseResult parseResult, List<FormatterStep> steps) {
		return parseResult.asCommandLineList().stream()
				.map(CommandLine::getCommand)
				.filter(command -> command instanceof SpotlessActionCommand)
				.map(SpotlessActionCommand.class::cast)
				.findFirst()
				.map(spotlessActionCommand -> spotlessActionCommand.executeSpotlessAction(steps))
				.orElse(-1);
	}
}
