/*
 * Copyright 2015-2020 DiffPlug
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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Throwing;
import com.diffplug.spotless.Formatter;

@CacheableTask
public class SpotlessTaskModern extends SpotlessTask {
	@TaskAction
	public void performAction(IncrementalTaskInputs inputs) throws Exception {
		// TODO: implement using the InputChanges api

		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> target'");
		}

		if (!inputs.isIncremental()) {
			getLogger().info("Not incremental: removing prior outputs");
			getProject().delete(outputDirectory);
			Files.createDirectories(outputDirectory.toPath());
		}

		Throwing.Specific.Predicate<File, IOException> shouldInclude;
		if (this.filePatterns.isEmpty()) {
			shouldInclude = file -> true;
		} else {
			Preconditions.checkArgument(ratchet == null,
					"Cannot use 'ratchetFrom' and '-PspotlessFiles' at the same time");

			// a list of files has been passed in via project property
			final String[] includePatterns = this.filePatterns.split(",");
			final List<Pattern> compiledIncludePatterns = Arrays.stream(includePatterns)
					.map(Pattern::compile)
					.collect(Collectors.toList());
			shouldInclude = file -> compiledIncludePatterns
					.stream()
					.anyMatch(filePattern -> filePattern.matcher(file.getAbsolutePath())
							.matches());
		}

		try (Formatter formatter = buildFormatter()) {
			inputs.outOfDate(inputDetails -> {
				File input = inputDetails.getFile();
				try {
					if (shouldInclude.test(input) && input.isFile()) {
						processInputFile(formatter, input);
					}
				} catch (IOException e) {
					throw Errors.asRuntime(e);
				}
			});
		}

		inputs.removed(removedDetails -> {
			File input = removedDetails.getFile();
			try {
				if (shouldInclude.test(input)) {
					deletePreviousResult(input);
				}
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		});
	}
}
