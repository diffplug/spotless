/*
 * Copyright 2023-2024 DiffPlug
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

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.diffplug.spotless.Formatter;
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
public final class CleanthatJavaStep implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String NAME = "cleanthat";
	private static final String MAVEN_COORDINATE = "io.github.solven-eu.cleanthat:java";
	/**
	 * CleanThat changelog is available at <a href="https://github.com/solven-eu/cleanthat/blob/master/CHANGES.MD">here</a>.
	 */
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(11, "2.22");

	private final JarState.Promised jarState;
	private final String version;
	private final String sourceJdkVersion;
	private final List<String> included;
	private final List<String> excluded;
	private final boolean includeDraft;

	private CleanthatJavaStep(JarState.Promised jarState,
			String version,
			String sourceJdkVersion,
			List<String> included,
			List<String> excluded,
			boolean includeDraft) {
		this.jarState = jarState;
		this.version = version;

		this.sourceJdkVersion = sourceJdkVersion;
		this.included = included;
		this.excluded = excluded;
		this.includeDraft = includeDraft;
	}

	/** Creates a step that applies default CleanThat mutators. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step that applies default CleanThat mutators. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return createWithStepName(NAME, MAVEN_COORDINATE, version, defaultSourceJdk(), defaultMutators(), defaultExcludedMutators(), defaultIncludeDraft(), provisioner);
	}

	public static String defaultSourceJdk() {
		// see IJdkVersionConstants.JDK_7
		// https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#source
		// 1.7 is the default for 'maven-compiler-plugin' since 3.9.0
		return "1.7";
	}

	/**
	 * By default, we include only safe and consensual mutators
	 */
	public static List<String> defaultMutators() {
		// see ICleanthatStepParametersProperties.SAFE_AND_CONSENSUAL
		return List.of("SafeAndConsensual");
	}

	public static List<String> defaultExcludedMutators() {
		return List.of();
	}

	public static boolean defaultIncludeDraft() {
		return false;
	}

	/** Creates a step that applies selected CleanThat mutators. */
	static FormatterStep createWithStepName(String stepName,
			String groupArtifact,
			String version,
			String sourceJdkVersion,
			List<String> included,
			List<String> excluded,
			boolean includeDraft,
			Provisioner provisioner) {
		Objects.requireNonNull(groupArtifact, "groupArtifact");
		if (groupArtifact.chars().filter(ch -> ch == ':').count() != 1) {
			throw new IllegalArgumentException("groupArtifact must be in the form 'groupId:artifactId'. it was: " + groupArtifact);
		}
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.create(stepName,
				new CleanthatJavaStep(JarState.promise(() -> JarState.from(groupArtifact + ":" + version, provisioner)), version, sourceJdkVersion, included, excluded, includeDraft),
				CleanthatJavaStep::equalityState,
				State::createFormat);
	}

	/** Creates a step that applies selected CleanThat mutators. */
	public static FormatterStep create(String groupArtifact,
			String version,
			String sourceJdkVersion,
			List<String> included,
			List<String> excluded,
			boolean includeDraft,
			Provisioner provisioner) {
		return createWithStepName(NAME, groupArtifact, version, sourceJdkVersion, included, excluded, includeDraft, provisioner);
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return Objects.requireNonNull(JVM_SUPPORT.getRecommendedFormatterVersion());
	}

	public static String defaultGroupArtifact() {
		return MAVEN_COORDINATE;
	}

	private State equalityState() {
		return new State(jarState.get(), version, sourceJdkVersion, included, excluded, includeDraft);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final JarState jarState;
		private final String version;
		private final String sourceJdkVersion;
		private final List<String> included;
		private final List<String> excluded;
		private final boolean includeDraft;

		State(JarState jarState,
				String version,
				String sourceJdkVersion,
				List<String> included,
				List<String> excluded,
				boolean includeDraft) {
			JVM_SUPPORT.assertFormatterSupported(version);
			ModuleHelper.doOpenInternalPackagesIfRequired();
			this.jarState = jarState;
			this.version = version;
			this.sourceJdkVersion = sourceJdkVersion;
			this.included = included;
			this.excluded = excluded;
			this.includeDraft = includeDraft;
		}

		private static class JvmSupportFormatterFunc implements FormatterFunc {

			final Object formatter;
			final Method formatterMethod;

			private JvmSupportFormatterFunc(Object formatter, Method formatterMethod) {
				this.formatter = formatter;
				this.formatterMethod = formatterMethod;
			}

			@Override
			public String apply(String input) throws Exception {
				return apply(input, Formatter.NO_FILE_SENTINEL);
			}

			@Override
			public String apply(String input, File file) throws Exception {
				if (file.isAbsolute()) {
					// Cleanthat expects a relative file as input (relative to the root of the repository)
					Path absolutePath = file.toPath();
					file = absolutePath.subpath(1, absolutePath.getNameCount()).toFile();
				}
				return (String) formatterMethod.invoke(formatter, input, file);
			}
		}

		FormatterFunc createFormat() {
			ClassLoader classLoader = jarState.getClassLoader();

			Object formatter;
			Method formatterMethod;
			try {
				Class<?> formatterClazz = classLoader.loadClass("com.diffplug.spotless.glue.java.JavaCleanthatRefactorerFunc");
				Constructor<?> formatterConstructor = formatterClazz.getConstructor(String.class, List.class, List.class, boolean.class);

				formatter = formatterConstructor.newInstance(sourceJdkVersion, included, excluded, includeDraft);
				formatterMethod = formatterClazz.getMethod("apply", String.class, File.class);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Issue executing the formatter", e);
			}

			FormatterFunc formatterFunc = new JvmSupportFormatterFunc(formatter, formatterMethod);

			return JVM_SUPPORT.suggestLaterVersionOnError(version, formatterFunc);
		}
	}
}
