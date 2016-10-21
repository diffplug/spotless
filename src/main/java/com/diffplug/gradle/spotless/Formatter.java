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
import java.util.Arrays;
import java.util.List;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/** Formatter which performs the full formatting. */
public class Formatter {
	private final LineEnding.Policy lineEndingPolicy;
	private final Path projectDirectory;
	private final List<FormatterStep> steps;
	private final Logger logger = Logging.getLogger(Formatter.class);

	public Formatter(LineEnding.Policy lineEndingPolicy, Path projectDirectory, List<FormatterStep> steps) {
		this.lineEndingPolicy = lineEndingPolicy;
		this.projectDirectory = projectDirectory;
		this.steps = new ArrayList<>(steps);
	}

	/** Returns true iff the given file's formatting is up-to-date. */
	public boolean isClean(File file) throws IOException {
		String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		String unix = LineEnding.toUnix(raw);

		// check the newlines
		int totalNewLines = (int) unix.codePoints().filter(val -> val == '\n').count();
		int windowsNewLines = raw.length() - unix.length();
		if (lineEndingPolicy.isUnix(file)) {
			if (windowsNewLines != 0) {
				return false;
			}
		} else {
			if (windowsNewLines != totalNewLines) {
				return false;
			}
		}

		// check the other formats
		String formatted = applyAll(unix, file);

		// return true iff the formatted string equals the unix one
		return formatted.equals(unix);
	}

	/** Applies formatting to the given file. */
	public void applyFormat(File file) throws IOException {
		byte[] rawBytes = Files.readAllBytes(file.toPath());
		String raw = new String(rawBytes, StandardCharsets.UTF_8);
		String rawUnix = LineEnding.toUnix(raw);

		// enforce the format
		String formattedUnix = applyAll(rawUnix, file);

		// convert the line endings if necessary
		String formatted;
		String ending = lineEndingPolicy.getEndingFor(file);
		if (!ending.equals(LineEnding.UNIX.str())) {
			formatted = formattedUnix.replace("\n", ending);
		} else {
			formatted = formattedUnix;
		}

		// write out the file iff it has changed
		byte[] formattedBytes = formatted.getBytes(StandardCharsets.UTF_8);
		if (!Arrays.equals(rawBytes, formattedBytes)) {
			Files.write(file.toPath(), formattedBytes, StandardOpenOption.TRUNCATE_EXISTING);
		}
	}

	/** Returns the result of calling all of the FormatterSteps. */
	String applyAll(String unix, File file) {
		for (FormatterStep step : steps) {
			try {
				String formatted = step.format(unix, file);
				// should already be unix-only, but
				// some steps might misbehave
				unix = LineEnding.toUnix(formatted);
			} catch (Throwable e) {
				logger.warn("Unable to apply step " + step.getName() + " to " + projectDirectory.relativize(file.toPath()) + ": " + e.getMessage());
				logger.info("Exception is ", e);
			}
		}
		return unix;
	}
}
