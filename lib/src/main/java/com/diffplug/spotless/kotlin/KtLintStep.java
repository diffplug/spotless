/*
 * Copyright 2016-2024 DiffPlug
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

/** Wraps up <a href="https://github.com/pinterest/ktlint">ktlint</a> as a FormatterStep. */
public class KtLintStep implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_VERSION = "1.5.0";
	private static final String NAME = "ktlint";
	private static final String MAVEN_COORDINATE_0_DOT = "com.pinterest:ktlint:";
	private static final String MAVEN_COORDINATE_1_DOT = "com.pinterest.ktlint:ktlint-cli:";

	private final JarState.Promised jarState;
	@Nullable
	private final FileSignature.Promised config;
	private final Map<String, Object> editorConfigOverride;
	private final String version;

	private KtLintStep(String version,
			JarState.Promised jarState,
			@Nullable FileSignature config,
			Map<String, Object> editorConfigOverride) {
		this.version = version;
		this.jarState = jarState;
		this.config = config != null ? config.asPromise() : null;
		this.editorConfigOverride = editorConfigOverride;
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, null, Collections.emptyMap(), Collections.emptyList());
	}

	public static FormatterStep create(String version,
			Provisioner provisioner,
			@Nullable FileSignature editorConfig,
			Map<String, Object> editorConfigOverride,
			List<String> customRuleSets) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		String ktlintCoordinate = (version.startsWith("0.") ? MAVEN_COORDINATE_0_DOT : MAVEN_COORDINATE_1_DOT) + version;
		Set<String> mavenCoordinates = new HashSet<>(customRuleSets);
		mavenCoordinates.add(ktlintCoordinate);
		return FormatterStep.create(NAME,
				new KtLintStep(version, JarState.promise(() -> JarState.from(mavenCoordinates, provisioner)), editorConfig, editorConfigOverride),
				KtLintStep::equalityState,
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(version, jarState.get(), config != null ? config.get() : null, editorConfigOverride);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;
		/** The jar that contains the formatter. */
		private final JarState jarState;
		private final TreeMap<String, Object> editorConfigOverride;
		private final String version;
		@Nullable
		private final FileSignature editorConfigPath;

		State(String version,
				JarState jarState,
				@Nullable FileSignature editorConfigPath,
				Map<String, Object> editorConfigOverride) {
			this.version = version;
			this.jarState = jarState;
			this.editorConfigOverride = new TreeMap<>(editorConfigOverride);
			this.editorConfigPath = editorConfigPath;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();
			Class<?> formatterFunc = classLoader.loadClass("com.diffplug.spotless.glue.ktlint.KtlintFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(
					String.class, FileSignature.class, Map.class);
			return (FormatterFunc.NeedsFile) constructor.newInstance(version, editorConfigPath, editorConfigOverride);
		}
	}
}
