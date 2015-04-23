package com.github.youribonnaffe.gradle.format;

import com.google.common.collect.Lists;
import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
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
	String licenseHeader = "";
	/** Header file to be appended to the file. */
	@Optional
	@InputFile
	File licenseHeaderFile;

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
		// load the Eclipse Formatter
		Properties settings = loadEclipseSettings();
		EclipseFormatter eclipseFormatter = new EclipseFormatter(settings);
		// load the import sorter
		ImportSorter importSorter = loadImportSorter();
		// combine them into the master formatter
		Formatter formatter = new Formatter(eclipseFormatter, importSorter, lineEndings);

		// create the formatter
		if (justCheck) {
			formatCheck(formatter);
		} else {
			formatApply(formatter);
		}
	}

	/** Checks the format. */
	private void formatCheck(Formatter formatter) throws IOException {
		List<File> problemFiles = Lists.newArrayList();
		for (File file : files) {
			getLogger().info("Checking format on " + file);
			// keep track of the problem files
			if (!formatter.checkFormat(file)) {
				problemFiles.add(file);
			}
		}
		if (!problemFiles.isEmpty()) {
			Path rootDir = getProject().getRootDir().toPath();
			throw new GradleException("Format violations were found. Run formatApply to fix them.\n"
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
			getLogger().info("Imports order: " + importsOrder);
			return new ImportSorter(importsOrder);
		} else if (importsOrderFile != null) {
			getLogger().info("Imports order file: " + importsOrderFile);
			return new ImportSorter(importsOrderFile);
		} else {
			importsOrder = Arrays.asList("java", "javax", "org");
			getLogger().info("Imports default order: " + importsOrder);
			return new ImportSorter(importsOrder);
		}
	}

	/** Loads the settings for the Eclipse formatter. */
	private Properties loadEclipseSettings() throws Exception {
		if (eclipseFormatFile == null) {
			getLogger().info("Formatting default configuration");
			return null;
		} else if (eclipseFormatFile.getName().endsWith(".properties")) {
			getLogger().info("Formatting using configuration file $configurationFile");
			return loadEclipseProperties();
		} else if (eclipseFormatFile.getName().endsWith(".xml")) {
			getLogger().info("Formatting using configuration file $configurationFile");
			return loadEclipseXml();
		} else {
			throw new GradleException("Configuration should be .xml or .properties file");
		}
	}

	private Properties loadEclipseProperties() throws IOException {
		Properties settings = new Properties();
		settings.load(new FileInputStream(eclipseFormatFile));
		return settings;
	}

	private Properties loadEclipseXml() throws Exception {
		Properties settings = new Properties();
		Node xmlSettings = new XmlParser().parse(eclipseFormatFile);
		NodeList xmlSettingsElements = xmlSettings.getAt(new QName("profile")).getAt("setting");
		for (int i = 0; i < xmlSettingsElements.size(); ++i) {
			Node setting = (Node) xmlSettingsElements.get(i);
			settings.put(setting.attributes().get("id"), setting.attributes().get("value"));
		}
		return settings;
	}
}
