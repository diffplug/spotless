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

	public Formatter(LineEnding lineEnding, Path rootDir, FormatterStep ... steps) {
		this.lineEnding = lineEnding;
		this.rootDir = rootDir;
		this.steps = Arrays.asList(steps);
	}

	/** Reads the file into a string, canonicalized to \n. */
	private String readAsUnix(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).replace("\r", "");
	}

	/** Returns true iff the given file's formatting is up-to-date. */
	public boolean isClean(File file) throws IOException {
		String raw = readAsUnix(file);

		// check the format
		for (FormatterStep step : steps) {
			try {
				if (!step.isClean(raw)) {
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
		String content = readAsUnix(file);

		// enforce the format
		for (FormatterStep step : steps) {
			try {
				content = step.format(content);
			} catch (Exception e) {
				logger.warn("Unable to apply format " + rootDir.relativize(file.toPath()).toString() + ": " + e.getMessage());
			}
		}

		// convert the line endings
		if (!lineEnding.string.equals("\n")) {
			content = content.replace("\n", lineEnding.string);
		}

		// write out the file
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
	}
}
