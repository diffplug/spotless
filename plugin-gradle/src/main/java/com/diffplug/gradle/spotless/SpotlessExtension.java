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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.LineEnding;

public class SpotlessExtension {
	final Project project;

	public SpotlessExtension(Project project) {
		this.project = requireNonNull(project);
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
				T newlyCreated = constructor.newInstance(this);
				formats.put(name, newlyCreated);
				return newlyCreated;
			} catch (NoSuchMethodException e) {
				throw new GradleException("Must have a constructor " + clazz.getSimpleName() + "(SpotlessExtension root)", e);
			} catch (Exception e) {
				throw Errors.asRuntime(e);
			}
		}
	}
}
