package com.diffplug.gradle.spotless;

import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;

public class SpotlessPlugin implements Plugin<Project> {
	Project project;
	SpotlessExtension extension;

	static final String EXTENSION = "spotless";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";

	public void apply(Project project) {
		this.project = project;

		// setup the extension
		extension = project.getExtensions().create(EXTENSION, SpotlessExtension.class, project);
		// ExtensionContainer container = ((ExtensionAware) project.getExtensions().getByName(EXTENSION)).getExtensions();

		// after the project has been evaluated, configure the check and format tasks per source set
		project.afterEvaluate(unused -> {
			try {
				createTasks();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} );
	}

	private void createTasks() throws Exception {
		Task rootCheckTask = project.task(EXTENSION + CHECK);
		Task rootApplyTask = project.task(EXTENSION + APPLY);

		for (Map.Entry<String, FormatExtension> entry : extension.formats.entrySet()) {
			rootCheckTask.dependsOn(createTask(entry.getKey(), entry.getValue(), true));
			rootApplyTask.dependsOn(createTask(entry.getKey(), entry.getValue(), false));
		}
		
		// add the check task as a dependency to the global check task (if there is one)
		Task checkTask = project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME);
		if (checkTask != null) {
			checkTask.dependsOn(rootCheckTask);
		}
	}

	private FormatTask createTask(String name, FormatExtension subExtension, boolean check) throws Exception {
		FormatTask task = project.getTasks().create(EXTENSION + capitalize(name) + (check ? CHECK : APPLY), FormatTask.class);
		task.lineEndings = extension.lineEndings;
		task.check = check;
		// sets toFormat and steps
		subExtension.setupTask(task);
		return task;
	}

	private static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}
