package com.github.youribonnaffe.gradle.format

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertTrue


class FormatPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    private Project project

    @Before
    public void createProject() {
        project = ProjectBuilder.builder().build()
        this.project.apply plugin: 'com.github.youribonnaffe.gradle.format'
    }

    @Test
    public void 'format task is created'() {
        assertTrue(project.tasks.format instanceof FormatTask)
    }

    @Test
    public void 'load properties settings'() {
        FormatTask task = project.tasks.format as FormatTask

        task.configurationFile = classpathResourceToFile("formatter.properties")

        def sourceFile = classpathResourceToFile("JavaCodeUnformatted.java")
        task.files = project.files(sourceFile)

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeFormatted.java").text
    }

    @Test
    public void 'load XML settings'() {
        FormatTask task = project.tasks.format as FormatTask

        task.configurationFile = classpathResourceToFile("formatter.xml")

        def sourceFile = classpathResourceToFile("JavaCodeUnformatted.java")
        task.files = project.files(sourceFile)

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeFormatted.java").text
    }

    @Test(expected = GradleException)
    public void 'load unknown settings'() {
        FormatTask task = project.tasks.format as FormatTask

        task.configurationFile = folder.newFile("formatter.unknown")

        task.format()
    }

    @Test
    public void 'load null settings'() {
        FormatTask task = project.tasks.format as FormatTask

        def sourceFile = classpathResourceToFile("JavaCodeUnformatted.java")
        task.files = project.files(sourceFile)

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeFormattedDefaultSettings.java").text
    }

    @Test
    public void 'sort imports'() {
        FormatTask task = project.tasks.format as FormatTask

        def sourceFile = classpathResourceToFile("JavaCodeUnsortedImports.java")
        task.files = project.files(sourceFile)
        task.importsOrder = ["java", "javax", "org", "\\#com"]

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeSortedImports.java").text
    }

    @Test
    public void 'sort imports reading Eclipse file'() {
        FormatTask task = project.tasks.format as FormatTask

        def sourceFile = classpathResourceToFile("JavaCodeUnsortedImports.java")
        task.files = project.files(sourceFile)
        task.importsOrderConfigurationFile = classpathResourceToFile('import.properties')

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeSortedImports.java").text
    }

    @Test
    public void 'sort imports and format code'() {
        FormatTask task = project.tasks.format as FormatTask

        def sourceFile = classpathResourceToFile("JavaUnsortedImportsAndCodeUnformatted.java")
        task.importsOrder = ["java", "javax", "org", "\\#com"]
        task.configurationFile = classpathResourceToFile("formatter.properties")
        task.files = project.files(sourceFile)

        task.format()

        assert sourceFile.text == getClass().getResourceAsStream("/JavaCodeSortedImportsCodeFormatted.java").text
    }

    private File classpathResourceToFile(String filename) {
        def file = folder.newFile(filename)
        file.write(getClass().getResourceAsStream("/" + filename).text)
        file
    }

}
