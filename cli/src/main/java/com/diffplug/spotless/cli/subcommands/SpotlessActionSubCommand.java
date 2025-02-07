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

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.xml.transform.Result;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.cli.SpotlessCLI;
import com.diffplug.spotless.cli.core.TargetResolver;
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

	@CommandLine.ParentCommand
	SpotlessCLI parent;

	@Override
	public Integer executeSpotlessAction(@Nonnull List<FormatterStep> formatterSteps) {
		TargetResolver targetResolver = new TargetResolver(parent.targets);

		try (Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(parent.lineEnding.createPolicy())
				.encoding(parent.encoding)
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
			System.out.println("Hello " + getClass().getSimpleName() + ", abc! Files: " + new TargetResolver(parent.targets).resolveTargets().collect(Collectors.toList()));
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
