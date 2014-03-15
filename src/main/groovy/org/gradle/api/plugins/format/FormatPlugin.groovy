package org.gradle.api.plugins.format

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class FormatPlugin implements Plugin<Project> {
    void apply(Project project) {
        if (!project.plugins.hasPlugin(JavaPlugin)) {
            throw new GradleException("Java plugin must be applied (apply plugin: 'java')")
        }
        project.task('format', type: FormatTask, description: 'Formats Java source code')
    }
}