package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

public class FormatTask extends DefaultTask {
	@InputFiles
	public Iterable<File> toFormat;
	@Input
	public boolean check = false;
	@Input
	public LineEnding lineEndings = LineEnding.PLATFORM_NATIVE;
	@Input
	public List<FormatterStep> steps = new ArrayList<>();

	@TaskAction
	public void format() throws Exception {
		if (toFormat == null) {
			throw new GradleException("You must specify 'Iterable<File> toFormat'");
		}
		// combine them into the master formatter
		Formatter formatter = new Formatter(lineEndings, getProject().getRootDir().toPath(), steps);

		// perform the check
		if (check) {
			formatCheck(formatter);
		} else {
			formatApply(formatter);
		}
	}

	/** Checks the format. */
	private void formatCheck(Formatter formatter) throws IOException {
		List<File> problemFiles = new ArrayList<>();
		for (File file : toFormat) {
			getLogger().info("Checking format on " + file);
			// keep track of the problem toFormat
			if (!formatter.isClean(file)) {
				problemFiles.add(file);
			}
		}
		if (!problemFiles.isEmpty()) {
			Path rootDir = getProject().getRootDir().toPath();
			throw new GradleException("Format violations were found. Run 'gradlew " + SpotlessPlugin.EXTENSION + SpotlessPlugin.APPLY + "' to fix them.\n"
					+ problemFiles.stream().map(file -> "    " + rootDir.relativize(file.toPath()).toString())
							.collect(Collectors.joining("\n")));
		}
	}

	/** Applies the format. */
	private void formatApply(Formatter formatter) throws IOException {
		for (File file : toFormat) {
			getLogger().info("Applying format to " + file);
			// keep track of the problem toFormat
			formatter.applyFormat(file);
		}
	}
}
