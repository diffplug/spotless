package com.github.youribonnaffe.gradle.format

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

public class FormatTask extends DefaultTask {
    def FileCollection files = project.sourceSets.main.java + project.sourceSets.test.java
    def File configurationFile
    def List<String> importsOrder
    def File importsOrderConfigurationFile

    @TaskAction
    void format() {
        Properties settings = loadSettings()
        def formatter = new JavaFormatter(settings)

        files.each { file ->
            logger.info "Formating $file"
            formatter.formatFile(file)
        }

        def importSorter
        if (importsOrder) {
            importSorter = new ImportSorterAdapter(importsOrder)
        }
        if (importsOrderConfigurationFile) {
            importSorter = new ImportSorterAdapter(importsOrderConfigurationFile.newInputStream())
        }
        if (importSorter) {
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
