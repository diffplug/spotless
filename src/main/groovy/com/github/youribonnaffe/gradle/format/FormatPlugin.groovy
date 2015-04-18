package com.github.youribonnaffe.gradle.format

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class FormatPlugin implements Plugin<Project> {
    void apply(Project project) {
        if (!project.plugins.hasPlugin(JavaPlugin)) {
            project.plugins.apply(JavaPlugin.class)
        }
        project.task('format', type: FormatTask, description: 'Formats Java source code (style and import order)')
    }
}
