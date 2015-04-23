package com.github.youribonnaffe.gradle.format;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

public class FormatTask extends DefaultTask {
	public FormatTask() {
		JavaPluginConvention java = getProject().getConvention().getPlugin(JavaPluginConvention.class);
		SourceSet main = java.getSourceSets().getByName("main");
		SourceSet test = java.getSourceSets().getByName("test");
		if (main != null && test != null) {
			files = new UnionFileCollection(main.getJava(), test.getJava());
		}
	}

	public FileCollection files;
	public File configurationFile;
	public List<String> importsOrder;
	public File importsOrderConfigurationFile;
	public EclipseFormatter.LineEnding lineEndings = EclipseFormatter.LineEnding.PLATFORM_NATIVE;

	@TaskAction
	void format() throws Exception {
		// load the Eclipse Formatter
		Properties settings = loadSettings();
		EclipseFormatter eclipseFormatter = new EclipseFormatter(settings, lineEndings);
		// load the import sorter
		ImportSorter importSorter = loadImportSorter();

		// create the formatter
		Formatter formatter = new Formatter(eclipseFormatter, importSorter);
		for (File file : files) {
			getLogger().info("Formatting " + file);
			formatter.applyFormat(file);
		}
	}

	/** Loads the ImportSorter. */
	private ImportSorter loadImportSorter() throws Exception {
		// if the user provided both, make her pick
		if (importsOrder != null && importsOrderConfigurationFile != null) {
			throw new IllegalArgumentException("Can't specify both importsOrder and importsOrderConfigurationFile");
		}

		// return the sorter
		if (importsOrder != null) {
			getLogger().info("Imports order: " + importsOrder);
			return new ImportSorter(importsOrder);
		} else if (importsOrderConfigurationFile != null) {
			getLogger().info("Imports order file: " + importsOrderConfigurationFile);
			return new ImportSorter(importsOrderConfigurationFile);
		} else {
			importsOrder = Arrays.asList("java", "javax", "org");
			getLogger().info("Imports default order: " + importsOrder);
			return new ImportSorter(importsOrder);
		}
	}

	/** Loads the settings for the Eclipse formatter. */
	private Properties loadSettings() throws Exception {
		if (configurationFile == null) {
			getLogger().info("Formatting default configuration");
			return null;
		} else if (configurationFile.getName().endsWith(".properties")) {
			getLogger().info("Formatting using configuration file $configurationFile");
			return loadPropertiesSettings();
		} else if (configurationFile.getName().endsWith(".xml")) {
			getLogger().info("Formatting using configuration file $configurationFile");
			return loadXmlSettings();
		} else {
			throw new GradleException("Configuration should be .xml or .properties file");
		}
	}

	private Properties loadPropertiesSettings() throws IOException {
		Properties settings = new Properties();
		settings.load(new FileInputStream(configurationFile));
		return settings;
	}

	private Properties loadXmlSettings() throws Exception {
		Properties settings = new Properties();
		Node xmlSettings = new XmlParser().parse(configurationFile);
		NodeList xmlSettingsElements = xmlSettings.getAt(new QName("profile")).getAt("setting");
		for (int i = 0; i < xmlSettingsElements.size(); ++i) {
			Node setting = (Node) xmlSettingsElements.get(i);
			settings.put(setting.attributes().get("id"), setting.attributes().get("value"));
		}
		return settings;
	}
}
