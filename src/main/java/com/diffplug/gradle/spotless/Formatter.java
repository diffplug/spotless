/*
 * Copyright 2016 DiffPlug
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/** Formatter which performs the full formatting. */
public class Formatter {
	private final Path projectDirectory;
	private final List<FormatterStep> steps;
	private final Logger logger = Logging.getLogger(Formatter.class);

	public Formatter(Path projectDirectory, List<FormatterStep> steps) {
		this.projectDirectory = projectDirectory;
		this.steps = new ArrayList<>(steps);
	}

	/** Returns true iff the given file's formatting is up-to-date. */
	public boolean isClean(File file) throws IOException {
		String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

		// check the other formats
		String formatted = applyAll(raw, file);

		// return true iff the formatted string equals raw
		return formatted.equals(raw);
	}

	/** Applies formatting to the given file. */
	public void applyFormat(File file) throws IOException {
		String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

		// enforce the format
		raw = applyAll(raw, file);

		// write out the file
		Files.write(file.toPath(), raw.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
	}

	/** Returns the result of calling all of the FormatterSteps. */
	String applyAll(String raw, File file) {
		for (FormatterStep step : steps) {
			try {
				raw = step.format(raw, file);
			} catch (Throwable e) {
				logger.warn("Unable to apply step " + step.getName() + " to " + projectDirectory.relativize(file.toPath()) + ": " + e.getMessage());
				logger.info("Exception is ", e);
			}
		}
		return raw;
	}
}
