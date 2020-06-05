/*
 * Copyright 2015-2020 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.spotless;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;

public class SpotlessExtension extends SpotlessExtensionBase {
	final Task rootCheckTask, rootApplyTask, rootDiagnoseTask;
	private static final String FILES_PROPERTY = "spotlessFiles";

	public SpotlessExtension(Project project) {
		super(project);
		rootCheckTask = project.task(EXTENSION + CHECK);
		rootCheckTask.setGroup(TASK_GROUP);
		rootCheckTask.setDescription(CHECK_DESCRIPTION);
		rootApplyTask = project.task(EXTENSION + APPLY);
		rootApplyTask.setGroup(TASK_GROUP);
		rootApplyTask.setDescription(APPLY_DESCRIPTION);
		rootDiagnoseTask = project.task(EXTENSION + DIAGNOSE);
		rootDiagnoseTask.setGroup(TASK_GROUP);	// no description on purpose
	}

	/**
	 * Configures the special css-specific extension for CSS files.
	 * <br/>
	 * The CSS extension is discontinued. CSS formatters are now part of
	 * the generic {@link FormatExtension}.
	 */
	@Deprecated
	public void css(Action<CssExtension> closure) {
		configure(CssExtension.NAME, CssExtension.class, closure);
	}

	/**
	 * Configures the special xml-specific extension for XML/XSL/... files (XHTML is excluded).
	 * <br/>
	 * The XML extension is discontinued. XML formatters are now part of
	 * the generic {@link FormatExtension}.
	 */
	@Deprecated
	public void xml(Action<XmlExtension> closure) {
		configure(XmlExtension.NAME, XmlExtension.class, closure);
	}

	/**
	 * Creates 3 tasks for the supplied format:
	 * - "spotless{FormatName}" is the main `SpotlessTask` that does the work for this format
	 * - "spotless{FormatName}Check" will depend on the main spotless task in `check` mode
	 * - "spotless{FormatName}Apply" will depend on the main spotless task in `apply` mode
	 */
	protected void createFormatTasks(String name, FormatExtension formatExtension) {
		// create the SpotlessTask
		String taskName = EXTENSION + SpotlessPlugin.capitalize(name);
		SpotlessTask spotlessTask = project.getTasks().create(taskName, SpotlessTask.class);
		project.afterEvaluate(unused -> formatExtension.setupTask(spotlessTask));

		// clean removes the SpotlessCache, so we have to run after clean
		Task clean = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
		spotlessTask.mustRunAfter(clean);

		// create the check and apply control tasks
		SpotlessCheck checkTask = project.getTasks().create(taskName + CHECK, SpotlessCheck.class);
		checkTask.setSpotlessOutDirectory(spotlessTask.getOutputDirectory());
		checkTask.source = spotlessTask;
		checkTask.dependsOn(spotlessTask);

		SpotlessApply applyTask = project.getTasks().create(taskName + APPLY, SpotlessApply.class);
		applyTask.setSpotlessOutDirectory(spotlessTask.getOutputDirectory());
		applyTask.linkSource(spotlessTask);
		applyTask.dependsOn(spotlessTask);

		// if the user runs both, make sure that apply happens first,
		checkTask.mustRunAfter(applyTask);

		// set the filePatterns property
		project.afterEvaluate(unused -> {
			String filePatterns;
			if (project.hasProperty(FILES_PROPERTY) && project.property(FILES_PROPERTY) instanceof String) {
				System.err.println("Spotless with -P" + FILES_PROPERTY + " has been deprecated and will be removed. It is slow and error-prone, especially for win/unix cross-platform, and we have better options available now:");
				System.err.println("  If you are formatting just one file, try the much faster IDE hook: https://github.com/diffplug/spotless/blob/master/plugin-gradle/IDE_HOOK.md");
				System.err.println("  If you are integrating with git, try `ratchetFrom 'origin/master'`: https://github.com/diffplug/spotless/tree/master/plugin-gradle#ratchet");
				System.err.println("  If neither of these work for you, please let us know in this PR: https://github.com/diffplug/spotless/pull/602");
				filePatterns = (String) project.property(FILES_PROPERTY);
			} else {
				// needs to be non-null since it is an @Input property of the task
				filePatterns = "";
			}
			spotlessTask.setFilePatterns(filePatterns);
		});

		// the root tasks depend on the control tasks
		rootCheckTask.dependsOn(checkTask);
		rootApplyTask.dependsOn(applyTask);

		// create the diagnose task
		SpotlessDiagnoseTask diagnoseTask = project.getTasks().create(taskName + DIAGNOSE, SpotlessDiagnoseTask.class);
		diagnoseTask.source = spotlessTask;
		rootDiagnoseTask.dependsOn(diagnoseTask);
		diagnoseTask.mustRunAfter(clean);

		if (project.hasProperty(IdeHook.PROPERTY)) {
			// disable the normal tasks, to disable their up-to-date checking
			spotlessTask.setEnabled(false);
			checkTask.setEnabled(false);
			applyTask.setEnabled(false);
			// the rootApplyTask is no longer just a marker task, now it does a bit of work itself
			rootApplyTask.doLast(unused -> IdeHook.performHook(spotlessTask));
		}
	}
}
