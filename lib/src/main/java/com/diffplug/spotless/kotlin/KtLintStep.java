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
package com.diffplug.spotless.kotlin;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Wraps up <a href="https://github.com/pinterest/ktlint">ktlint</a> as a FormatterStep. */
public class KtLintStep {
	// prevent direct instantiation
	private KtLintStep() {}

	private static final String DEFAULT_VERSION = "1.0.0";
	static final String NAME = "ktlint";
	static final String MAVEN_COORDINATE_0_DOT = "com.pinterest:ktlint:";
	public static final String MAVEN_COORDINATE_1_DOT = "com.pinterest.ktlint:ktlint-cli:";

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, Collections.emptyMap(), Collections.emptyMap());
	}

	public static FormatterStep create(String version, Provisioner provisioner,
			Map<String, String> userData, Map<String, Object> editorConfigOverride) {
		return create(version, provisioner, false, null, userData, editorConfigOverride);
	}

	public static FormatterStep createForScript(String version, Provisioner provisioner) {
		return create(version, provisioner, true, null, Collections.emptyMap(), Collections.emptyMap());
	}

	public static FormatterStep createForScript(String version,
			Provisioner provisioner,
			@Nullable FileSignature editorConfigPath,
			Map<String, String> userData,
			Map<String, Object> editorConfigOverride) {
		return create(version,
				provisioner,
				true,
				editorConfigPath,
				userData,
				editorConfigOverride);
	}

	public static FormatterStep create(String version,
			Provisioner provisioner,
			boolean isScript,
			@Nullable FileSignature editorConfig,
			Map<String, String> userData,
			Map<String, Object> editorConfigOverride) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(version, provisioner, isScript, editorConfig, userData, editorConfigOverride),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** Are the files being linted Kotlin script files. */
		private final boolean isScript;
		/** The jar that contains the formatter. */
		final JarState jarState;
		private final TreeMap<String, String> userData;
		private final TreeMap<String, Object> editorConfigOverride;
		private final String version;
		@Nullable
		private final FileSignature editorConfigPath;

		State(String version,
				Provisioner provisioner,
				boolean isScript,
				@Nullable FileSignature editorConfigPath,
				Map<String, String> userData,
				Map<String, Object> editorConfigOverride) throws IOException {
			this.version = version;
			this.userData = new TreeMap<>(userData);
			this.editorConfigOverride = new TreeMap<>(editorConfigOverride);
			this.jarState = JarState.from((version.startsWith("0.") ? MAVEN_COORDINATE_0_DOT : MAVEN_COORDINATE_1_DOT) + version,
					provisioner);
			this.editorConfigPath = editorConfigPath;
			this.isScript = isScript;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();
			Class<?> formatterFunc = classLoader.loadClass("com.diffplug.spotless.glue.ktlint.KtlintFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(
					String.class, boolean.class, FileSignature.class, Map.class, Map.class);
			return (FormatterFunc.NeedsFile) constructor.newInstance(version, isScript, editorConfigPath, userData, editorConfigOverride);
		}
	}
}
