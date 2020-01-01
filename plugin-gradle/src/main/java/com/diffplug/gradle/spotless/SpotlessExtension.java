/*
 * Copyright 2016 DiffPlug
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

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.plugins.BasePlugin;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.LineEnding;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;

public class SpotlessExtension {
	final Project project;
	final Task rootCheckTask, rootApplyTask;
	final @Nullable RegisterDependenciesTask registerDependenciesTask;

	static final String EXTENSION = "spotless";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";

	private static final String TASK_GROUP = "Verification";
	private static final String CHECK_DESCRIPTION = "Checks that sourcecode satisfies formatting steps.";
	private static final String APPLY_DESCRIPTION = "Applies code formatting steps to sourcecode in-place.";

	private static final String FILES_PROPERTY = "spotlessFiles";

	public SpotlessExtension(Project project) {
		this.project = requireNonNull(project);
		rootCheckTask = project.task(EXTENSION + CHECK);
		rootCheckTask.setGroup(TASK_GROUP);
		rootCheckTask.setDescription(CHECK_DESCRIPTION);
		rootApplyTask = project.task(EXTENSION + APPLY);
		rootApplyTask.setGroup(TASK_GROUP);
		rootApplyTask.setDescription(APPLY_DESCRIPTION);
		boolean registerDependenciesInRoot = RegisterDependenciesInRoot.isEnabled(project);
		if (registerDependenciesInRoot) {
			if (project.getRootProject() == project) {
				registerDependenciesTask = project.getTasks().create(RegisterDependenciesInRoot.TASK_NAME, RegisterDependenciesTask.class);
				registerDependenciesTask.setup();
			} else {
				registerDependenciesTask = project.getRootProject().getPlugins().apply(SpotlessPlugin.class).spotlessExtension.registerDependenciesTask;
			}
		} else {
			registerDependenciesTask = null;
		}
	}

	/** Line endings (if any). */
	LineEnding lineEndings = LineEnding.GIT_ATTRIBUTES;

	public LineEnding getLineEndings() {
		return lineEndings;
	}

	public void setLineEndings(LineEnding lineEndings) {
		this.lineEndings = requireNonNull(lineEndings);
	}

	Charset encoding = StandardCharsets.UTF_8;

	/** Returns the encoding to use. */
	public Charset getEncoding() {
		return encoding;
	}

	/** Sets encoding to use (defaults to UTF_8). */
	public void setEncoding(String name) {
		requireNonNull(name);
		setEncoding(Charset.forName(name));
	}

	/** Sets encoding to use (defaults to UTF_8). */
	public void setEncoding(Charset charset) {
		encoding = requireNonNull(charset);
	}

	/** Sets encoding to use (defaults to UTF_8). */
	public void encoding(String charset) {
		setEncoding(charset);
	}

	final Map<String, FormatExtension> formats = new LinkedHashMap<>();

	/** Configures the special java-specific extension. */
	public void java(Action<JavaExtension> closure) {
		requireNonNull(closure);
		configure(JavaExtension.NAME, JavaExtension.class, closure);
	}

	/** Configures the special scala-specific extension. */
	public void scala(Action<ScalaExtension> closure) {
		requireNonNull(closure);
		configure(ScalaExtension.NAME, ScalaExtension.class, closure);
	}

	/** Configures the special kotlin-specific extension. */
	public void kotlin(Action<KotlinExtension> closure) {
		requireNonNull(closure);
		configure(KotlinExtension.NAME, KotlinExtension.class, closure);
	}

	/** Configures the special Gradle Kotlin DSL specific extension. */
	public void kotlinGradle(Action<KotlinGradleExtension> closure) {
		requireNonNull(closure);
		configure(KotlinGradleExtension.NAME, KotlinGradleExtension.class, closure);
	}

	/** Configures the special freshmark-specific extension. */
	public void freshmark(Action<FreshMarkExtension> closure) {
		requireNonNull(closure);
		configure(FreshMarkExtension.NAME, FreshMarkExtension.class, closure);
	}

	/** Configures the special groovy-specific extension. */
	public void groovy(Action<GroovyExtension> closure) {
		configure(GroovyExtension.NAME, GroovyExtension.class, closure);
	}

	/** Configures the special groovy-specific extension for Gradle files. */
	public void groovyGradle(Action<GroovyGradleExtension> closure) {
		configure(GroovyGradleExtension.NAME, GroovyGradleExtension.class, closure);
	}

	/** Configures the special sql-specific extension for SQL files. */
	public void sql(Action<SqlExtension> closure) {
		configure(SqlExtension.NAME, SqlExtension.class, closure);
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

	/** Configures the special C/C++-specific extension. */
	public void cpp(Action<CppExtension> closure) {
		configure(CppExtension.NAME, CppExtension.class, closure);
	}

	/** Configures the special typescript-specific extension for typescript files. */
	public void typescript(Action<TypescriptExtension> closure) {
		configure(TypescriptExtension.NAME, TypescriptExtension.class, closure);
	}

	/** Configures a custom extension. */
	public void format(String name, Action<FormatExtension> closure) {
		requireNonNull(name, "name");
		requireNonNull(closure, "closure");
		configure(name, FormatExtension.class, closure);
	}

	/** Makes it possible to remove a format which was created earlier. */
	public void removeFormat(String name) {
		requireNonNull(name);
		FormatExtension toRemove = formats.remove(name);
		if (toRemove == null) {
			project.getLogger().warn("Called removeFormat('" + name + "') but there was no such format.");
		}
	}

	boolean enforceCheck = true;

	/** Returns `true` if Gradle's `check` task should run `spotlessCheck`; `false` otherwise. */
	public boolean isEnforceCheck() {
		return enforceCheck;
	}

	/**
	 * Configures Gradle's `check` task to run `spotlessCheck` if `true`,
	 * but to not do so if `false`.
	 *
	 * `true` by default.
	 */
	public void setEnforceCheck(boolean enforceCheck) {
		this.enforceCheck = enforceCheck;
	}

	private <T extends FormatExtension> void configure(String name, Class<T> clazz, Action<T> configure) {
		T value = maybeCreate(name, clazz);
		configure.execute(value);
	}

	@SuppressWarnings("unchecked")
	private <T extends FormatExtension> T maybeCreate(String name, Class<T> clazz) {
		FormatExtension existing = formats.get(name);
		if (existing != null) {
			if (!existing.getClass().equals(clazz)) {
				throw new GradleException("Tried to add format named '" + name + "'" +
						" of type " + clazz + " but one has already been created of type " + existing.getClass());
			} else {
				return (T) existing;
			}
		} else {
			try {
				Constructor<T> constructor = clazz.getConstructor(SpotlessExtension.class);
				T formatExtension = constructor.newInstance(this);
				formats.put(name, formatExtension);
				createFormatTask(name, formatExtension);
				return formatExtension;
			} catch (NoSuchMethodException e) {
				throw new GradleException("Must have a constructor " + clazz.getSimpleName() + "(SpotlessExtension root)", e);
			} catch (Exception e) {
				throw Errors.asRuntime(e);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void createFormatTask(String name, FormatExtension formatExtension) {
		// create the SpotlessTask
		String taskName = EXTENSION + SpotlessPlugin.capitalize(name);
		SpotlessTask spotlessTask = project.getTasks().create(taskName, SpotlessTask.class);
		project.afterEvaluate(unused -> formatExtension.setupTask(spotlessTask));

		// clean removes the SpotlessCache, so we have to run after clean
		Task clean = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
		spotlessTask.mustRunAfter(clean);

		// create the check and apply control tasks
		Task checkTask = project.getTasks().create(taskName + CHECK);
		Task applyTask = project.getTasks().create(taskName + APPLY);

		checkTask.dependsOn(spotlessTask);
		applyTask.dependsOn(spotlessTask);
		// when the task graph is ready, we'll configure the spotlessTask appropriately
		project.getGradle().getTaskGraph().whenReady(new Closure(null) {
			private static final long serialVersionUID = 1L;

			// called by gradle
			@SuppressFBWarnings("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS")
			public Object doCall(TaskExecutionGraph graph) {
				if (graph.hasTask(checkTask)) {
					spotlessTask.setCheck();
				}
				if (graph.hasTask(applyTask)) {
					spotlessTask.setApply();
				}
				return Closure.DONE;
			}
		});

		// set the filePatterns property
		project.afterEvaluate(unused -> {
			String filePatterns;
			if (project.hasProperty(FILES_PROPERTY) && project.property(FILES_PROPERTY) instanceof String) {
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
	}
}
