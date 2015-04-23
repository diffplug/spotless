package com.github.youribonnaffe.gradle.format;

import java.util.Collections;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;

public class FormatPlugin implements Plugin<Project> {
	private static Logger logger = Logging.getLogger(FormatPlugin.class);

	Project project;
	FormatExtension extension;

	static final String EXTENSION = "format";
	static final String TASK_CHECK = "formatCheck";
	static final String TASK_APPLY = "formatApply";

	Task rootCheckTask;
	Task rootFormatTask;

	public void apply(Project project) {
		this.project = project;
		project.getPlugins().apply(JavaPlugin.class);

		// create a root task to run all the checks and applications
		rootCheckTask = project.task(TASK_CHECK);
		rootFormatTask = project.task(TASK_APPLY);

		// setup the extension
		extension = createExtension();

		// by default, the extension will apply to all Java sets
		JavaPluginConvention java = project.getConvention().getPlugin(JavaPluginConvention.class);
		extension.setSourceSets(java.getSourceSets());
		logger.info("Adding license extension rule");

		// after the project has been evaluated, configure the check and format tasks per source set
		project.afterEvaluate(unused -> {
			configureSourceSetRule();
		} );

		// whenever a FormatTask is created, set its defaults
		project.getTasks().withType(FormatTask.class, task -> {
			configureTaskDefaults(task);
		} );
	}

	/** Create a formatCheck and formatApply for each sourceSet. */
	private void configureSourceSetRule() {
		JavaPluginConvention java = project.getConvention().getPlugin(JavaPluginConvention.class);
		java.getSourceSets().forEach(sourceSet -> {
			logger.info("Adding license tasks for sourceSet " + sourceSet.getName());

			FormatTask checkTask = project.getTasks().create(sourceSet.getTaskName(TASK_CHECK, null), FormatTask.class);
			checkTask.justCheck = true;
			checkTask.files = sourceSet.getJava();
			checkTask.setDescription("Checking format on " + sourceSet.getName() + " files.");
			rootCheckTask.dependsOn(checkTask);

			FormatTask applyTask = project.getTasks().create(sourceSet.getTaskName(TASK_APPLY, null), FormatTask.class);
			applyTask.justCheck = false;
			applyTask.files = sourceSet.getJava();
			applyTask.setDescription("Applying format on " + sourceSet.getName() + " files.");
			rootFormatTask.dependsOn(applyTask);
		} );

		// add the check task as a dependency to the global check task
		project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(rootCheckTask);
	}

	/** Creates the LicenseExtension. */
	private FormatExtension createExtension() {
		FormatExtension licenseExtension = project.getExtensions().create(EXTENSION, FormatExtension.class);
		licenseExtension.setSourceSets(Collections.emptyList());
		return licenseExtension;
	}

	/** Set the task's properties from the extension. */
	private void configureTaskDefaults(FormatTask task) {
		task.licenseHeader = extension.licenseHeader;
		task.licenseHeaderFile = extension.licenseHeaderFile;

		task.importsOrder = extension.importsOrder;
		task.importsOrderFile = extension.importsOrderFile;

		task.eclipseFormatFile = extension.eclipseFormatFile;
		task.lineEndings = extension.lineEndings;
	}
}
