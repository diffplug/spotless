package com.github.youribonnaffe.gradle.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

public class FormatTask extends DefaultTask {
	@InputFiles
	public FileCollection files;
	@Input
	public boolean justCheck = false;

	////////////////////
	// LICENSE HEADER //
	////////////////////
	/** Header string for the file. */
	@Input
	public String licenseHeader;
	/** Header file to be appended to the file. */
	@Optional
	@InputFile
	public File licenseHeaderFile;

	///////////////////
	// IMPORTS ORDER //
	///////////////////
	/** The imports ordering. */
	@Optional
	@Input
	public List<String> importsOrder;
	/** The imports ordering file. */
	@Optional
	@Input
	public File importsOrderFile;

	////////////////////
	// ECLIPSE FORMAT //
	////////////////////
	@Optional
	@InputFile
	public File eclipseFormatFile;
	@Input
	public LineEnding lineEndings = LineEnding.PLATFORM_NATIVE;

	@TaskAction
	void format() throws Exception {
		// load the various steps
		FormatterStep licenseEnforcer = LicenseEnforcer.load(licenseHeader, licenseHeaderFile);
		FormatterStep eclipseFormatter = EclipseFormatter.readFrom(eclipseFormatFile);
		FormatterStep importSorter = loadImportSorter();
		// combine them into the master formatter
		Formatter formatter = new Formatter(lineEndings, getProject().getRootDir().toPath(),
				licenseEnforcer, eclipseFormatter, importSorter);

		// perform the check
		if (justCheck) {
			formatCheck(formatter);
		} else {
			formatApply(formatter);
		}
	}

	/** Checks the format. */
	private void formatCheck(Formatter formatter) throws IOException {
		List<File> problemFiles = new ArrayList<>();
		for (File file : files) {
			getLogger().info("Checking format on " + file);
			// keep track of the problem files
			if (!formatter.isClean(file)) {
				problemFiles.add(file);
			}
		}
		if (!problemFiles.isEmpty()) {
			Path rootDir = getProject().getRootDir().toPath();
			throw new GradleException("Format violations were found. Run 'gradlew " + FormatPlugin.TASK_APPLY + "' to fix them.\n"
					+ problemFiles.stream().map(file -> "    " + rootDir.relativize(file.toPath()).toString())
							.collect(Collectors.joining("\n")));
		}
	}

	/** Applies the format. */
	private void formatApply(Formatter formatter) throws IOException {
		for (File file : files) {
			getLogger().info("Applying format to " + file);
			// keep track of the problem files
			formatter.applyFormat(file);
		}
	}

	/** Loads the ImportSorter. */
	private ImportSorter loadImportSorter() throws Exception {
		// if the user provided both, make her pick
		if (importsOrder != null && importsOrderFile != null) {
			throw new IllegalArgumentException("Can't specify both importsOrder and importsOrderConfigurationFile");
		}

		// return the sorter
		if (importsOrder != null) {
			return new ImportSorter(importsOrder);
		} else if (importsOrderFile != null) {
			return new ImportSorter(importsOrderFile);
		} else {
			importsOrder = Arrays.asList("java", "javax", "org");
			return new ImportSorter(importsOrder);
		}
	}
}
