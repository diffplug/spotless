package org.gradle.api.plugins.format

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue


class FormatPluginTest {

    @Test
    public void 'format task is created'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.apply plugin: 'format'

        assertTrue(project.tasks.format instanceof FormatTask)
    }

    @Test(expected = GradleException)
    public void 'java plugin is needed'() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'format'
    }

}
