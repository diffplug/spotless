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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.diffplug.common.collect.ImmutableSortedMap;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.kotlin.DiktatStep;
import com.diffplug.spotless.kotlin.KtLintStep;
import com.diffplug.spotless.kotlin.KtfmtStep;

public abstract class BaseKotlinExtension extends FormatExtension {
	public BaseKotlinExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	public DiktatConfig diktat() {
		return diktat(DiktatStep.defaultVersionDiktat());
	}

	/** Adds the specified version of <a href="https://github.com/cqfn/diKTat">diktat</a>. */
	public DiktatConfig diktat(String version) {
		return new DiktatConfig(version);
	}

	public KtlintConfig ktlint() throws IOException {
		return ktlint(KtLintStep.defaultVersion());
	}

	/** Adds the specified version of <a href="https://github.com/pinterest/ktlint">ktlint</a>. */
	public KtlintConfig ktlint(String version) throws IOException {
		return new KtlintConfig(version, Collections.emptyMap(), Collections.emptyMap());
	}

	/** Uses the <a href="https://github.com/facebookincubator/ktfmt">ktfmt</a> jar to format source code. */
	public KtfmtConfig ktfmt() {
		return ktfmt(KtfmtStep.defaultVersion());
	}

	/**
	 * Uses the given version of <a href="https://github.com/facebookincubator/ktfmt">ktfmt</a> and applies the dropbox style
	 * option to format source code.
	 */
	public KtfmtConfig ktfmt(String version) {
		return new KtfmtConfig(version);
	}

	protected abstract boolean isScript();

	public class DiktatConfig {

		private final String version;
		private FileSignature config;

		DiktatConfig(String version) {
			Objects.requireNonNull(version, "version");
			this.version = version;
			addStep(createStep());
		}

		public DiktatConfig configFile(Object file) throws IOException {
			// Specify the path to the configuration file
			if (file == null) {
				this.config = null;
			} else {
				this.config = FileSignature.signAsList(getProject().file(file));
			}
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return DiktatStep.create(version, provisioner(), isScript(), config);
		}
	}

	public class KtfmtConfig {
		final String version;
		KtfmtStep.Style style;
		KtfmtStep.KtfmtFormattingOptions options;

		private final ConfigurableStyle configurableStyle = new ConfigurableStyle();

		KtfmtConfig(String version) {
			Objects.requireNonNull(version);
			this.version = Objects.requireNonNull(version);
			addStep(createStep());
		}

		private ConfigurableStyle style(KtfmtStep.Style style) {
			this.style = style;
			replaceStep(createStep());
			return configurableStyle;
		}

		public ConfigurableStyle dropboxStyle() {
			return style(KtfmtStep.Style.DROPBOX);
		}

		public ConfigurableStyle googleStyle() {
			return style(KtfmtStep.Style.GOOGLE);
		}

		public ConfigurableStyle kotlinlangStyle() {
			return style(KtfmtStep.Style.KOTLINLANG);
		}

		public void configure(Consumer<KtfmtStep.KtfmtFormattingOptions> optionsConfiguration) {
			this.configurableStyle.configure(optionsConfiguration);
		}

		private FormatterStep createStep() {
			return KtfmtStep.create(version, provisioner(), style, options);
		}

		public class ConfigurableStyle {

			public void configure(Consumer<KtfmtStep.KtfmtFormattingOptions> optionsConfiguration) {
				KtfmtStep.KtfmtFormattingOptions ktfmtFormattingOptions = new KtfmtStep.KtfmtFormattingOptions();
				optionsConfiguration.accept(ktfmtFormattingOptions);
				options = ktfmtFormattingOptions;
				replaceStep(createStep());
			}
		}
	}

	public class KtlintConfig {

		private final String version;
		@Nullable
		private FileSignature editorConfigPath;
		private Map<String, String> userData;
		private Map<String, Object> editorConfigOverride;

		KtlintConfig(String version, Map<String, String> config,
				Map<String, Object> editorConfigOverride) throws IOException {
			Objects.requireNonNull(version);
			File defaultEditorConfig = getProject().getRootProject().file(".editorconfig");
			FileSignature editorConfigPath = defaultEditorConfig.exists() ? FileSignature.signAsList(defaultEditorConfig) : null;
			this.version = version;
			this.editorConfigPath = editorConfigPath;
			this.userData = config;
			this.editorConfigOverride = editorConfigOverride;
			addStep(createStep());
		}

		public KtlintConfig setEditorConfigPath(Object editorConfigPath) throws IOException {
			if (editorConfigPath == null) {
				this.editorConfigPath = null;
			} else {
				File editorConfigFile = getProject().file(editorConfigPath);
				if (!editorConfigFile.exists()) {
					throw new IllegalArgumentException("EditorConfig file does not exist: " + editorConfigFile);
				}
				this.editorConfigPath = FileSignature.signAsList(editorConfigFile);
			}
			replaceStep(createStep());
			return this;
		}

		public KtlintConfig userData(Map<String, String> userData) {
			// Copy the map to a sorted map because up-to-date checking is based on binary-equals of the serialized
			// representation.
			this.userData = ImmutableSortedMap.copyOf(userData);
			replaceStep(createStep());
			return this;
		}

		public KtlintConfig editorConfigOverride(Map<String, Object> editorConfigOverride) {
			// Copy the map to a sorted map because up-to-date checking is based on binary-equals of the serialized
			// representation.
			this.editorConfigOverride = ImmutableSortedMap.copyOf(editorConfigOverride);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return KtLintStep.create(
					version,
					provisioner(),
					isScript(),
					editorConfigPath,
					userData,
					editorConfigOverride);
		}
	}
}
