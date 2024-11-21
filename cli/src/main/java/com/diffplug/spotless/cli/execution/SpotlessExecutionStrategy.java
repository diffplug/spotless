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
import com.diffplug.spotless.cli.core.SpotlessActionContext;
import com.diffplug.spotless.cli.core.SpotlessCommandLineStream;

import picocli.CommandLine;

public class SpotlessExecutionStrategy implements CommandLine.IExecutionStrategy {

	public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException {
		Integer helpResult = executeHelpRequest(parseResult);
		if (helpResult != null) {
			return helpResult;
		}
		return runSpotlessActions(SpotlessCommandLineStream.of(parseResult));
	}

	private Integer runSpotlessActions(SpotlessCommandLineStream commandLineStream) {
		// 1. prepare context
		SpotlessActionContext context = provideSpotlessActionContext(commandLineStream);

		// 2. run setup (for combining steps handled as subcommands)
		List<FormatterStep> steps = prepareFormatterSteps(commandLineStream, context);

		// 3. run spotless steps
		return executeSpotlessAction(commandLineStream, steps);
	}

	private SpotlessActionContext provideSpotlessActionContext(SpotlessCommandLineStream commandLineStream) {
		return commandLineStream.contextProviders()
				.findFirst()
				.map(provider -> provider.spotlessActionContext(commandLineStream))
				.orElseThrow(() -> new IllegalStateException("No SpotlessActionContextProvider found"));
	}

	private List<FormatterStep> prepareFormatterSteps(SpotlessCommandLineStream commandLineStream, SpotlessActionContext context) {
		return commandLineStream.formatterSteps()
				.flatMap(step -> step.prepareFormatterSteps(context).stream())
				.collect(Collectors.toList());
	}

	private Integer executeSpotlessAction(SpotlessCommandLineStream commandLineStream, List<FormatterStep> steps) {
		return commandLineStream.actions()
				.findFirst()
				.map(spotlessAction -> spotlessAction.executeSpotlessAction(steps))
				.orElse(-1);
	}
}
