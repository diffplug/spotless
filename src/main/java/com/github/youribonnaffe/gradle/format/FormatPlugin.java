package com.github.youribonnaffe.gradle.format;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

public class FormatPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
			project.getPlugins().apply(JavaPlugin.class);
		}
		project.getTasks().create("format", FormatTask.class);
	}
}
