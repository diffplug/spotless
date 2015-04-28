package com.diffplug.gradle.spotless;

import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class SpotlessPlugin implements Plugin<Project> {
	Project project;
	SpotlessRootExtension extension;

	static final String EXTENSION = "spotless";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";

	Task rootCheckTask;
	Task rootApplyTask;

	public void apply(Project project) {
		this.project = project;

		// create a root task to run all the checks and applications
		rootCheckTask = project.task(EXTENSION + CHECK);
		rootApplyTask = project.task(EXTENSION + APPLY);

		// setup the extension
		extension = project.getExtensions().create(EXTENSION, SpotlessRootExtension.class);

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
		for (Map.Entry<String, SpotlessExtension> entry : extension.extensions.entrySet()) {
			FormatTask checkTask = project.getTasks().create(EXTENSION + capitalize(entry.getKey()) + CHECK, FormatTask.class);
			FormatTask applyTask = project.getTasks().create(EXTENSION + capitalize(entry.getKey()) + APPLY, FormatTask.class);
			checkTask.lineEndings = extension.lineEndings;
			applyTask.lineEndings = extension.lineEndings;
			checkTask.check = true;
			applyTask.check = false;
			// sets toFormat and steps
			entry.getValue().setupTask(checkTask);
			entry.getValue().setupTask(applyTask);

			rootCheckTask.dependsOn(checkTask);
			rootApplyTask.dependsOn(applyTask);
		}
	}

	private static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}
