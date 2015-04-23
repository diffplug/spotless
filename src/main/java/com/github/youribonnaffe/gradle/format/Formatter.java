package com.github.youribonnaffe.gradle.format;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/** Formatter which performs the full formatting. */
public class Formatter {
	private EclipseFormatter formatter;
	private ImportSorter importSorter;
	private LineEnding lineEnding;
	private Logger logger = Logging.getLogger(Formatter.class);

	public Formatter(EclipseFormatter formatter, ImportSorter importSorter, LineEnding lineEnding) {
		this.formatter = formatter;
		this.importSorter = importSorter;
		this.lineEnding = lineEnding;
	}

	/** Reads the file into a string, canonicalized to \n. */
	private String readAsUnix(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).replace("\r", "");
	}

	/** Returns true iff the given file's formatting is up-to-date. */
	public boolean checkFormat(File file) throws IOException {
		String raw = readAsUnix(file);

		// check the imports
		String importOrder = importSorter.sortImports(raw);
		if (!raw.equals(importOrder)) {
			return false;
		}

		// check the java format
		try {
			if (formatter.editRequired(raw)) {
				return false;
			}
		} catch (Exception e) {
			logger.warn("Unable to check format " + file + ": " + e.getMessage());
		}

		// it passed all the tests, so we're good!
		return true;
	}

	/** Applies formatting to the given file. */
	public void applyFormat(File file) throws IOException {
		String raw = readAsUnix(file);

		// check the imports
		String importsFixed = importSorter.sortImports(raw);

		// check the java format
		String formatted = importsFixed;
		try {
			formatted = formatter.format(importsFixed);
		} catch (Exception e) {
			logger.warn("Unable to format " + file + ": " + e.getMessage());
		}

		// convert the line endings
		if (!lineEnding.string.equals("\n")) {
			formatted = formatted.replace("\n", lineEnding.string);
		}

		// write out the file
		Files.write(file.toPath(), formatted.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
	}
}
