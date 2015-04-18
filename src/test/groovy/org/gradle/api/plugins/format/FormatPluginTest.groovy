package org.gradle.api.plugins.format

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertTrue


class FormatPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void 'format task is created'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: 'format'

        assertTrue(project.tasks.format instanceof FormatTask)
    }

    @Test
    public void 'load properties settings'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: 'format'
        FormatTask task = project.tasks.format as FormatTask

        task.configurationFile = classpathResourceToFile("formatter.properties")

        def sourceFile = classpathResourceToFile("JavaCodeUnformatted.java")
        task.files = project.files(sourceFile)

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeFormatted.java").text
    }

    @Test
    public void 'load XML settings'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: 'format'
        FormatTask task = project.tasks.format as FormatTask

        task.configurationFile = classpathResourceToFile("formatter.xml")

        def sourceFile = classpathResourceToFile("JavaCodeUnformatted.java")
        task.files = project.files(sourceFile)

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeFormatted.java").text
    }

    @Test(expected = GradleException)
    public void 'load unknown settings'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: 'format'
        FormatTask task = project.tasks.format as FormatTask

        task.configurationFile = folder.newFile("formatter.unknown")

        task.format()
    }

    @Test
    public void 'load null settings'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: 'format'
        FormatTask task = project.tasks.format as FormatTask

        def sourceFile = classpathResourceToFile("JavaCodeUnformatted.java")
        task.files = project.files(sourceFile)

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeFormattedDefaultSettings.java").text
    }

    @Test
    public void 'sort imports'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: 'format'
        FormatTask task = project.tasks.format as FormatTask

        def sourceFile = classpathResourceToFile("JavaCodeUnsortedImports.java")
        task.files = project.files(sourceFile)
        task.importsOrder = ["java", "javax", "org", "\\#com"]

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeSortedImports.java").text
    }

    @Test
    public void 'sort imports reading Eclipse file'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: 'format'
        FormatTask task = project.tasks.format as FormatTask

        def sourceFile = classpathResourceToFile("JavaCodeUnsortedImports.java")
        task.files = project.files(sourceFile)
        task.importsOrderConfigurationFile = classpathResourceToFile('import.properties')

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeSortedImports.java").text
    }

    private File classpathResourceToFile(String filename) {
        def file = folder.newFile(filename)
        file.write(getClass().getResourceAsStream("/" + filename).text)
        file
    }

}
