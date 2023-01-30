/*
 * Copyright 2016-2023 Solven
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

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * Enables CleanThat as a SpotLess step. This may be moved to Spotless own repo
 * (https://github.com/diffplug/spotless/tree/main/lib)
 *
 * @author Benoit Lacelle
 */
// https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#how-to-add-a-new-formatterstep
public final class CleanthatStepFactory {

	private static final String NAME = "cleanthat";
	private static final String MAVEN_COORDINATE = "io.github.solven-eu.cleanthat:java:";

	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String>support(NAME).add(8, "2.0");

	// prevent direct instantiation
	private CleanthatStepFactory() {
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(MAVEN_COORDINATE, version, defaultExcluded(), defaultIncluded(), provisioner);
	}

	private static List<String> defaultExcluded() {
		return List.of();
	}

	private static List<String> defaultIncluded() {
		return List.of("*");
	}

	/** Creates a step which formats everything - groupArtifact, code, import order, and unused imports. */
	public static FormatterStep create(String groupArtifact,
			String version,
			List<String> excluded,
			List<String> included,
			Provisioner provisioner) {
		Objects.requireNonNull(groupArtifact, "groupArtifact");
		if (groupArtifact.chars().filter(ch -> ch == ':').count() != 1) {
			throw new IllegalArgumentException("groupArtifact must be in the form 'groupId:artifactId'");
		}
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new JavaRulesState(NAME, groupArtifact, version, excluded, included, provisioner),
				JavaRulesState::createFormat);
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	static final class JavaRulesState implements Serializable {
		private static final long serialVersionUID = 1L;

		final JarState jarState;
		final String stepName;
		final String version;

		final List<String> included;
		final List<String> excluded;

		JavaRulesState(String stepName, String version, Provisioner provisioner) throws IOException {
			this(stepName, MAVEN_COORDINATE, version, defaultExcluded(), defaultIncluded(), provisioner);
		}

		JavaRulesState(String stepName,
				String groupArtifact,
				String version,
				List<String> included,
				List<String> excluded,
				Provisioner provisioner) throws IOException {
			JVM_SUPPORT.assertFormatterSupported(version);
			// ModuleHelper.doOpenInternalPackagesIfRequired();
			this.jarState = JarState.from(groupArtifact + ":" + version, provisioner);
			this.stepName = stepName;
			this.version = version;

			this.included = included;
			this.excluded = excluded;
		}

		@SuppressWarnings("PMD.UseProperClassLoader")
		FormatterFunc createFormat() {
			ClassLoader classLoader = jarState.getClassLoader();

			Object formatter;
			Method formatterMethod;
			try {
				Class<?> formatterClazz =
						classLoader.loadClass("com.diffplug.spotless.glue.java.JavaCleanthatRefactoringFunc");
				Constructor<?> formatterConstructor = formatterClazz.getConstructor(List.class, List.class);

				formatter = formatterConstructor.newInstance(included, excluded);
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
