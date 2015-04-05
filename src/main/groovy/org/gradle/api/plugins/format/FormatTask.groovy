package org.gradle.api.plugins.format;

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction
import org.hibernate.tool.ide.formatting.JavaFormatter;

public class FormatTask extends DefaultTask {
    def FileCollection files = project.sourceSets.main.java + project.sourceSets.test.java
    def File configurationFile
    def List<String> importsOrder

    @TaskAction
    void format() {
        Properties settings = loadSettings()
        def formatter = new JavaFormatter(settings)

        files.each { file ->
            logger.info "Formating $file"
            formatter.formatFile(file)
        }

        if (importsOrder) {
            def importSorter = new ImportSorterAdapter(importsOrder)
            files.each { file ->
                logger.info "Ordering imports for $file"
                def sortedImportsText = importSorter.sortImports(file.text)
                file.write(sortedImportsText)
            }
        }
    }

    private Properties loadSettings() {
        if (!configurationFile) {
            logger.info "Formatting default configuration"
            return null
        } else if (configurationFile.name.endsWith(".properties")) {
            logger.info "Formatting using configuration file $configurationFile"
            return loadPropertiesSettings()
        } else if (configurationFile.name.endsWith(".xml")) {
            logger.info "Formatting using configuration file $configurationFile"
            return loadXmlSettings()
        } else {
            throw new GradleException("Configuration should be .xml or .properties file")
        }
    }

    private Properties loadPropertiesSettings() {
        Properties settings = new Properties();
        settings.load(configurationFile.newInputStream());
        return settings
    }

    private Properties loadXmlSettings() {
        Properties settings = new Properties();

        def xmlSettings = new XmlParser().parse(configurationFile)
        xmlSettings.profile.setting.each {
            settings.setProperty(it.@id, it.@value)
        }
        return settings
    }

}
