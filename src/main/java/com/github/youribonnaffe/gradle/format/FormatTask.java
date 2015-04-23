package com.github.youribonnaffe.gradle.format;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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

	@TaskAction
	void format() throws Exception {
		Properties settings = loadSettings();
		JavaFormatter formatter = new JavaFormatter(settings);

		for (File file : files) {
			getLogger().info("Formatting " + file);
			formatter.formatFile(file);
		}

		ImportSorterAdapter importSorter = null;
		if (importsOrder != null) {
			importSorter = new ImportSorterAdapter(importsOrder);
		}
		if (importsOrderConfigurationFile != null) {
			importSorter = new ImportSorterAdapter(importsOrderConfigurationFile);
		}
		if (importSorter != null) {
			for (File file : files) {
				getLogger().info("Ordering imports for " + file);
				String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
				String sortedImportsText = importSorter.sortImports(content);
				Files.write(file.toPath(), sortedImportsText.getBytes(StandardCharsets.UTF_8),
						StandardOpenOption.TRUNCATE_EXISTING);
			}
		}
	}

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
