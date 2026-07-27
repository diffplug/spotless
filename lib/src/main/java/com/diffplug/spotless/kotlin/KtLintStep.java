/*
 * Copyright 2016-2026 DiffPlug
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
package com.diffplug.spotless.kotlin;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Wraps up <a href="https://github.com/pinterest/ktlint">ktlint</a> as a FormatterStep. */
public final class KtLintStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_VERSION = "1.8.0";
	private static final String NAME = "ktlint";
	private static final String MAVEN_COORDINATE_0_DOT = "com.pinterest:ktlint:";
	private static final String MAVEN_COORDINATE_1_DOT = "com.pinterest.ktlint:ktlint-cli:";

	private final JarState.Promised jarState;
	@Nullable private final PromisedClasspath additionalClasspath;
	@Nullable private final FileSignature.Promised config;
	private final Map<String, Object> editorConfigOverride;
	private final String version;

	private KtLintStep(String version,
			JarState.Promised jarState,
			@Nullable PromisedClasspath additionalClasspath,
			@Nullable FileSignature config,
			Map<String, Object> editorConfigOverride) {
		this.version = version;
		this.jarState = jarState;
		this.additionalClasspath = additionalClasspath;
		this.config = config != null ? config.asPromise() : null;
		this.editorConfigOverride = editorConfigOverride;
	}

	/** Creates a ktlint step using the default version and configuration. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a ktlint step using the specified version and default configuration. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, null, Collections.emptyMap(), Collections.emptyList());
	}

	/** Creates a ktlint step with editor configuration and custom rule sets resolved from Maven coordinates. */
	public static FormatterStep create(String version,
			Provisioner provisioner,
			@Nullable FileSignature editorConfig,
			Map<String, Object> editorConfigOverride,
			List<String> customRuleSets) {
		return create(version, provisioner, editorConfig, editorConfigOverride, customRuleSets, null);
	}

	/**
	 * Creates a ktlint step with generated JARs whose contents are modeled separately by the calling build system.
	 * The supplier may return paths which do not exist until immediately before formatter execution.
	 */
	public static FormatterStep create(String version,
			Provisioner provisioner,
			@Nullable FileSignature editorConfig,
			Map<String, Object> editorConfigOverride,
			List<String> customRuleSets,
			@Nullable ThrowingEx.Supplier<? extends Iterable<File>> additionalClasspath) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		Set<String> mavenCoordinates = new HashSet<>(customRuleSets);
		mavenCoordinates.add(mavenCoordinate(version));
		return FormatterStep.create(NAME,
				new KtLintStep(
						version,
						JarState.promise(() -> JarState.from(mavenCoordinates, provisioner)),
						additionalClasspath == null ? null : new PromisedClasspath(additionalClasspath),
						editorConfig,
						editorConfigOverride),
				KtLintStep::equalityState,
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	/** Returns the Maven coordinate used for the specified ktlint version. */
	public static String mavenCoordinate(String version) {
		Objects.requireNonNull(version, "version");
		return (version.startsWith("0.") ? MAVEN_COORDINATE_0_DOT : MAVEN_COORDINATE_1_DOT) + version;
	}

	private State equalityState() {
		return new State(
				version,
				jarState.get(),
				additionalClasspath == null ? List.of() : additionalClasspath.get(),
				config != null ? config.get() : null,
				editorConfigOverride);
	}

	private static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
		/** The jar that contains the formatter. */
		private final JarState jarState;
		@SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "Project classpath contents are separate Gradle task inputs and must not enter formatter equality")
		private final transient List<File> additionalClasspath;
		private final TreeMap<String, Object> editorConfigOverride;
		private final String version;
		@Nullable private final FileSignature editorConfigPath;

		State(String version,
				JarState jarState,
				List<File> additionalClasspath,
				@Nullable FileSignature editorConfigPath,
				Map<String, Object> editorConfigOverride) {
			this.version = version;
			this.jarState = jarState;
			this.additionalClasspath = List.copyOf(additionalClasspath);
			this.editorConfigOverride = new TreeMap<>(editorConfigOverride);
			this.editorConfigPath = editorConfigPath;
		}

		FormatterFunc createFormat() throws Exception {
			JarState runtimeJarState = additionalClasspath == null || additionalClasspath.isEmpty()
					? jarState
					: jarState.withAdditionalJars(additionalClasspath);
			// At this time, it is possible to sign the generated JAR contents.
			final ClassLoader classLoader = runtimeJarState.getClassLoader();
			Class<?> formatterFunc = classLoader.loadClass("com.diffplug.spotless.glue.ktlint.KtlintFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(
					String.class, FileSignature.class, Map.class);
			return (FormatterFunc.NeedsFile) constructor.newInstance(version, editorConfigPath, editorConfigOverride);
		}
	}
}
