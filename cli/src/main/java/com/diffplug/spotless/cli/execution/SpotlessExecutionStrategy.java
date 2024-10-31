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

import java.util.function.Function;

import com.diffplug.spotless.cli.SpotlessCommand;
import com.diffplug.spotless.cli.subcommands.SpotlessActionCommand;
import com.diffplug.spotless.cli.subcommands.steps.SpotlessCLIStep;

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

		// TODO: maybe collect a list of steps and pass them to the spotless action in step 2?
		Integer prepareResult = runSpotlessRecursive(parseResult, this::prepareStep);
		if (prepareResult != null) {
			return prepareResult;
		}
		// 2. run spotless steps
		return runSpotlessRecursive(parseResult, this::executeSpotlessAction);
	}

	private Integer runSpotlessRecursive(CommandLine.ParseResult parseResult, Function<SpotlessCommand, Integer> action) {
		SpotlessCommand spotlessCommand = parseResult.commandSpec().commandLine().getCommand();
		Integer result = action.apply(spotlessCommand);
		if (result != null) {
			return result;
		}
		for (CommandLine.ParseResult subCommand : parseResult.subcommands()) {
			Integer subResult = runSpotlessRecursive(subCommand, action);
			if (subResult != null) {
				return subResult;
			}
		}
		return null;
	}

	private Integer prepareStep(SpotlessCommand spotlessCommand) {
		if (spotlessCommand instanceof SpotlessCLIStep) {
			((SpotlessCLIStep) spotlessCommand).prepare();
		}
		return null;
	}

	private Integer executeSpotlessAction(SpotlessCommand spotlessCommand) {
		if (spotlessCommand instanceof SpotlessActionCommand) {
			return ((SpotlessActionCommand) spotlessCommand).executeSpotlessAction();
		}
		return null;
	}
}
