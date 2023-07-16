/*
 * Copyright 2016-2023 DiffPlug
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import com.diffplug.spotless.LineEnding;

public abstract class SpotlessExtension {
	final Project project;
	private final RegisterDependenciesTask registerDependenciesTask;

	protected static final String TASK_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP;
	protected static final String CHECK_DESCRIPTION = "Checks that sourcecode satisfies formatting steps.";
	protected static final String APPLY_DESCRIPTION = "Applies code formatting steps to sourcecode in-place.";

	static final String EXTENSION = "spotless";
	static final String EXTENSION_PREDECLARE = "spotlessPredeclare";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";
	static final String DIAGNOSE = "Diagnose";

	protected SpotlessExtension(Project project) {
		this.project = requireNonNull(project);
		this.registerDependenciesTask = findRegisterDepsTask().get();
	}

	RegisterDependenciesTask getRegisterDependenciesTask() {
		return registerDependenciesTask;
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
	public void setEncoding(Charset charset) {
		encoding = requireNonNull(charset);
	}

	/** Sets encoding to use (defaults to UTF_8). */
	public void setEncoding(String name) {
		requireNonNull(name);
		setEncoding(Charset.forName(name));
	}

	/** Sets encoding to use (defaults to UTF_8). */
	public void encoding(Charset charset) {
		setEncoding(charset);
	}

	/** Sets encoding to use (defaults to UTF_8). */
	public void encoding(String charset) {
		setEncoding(charset);
	}

	private @Nullable String ratchetFrom;

	/**
	 * Limits the target to only the files which have changed since the given git reference,
	 * which is resolved according to <a href="https://javadoc.io/static/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-">this</a>
	 */
	public void setRatchetFrom(String ratchetFrom) {
		this.ratchetFrom = ratchetFrom;
	}

	/** @see #setRatchetFrom(String) */
	public @Nullable String getRatchetFrom() {
		return ratchetFrom;
	}

	/** @see #setRatchetFrom(String) */
	public void ratchetFrom(String ratchetFrom) {
		setRatchetFrom(ratchetFrom);
	}

	final Map<String, FormatExtension> formats = new LinkedHashMap<>();

	/** Configures the special java-specific extension. */
	public void java(Action<JavaExtension> closure) {
		requireNonNull(closure);
		format(JavaExtension.NAME, JavaExtension.class, closure);
	}

	/** Configures the special scala-specific extension. */
	public void scala(Action<ScalaExtension> closure) {
		requireNonNull(closure);
		format(ScalaExtension.NAME, ScalaExtension.class, closure);
	}

	/** Configures the special kotlin-specific extension. */
	public void kotlin(Action<KotlinExtension> closure) {
		requireNonNull(closure);
		format(KotlinExtension.NAME, KotlinExtension.class, closure);
	}

	/** Configures the special Gradle Kotlin DSL specific extension. */
	public void kotlinGradle(Action<KotlinGradleExtension> closure) {
		requireNonNull(closure);
		format(KotlinGradleExtension.NAME, KotlinGradleExtension.class, closure);
	}

	/** Configures the special freshmark-specific extension. */
	public void freshmark(Action<FreshMarkExtension> closure) {
		requireNonNull(closure);
		format(FreshMarkExtension.NAME, FreshMarkExtension.class, closure);
	}

	/** Configures the special groovy-specific extension. */
	public void groovy(Action<GroovyExtension> closure) {
		format(GroovyExtension.NAME, GroovyExtension.class, closure);
	}

	/** Configures the special groovy-specific extension for Gradle files. */
	public void groovyGradle(Action<GroovyGradleExtension> closure) {
		format(GroovyGradleExtension.NAME, GroovyGradleExtension.class, closure);
	}

	/** Configures the special sql-specific extension for SQL files. */
	public void sql(Action<SqlExtension> closure) {
		format(SqlExtension.NAME, SqlExtension.class, closure);
	}

	/** Configures the special C/C++-specific extension. */
	public void cpp(Action<CppExtension> closure) {
		format(CppExtension.NAME, CppExtension.class, closure);
	}

	/** Configures the special javascript-specific extension for javascript files. */
	public void javascript(Action<JavascriptExtension> closure) {
		format(JavascriptExtension.NAME, JavascriptExtension.class, closure);
	}

	/** Configures the special typescript-specific extension for typescript files. */
	public void typescript(Action<TypescriptExtension> closure) {
		format(TypescriptExtension.NAME, TypescriptExtension.class, closure);
	}

	/** Configures the special antlr4-specific extension for antlr4 files. */
	public void antlr4(Action<Antlr4Extension> closure) {
		format(Antlr4Extension.NAME, Antlr4Extension.class, closure);
	}

	/** Configures the special python-specific extension for python files. */
	public void python(Action<PythonExtension> closure) {
		format(PythonExtension.NAME, PythonExtension.class, closure);
	}

	/** Configures the special JSON-specific extension. */
	public void json(Action<JsonExtension> closure) {
		requireNonNull(closure);
		format(JsonExtension.NAME, JsonExtension.class, closure);
	}

	/** Configures the special protobuf-specific extension. */
	public void protobuf(Action<ProtobufExtension> closure) {
		requireNonNull(closure);
		format(ProtobufExtension.NAME, ProtobufExtension.class, closure);
  }

	/** Configures the special YAML-specific extension. */
	public void yaml(Action<YamlExtension> closure) {
		requireNonNull(closure);
		format(YamlExtension.NAME, YamlExtension.class, closure);
	}

	/** Configures the special Gherkin-specific extension. */
	public void gherkin(Action<GherkinExtension> closure) {
		requireNonNull(closure);
		format(GherkinExtension.NAME, GherkinExtension.class, closure);
	}

	/** Configures a custom extension. */
	public void format(String name, Action<FormatExtension> closure) {
		requireNonNull(name, "name");
		requireNonNull(closure, "closure");
		format(name, FormatExtension.class, closure);
	}

	boolean enforceCheck = true;

	/** Returns {@code true} if Gradle's {@code check} task should run {@code spotlessCheck}; {@code false} otherwise. */
	public boolean isEnforceCheck() {
		return enforceCheck;
	}

	/**
	 * Configures Gradle's {@code check} task to run {@code spotlessCheck} if {@code true},
	 * but to not do so if {@code false}.
	 *
	 * {@code true} by default.
	 */
	public void setEnforceCheck(boolean enforceCheck) {
		this.enforceCheck = enforceCheck;
	}

	@SuppressWarnings("unchecked")
	public <T extends FormatExtension> void format(String name, Class<T> clazz, Action<T> configure) {
		maybeCreate(name, clazz).lazyActions.add((Action<FormatExtension>) configure);
	}

	@SuppressWarnings("unchecked")
	protected final <T extends FormatExtension> T maybeCreate(String name, Class<T> clazz) {
		FormatExtension existing = formats.get(name);
		if (existing != null) {
			if (!clazz.isInstance(existing)) {
				throw new GradleException("Tried to add format named '" + name + "'" +
						" of type " + clazz + " but one has already been created of type " + existing.getClass());
			} else {
				return (T) existing;
			}
		} else {
			T formatExtension = instantiateFormatExtension(clazz);
			formats.put(name, formatExtension);
			createFormatTasks(name, formatExtension);
			return formatExtension;
		}
	}

	<T extends FormatExtension> T instantiateFormatExtension(Class<T> clazz) {
		try {
			return project.getObjects().newInstance(clazz, this);
		} catch (Exception e) {
			throw new GradleException("Must have a constructor " + clazz.getSimpleName() + "(SpotlessExtension root), annotated with @javax.inject.Inject", e);
		}
	}

	protected abstract void createFormatTasks(String name, FormatExtension formatExtension);

	TaskProvider<RegisterDependenciesTask> findRegisterDepsTask() {
		try {
			return findRegisterDepsTask(RegisterDependenciesTask.TASK_NAME);
		} catch (Exception e) {
			// in a composite build there can be multiple Spotless plugins on the classpath, and they will each try to register
			// a task on the root project with the same name. That will generate casting errors, which we can catch and try again
			// with an identity-specific identifier.
			// https://github.com/diffplug/spotless/pull/1001 for details
			return findRegisterDepsTask(RegisterDependenciesTask.TASK_NAME + System.identityHashCode(RegisterDependenciesTask.class));
		}
	}

	private TaskProvider<RegisterDependenciesTask> findRegisterDepsTask(String taskName) {
		TaskContainer rootProjectTasks = project.getRootProject().getTasks();
		if (!rootProjectTasks.getNames().contains(taskName)) {
			return rootProjectTasks.register(taskName, RegisterDependenciesTask.class, RegisterDependenciesTask::setup);
		} else {
			return rootProjectTasks.named(taskName, RegisterDependenciesTask.class);
		}
	}

	public void predeclareDepsFromBuildscript() {
		if (project.getRootProject() != project) {
			throw new GradleException("predeclareDepsFromBuildscript can only be called from the root project");
		}
		predeclare(GradleProvisioner.Policy.ROOT_BUILDSCRIPT);
	}

	public void predeclareDeps() {
		if (project.getRootProject() != project) {
			throw new GradleException("predeclareDeps can only be called from the root project");
		}
		predeclare(GradleProvisioner.Policy.ROOT_PROJECT);
	}

	protected void predeclare(GradleProvisioner.Policy policy) {
		project.getExtensions().create(SpotlessExtensionPredeclare.class, EXTENSION_PREDECLARE, SpotlessExtensionPredeclare.class, project, policy);
	}
}
