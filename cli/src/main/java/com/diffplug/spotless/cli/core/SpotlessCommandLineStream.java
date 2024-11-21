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
package com.diffplug.spotless.cli.core;

import java.util.stream.Stream;

import com.diffplug.spotless.cli.SpotlessAction;
import com.diffplug.spotless.cli.SpotlessActionContextProvider;
import com.diffplug.spotless.cli.steps.SpotlessCLIFormatterStep;

import picocli.CommandLine;

public interface SpotlessCommandLineStream { // todo turn into an interface

	static SpotlessCommandLineStream of(CommandLine.ParseResult parseResult) {
		return new DefaultSpotlessCommandLineStream(parseResult);
	}

	Stream<SpotlessCLIFormatterStep> formatterSteps();

	Stream<SpotlessActionContextProvider> contextProviders();

	Stream<SpotlessAction> actions();

	class DefaultSpotlessCommandLineStream implements SpotlessCommandLineStream {

		private final CommandLine.ParseResult parseResult;

		private DefaultSpotlessCommandLineStream(CommandLine.ParseResult parseResult) {
			this.parseResult = parseResult;
		}

		@Override
		public Stream<SpotlessCLIFormatterStep> formatterSteps() {
			return parseResult.asCommandLineList().stream()
					.map(CommandLine::getCommand)
					.filter(command -> command instanceof SpotlessCLIFormatterStep)
					.map(SpotlessCLIFormatterStep.class::cast);
		}

		@Override
		public Stream<SpotlessActionContextProvider> contextProviders() {
			return parseResult.asCommandLineList().stream()
					.map(CommandLine::getCommand)
					.filter(command -> command instanceof SpotlessActionContextProvider)
					.map(SpotlessActionContextProvider.class::cast);
		}

		@Override
		public Stream<SpotlessAction> actions() {
			return parseResult.asCommandLineList().stream()
					.map(CommandLine::getCommand)
					.filter(command -> command instanceof SpotlessAction)
					.map(SpotlessAction.class::cast);
		}
	}
}
