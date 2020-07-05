/*
 * Copyright 2016-2020 DiffPlug
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

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.LineEnding;

public abstract class SpotlessExtension {
	final Project project;

	protected static final String TASK_GROUP = "Verification";
	protected static final String CHECK_DESCRIPTION = "Checks that sourcecode satisfies formatting steps.";
	protected static final String APPLY_DESCRIPTION = "Applies code formatting steps to sourcecode in-place.";

	static final String EXTENSION = "spotless";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";
	static final String DIAGNOSE = "Diagnose";

	protected SpotlessExtension(Project project) {
		this.project = requireNonNull(project);
	}

	abstract RegisterDependenciesTask getRegisterDependenciesTask();

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

	private @Nullable String ratchetFrom;

	/**
	 * Limits the target to only the files which have changed since the given git reference,
	 * which is resolved according to [this](https://javadoc.io/static/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-)
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

	/** Configures the special typescript-specific extension for typescript files. */
	public void typescript(Action<TypescriptExtension> closure) {
		format(TypescriptExtension.NAME, TypescriptExtension.class, closure);
	}

	/** Configures the special antlr4-specific extension for antlr4 files. */
	public void antlr4(Action<Antlr4Extension> closure) {
		format(Antlr4Extension.NAME, Antlr4Extension.class, closure);
	}

	/** Configures a custom extension. */
	public void format(String name, Action<FormatExtension> closure) {
		requireNonNull(name, "name");
		requireNonNull(closure, "closure");
		format(name, FormatExtension.class, closure);
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

	@SuppressWarnings("unchecked")
	public <T extends FormatExtension> void format(String name, Class<T> clazz, Action<T> configure) {
		maybeCreate(name, clazz).lazyActions.add((Action<FormatExtension>) configure);
	}

	@SuppressWarnings("unchecked")
	protected final <T extends FormatExtension> T maybeCreate(String name, Class<T> clazz) {
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
				createFormatTasks(name, formatExtension);
				return formatExtension;
			} catch (NoSuchMethodException e) {
				throw new GradleException("Must have a constructor " + clazz.getSimpleName() + "(SpotlessExtension root)", e);
			} catch (Exception e) {
				throw Errors.asRuntime(e);
			}
		}
	}

	protected abstract void createFormatTasks(String name, FormatExtension formatExtension);
}
