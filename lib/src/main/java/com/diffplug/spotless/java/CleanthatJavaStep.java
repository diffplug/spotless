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
import java.util.List;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;

/**
 * Enables CleanThat as a SpotLess step.
 *
 * @author Benoit Lacelle
 */
// https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#how-to-add-a-new-formatterstep
public final class CleanthatJavaStep {

	private static final String NAME = "cleanthat";
	private static final String MAVEN_COORDINATE = "io.github.solven-eu.cleanthat:java";

	// CleanThat changelog is available at https://github.com/solven-eu/cleanthat/blob/master/CHANGES.MD
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(11, "2.3");

	// prevent direct instantiation
	private CleanthatJavaStep() {}

	/** Creates a step which apply default CleanThat mutators. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which apply default CleanThat mutators. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(MAVEN_COORDINATE, version, defaultSourceJdk(), defaultMutators(), defaultExcludedMutators(), defaultIncludeDraft(), provisioner);
	}

	public static String defaultSourceJdk() {
		// see IJdkVersionConstants.JDK_7
		// https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#source
		// 1.7 is the default for 'maven-compiler-plugin' since 3.9.0
		return "1.7";
	}

	/**
	 * By default, we include only safe and consensual mutators
	 * @return
	 */
	public static List<String> defaultMutators() {
		return List.of("SafeAndConsensualMutators");
	}

	public static List<String> defaultExcludedMutators() {
		return List.of();
	}

	public static boolean defaultIncludeDraft() {
		return false;
	}

	/** Creates a step which apply selected CleanThat mutators. */
	public static FormatterStep create(String groupArtifact,
			String version,
			String sourceJdkVersion,
			List<String> excluded,
			List<String> included,
			boolean includeDraft,
			Provisioner provisioner) {
		Objects.requireNonNull(groupArtifact, "groupArtifact");
		if (groupArtifact.chars().filter(ch -> ch == ':').count() != 1) {
			throw new IllegalArgumentException("groupArtifact must be in the form 'groupId:artifactId'. it was: " + groupArtifact);
		}
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new JavaRefactorerState(NAME, groupArtifact, version, sourceJdkVersion, excluded, included, includeDraft, provisioner),
				JavaRefactorerState::createFormat);
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	public static String defaultGroupArtifact() {
		return MAVEN_COORDINATE;
	}

	static final class JavaRefactorerState implements Serializable {
		private static final long serialVersionUID = 1L;

		final JarState jarState;
		final String stepName;
		final String version;

		final String sourceJdkVersion;
		final List<String> included;
		final List<String> excluded;

		JavaRefactorerState(String stepName, String version, Provisioner provisioner) throws IOException {
			this(stepName, MAVEN_COORDINATE, version, defaultSourceJdk(), defaultExcludedMutators(), defaultMutators(), defaultIncludeDraft(), provisioner);
		}

		JavaRefactorerState(String stepName,
				String groupArtifact,
				String version,
				String sourceJdkVersion,
				List<String> included,
				List<String> excluded,
				boolean includeDraft,
				Provisioner provisioner) throws IOException {
			JVM_SUPPORT.assertFormatterSupported(version);
			ModuleHelper.doOpenInternalPackagesIfRequired();
			this.jarState = JarState.from(groupArtifact + ":" + version, provisioner);
			this.stepName = stepName;
			this.version = version;

			this.sourceJdkVersion = sourceJdkVersion;
			this.included = included;
			this.excluded = excluded;
		}

		@SuppressWarnings("PMD.UseProperClassLoader")
		FormatterFunc createFormat() {
			ClassLoader classLoader = jarState.getClassLoader();

			Object formatter;
			Method formatterMethod;
			try {
				Class<?> formatterClazz = classLoader.loadClass("com.diffplug.spotless.glue.java.JavaCleanthatRefactorerFunc");
				Constructor<?> formatterConstructor = formatterClazz.getConstructor(String.class, List.class, List.class);

				formatter = formatterConstructor.newInstance(sourceJdkVersion, included, excluded);
				formatterMethod = formatterClazz.getMethod("apply", String.class);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Issue executing the formatter", e);
			}
			return JVM_SUPPORT.suggestLaterVersionOnError(version, input -> {
				return (String) formatterMethod.invoke(formatter, input);
			});
		}

	}
}
