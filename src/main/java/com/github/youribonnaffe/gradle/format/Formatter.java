package com.github.youribonnaffe.gradle.format;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/** Formatter which performs the full formatting. */
public class Formatter {
	private final LineEnding lineEnding;
	private final Path rootDir;
	private final List<FormatterStep> steps;
	private final Logger logger = Logging.getLogger(Formatter.class);

	public Formatter(LineEnding lineEnding, Path rootDir, FormatterStep... steps) {
		this.lineEnding = lineEnding;
		this.rootDir = rootDir;
		this.steps = Arrays.asList(steps);
	}

	/** Returns true iff the given file's formatting is up-to-date. */
	public boolean isClean(File file) throws IOException {
		String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		String unix = raw.replaceAll("\r", "");

		// check the newlines
		int totalNewLines = (int) unix.codePoints().filter(val -> val == '\n').count();
		int windowsNewLines = raw.length() - unix.length();
		if (lineEnding.isWin()) {
			if (windowsNewLines != totalNewLines) {
				return false;
			}
		} else {
			if (windowsNewLines != 0) {
				return false;
			}
		}

		// check the format
		for (FormatterStep step : steps) {
			try {
				if (!step.isClean(unix)) {
					return false;
				}
			} catch (Exception e) {
				logger.warn("Unable to check format " + rootDir.relativize(file.toPath()).toString() + ": " + e.getMessage());
			}
		}

		// it passed all the tests, so we're good!
		return true;
	}

	/** Applies formatting to the given file. */
	public void applyFormat(File file) throws IOException {
		String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		String unix = raw.replaceAll("\r", "");

		// enforce the format
		for (FormatterStep step : steps) {
			try {
				unix = step.format(unix);
			} catch (Exception e) {
				logger.warn("Unable to apply format " + rootDir.relativize(file.toPath()).toString() + ": " + e.getMessage());
			}
		}

		// convert the line endings
		if (!lineEnding.string.equals("\n")) {
			unix = unix.replace("\n", lineEnding.string);
		}

		// write out the file
		Files.write(file.toPath(), unix.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
	}
}
