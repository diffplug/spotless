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
package com.diffplug.spotless.cli.subcommands;

import java.util.List;

import javax.annotation.Nonnull;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.cli.subcommands.steps.generic.LicenseHeader;
import com.diffplug.spotless.cli.subcommands.steps.generic.RemoveMeLaterSubCommand;

import picocli.CommandLine;

// repeatable subcommands: https://picocli.info/#_repeatable_subcommands_specification

// access command spec: https://picocli.info/#spec-annotation

@CommandLine.Command(mixinStandardHelpOptions = true, subcommandsRepeatable = true, subcommands = {
		LicenseHeader.class,
		RemoveMeLaterSubCommand.class
})
public abstract class SpotlessActionSubCommand implements SpotlessActionCommand {

	@Override
	public Integer executeSpotlessAction(@Nonnull List<FormatterStep> formatterSteps) {
		System.out.println("Hello " + getClass().getSimpleName() + ", abc!");
		formatterSteps.forEach(step -> System.out.println("Step: " + step));
		return 0;
	}
}
