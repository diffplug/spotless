package org.gradle.api.plugins.format;

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;

public class FormatTask extends DefaultTask {
    def FileCollection files = project.sourceSets.main.java + project.sourceSets.test.java
    def File configurationFile

    @TaskAction
    void format() {
        ant.taskdef(name: 'formatter', classname: 'org.hibernate.tool.ant.JavaFormatterTask', classpath:
                project.rootProject.buildscript.configurations.classpath.asPath)
        def parameters = [:]
        if (configurationFile) {
            parameters << ['configurationFile': configurationFile]
        }
        ant.formatter(parameters) {
            if (!files.isEmpty()) {
                fileset(dir: project.projectDir.getPath()) {
                    files.each {
                        file ->
                            println file
                            include(name: project.relativePath(file))
                    }
                }
            }
        }

    }
}
