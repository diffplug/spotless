/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.java;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Uses java-parser, but only to remove unused imports. */
public class JavaparserRemoveUnusedImportsStep {
	private static final String MAVEN_COORDINATE = "com.github.javaparser:javaparser-core";

	// prevent direct instantiation
	private JavaparserRemoveUnusedImportsStep() {}

	static final String NAME = "removeUnusedImports_javaParser";

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultGroupArtifact(), JavaparserRemoveUnusedImportsStep.defaultVersion(), provisioner);
	}

	/** Creates a step which apply selected CleanThat mutators. */
	public static FormatterStep create(String groupArtifact,
			String version,
			Provisioner provisioner) {
		Objects.requireNonNull(groupArtifact, "groupArtifact");
		if (groupArtifact.chars().filter(ch -> ch == ':').count() != 1) {
			throw new IllegalArgumentException("groupArtifact must be in the form 'groupId:artifactId'. It was: " + groupArtifact);
		}
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new JavaParserUnusedImportsState(groupArtifact, version, provisioner),
				JavaparserRemoveUnusedImportsStep.JavaParserUnusedImportsState::createFormat);
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return "3.25.0";
	}

	public static String defaultGroupArtifact() {
		return MAVEN_COORDINATE;
	}

	static final class JavaParserUnusedImportsState implements Serializable {
		private static final long serialVersionUID = 1L;

		final JarState jarState;

		JavaParserUnusedImportsState(String version, Provisioner provisioner) throws IOException {
			this(MAVEN_COORDINATE, version, provisioner);
		}

		JavaParserUnusedImportsState(
				String groupArtifact,
				String version,
				Provisioner provisioner) throws IOException {
			ModuleHelper.doOpenInternalPackagesIfRequired();
			this.jarState = JarState.from(groupArtifact + ":" + version, provisioner);
		}

		@SuppressWarnings("PMD.UseProperClassLoader")
		FormatterFunc createFormat() {
			ClassLoader classLoader = jarState.getClassLoader();

			Object formatter;
			Method formatterMethod;
			try {
				Class<?> formatterClazz = classLoader.loadClass("com.diffplug.spotless.glue.java.JavaparserRemoveUnusedImportsFunc");
				Constructor<?> formatterConstructor = formatterClazz.getConstructor();

				formatter = formatterConstructor.newInstance();
				formatterMethod = formatterClazz.getMethod("apply", String.class);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Issue executing the formatter", e);
			}
			return input -> {
				return (String) formatterMethod.invoke(formatter, input);
			};
		}

	}
}
