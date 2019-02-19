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

import static com.diffplug.spotless.npm.TsFmtFormatterStep.DEFAULT_TYPESCRIPT_FORMATTER_TSLINT_VERSION;
import static com.diffplug.spotless.npm.TsFmtFormatterStep.DEFAULT_TYPESCRIPT_FORMATTER_TYPESCRIPT_VERSION;
import static com.diffplug.spotless.npm.TsFmtFormatterStep.DEFAULT_TYPESCRIPT_FORMATTER_VERSION;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.gradle.api.Project;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.npm.TsConfigFileType;
import com.diffplug.spotless.npm.TsFmtFormatterStep;
import com.diffplug.spotless.npm.TypedTsFmtConfigFile;

public class TypescriptExtension extends FormatExtension {

	static final String NAME = "typescript";

	public TypescriptExtension(SpotlessExtension root) {
		super(root);
	}

	public TypescriptFormatExtension tsfmt() {
		return tsfmt(Collections.emptyMap());
	}

	public TypescriptFormatExtension tsfmt(String formatterVersion) {
		return tsfmt(formatterVersion, null, null);
	}

	public TypescriptFormatExtension tsfmt(Map<String, String> versions) {
		return tsfmt(versions.get("formatterVersion"), versions.get("typescriptVersion"), versions.get("tslintVersion"));
	}

	public TypescriptFormatExtension tsfmt(String formatterVersion, String typescriptVersion, String tslintVersion) {
		TypescriptFormatExtension tsfmt = new TypescriptFormatExtension(Optional.ofNullable(formatterVersion).orElse(DEFAULT_TYPESCRIPT_FORMATTER_VERSION),
				Optional.ofNullable(typescriptVersion).orElse(DEFAULT_TYPESCRIPT_FORMATTER_TYPESCRIPT_VERSION),
				Optional.ofNullable(tslintVersion).orElse(DEFAULT_TYPESCRIPT_FORMATTER_TSLINT_VERSION));
		addStep(tsfmt.createStep());
		return tsfmt;
	}

	public class TypescriptFormatExtension extends NpmStepConfig<TypescriptFormatExtension> {

		private Map<String, Object> config = Collections.emptyMap();

		@Nullable
		TsConfigFileType configFileType = null;

		@Nullable
		Object configFilePath = null;

		private final Map<String, String> versions = new TreeMap<>();

		public TypescriptFormatExtension() {
			this(DEFAULT_TYPESCRIPT_FORMATTER_VERSION, DEFAULT_TYPESCRIPT_FORMATTER_TYPESCRIPT_VERSION, DEFAULT_TYPESCRIPT_FORMATTER_TSLINT_VERSION);
		}

		public TypescriptFormatExtension(String formatterVersion, String typescriptVersion, String tslintVersion) {
			this.versions.put("formatterVersion", requireNonNull(formatterVersion));
			this.versions.put("typescriptVersion", requireNonNull(typescriptVersion));
			this.versions.put("tslintVersion", requireNonNull(tslintVersion));
		}

		public void config(final Map<String, Object> config) {
			this.config = new TreeMap<>(requireNonNull(config));
			replaceStep(createStep());
		}

		public void tsconfigFile(final Object path) {
			configFile(TsConfigFileType.TSCONFIG, path);
		}

		public void tslintFile(final Object path) {
			configFile(TsConfigFileType.TSLINT, path);
		}

		public void vscodeFile(final Object path) {
			configFile(TsConfigFileType.VSCODE, path);
		}

		public void tsfmtFile(final Object path) {
			configFile(TsConfigFileType.TSFMT, path);
		}

		private void configFile(TsConfigFileType filetype, Object path) {
			this.configFileType = requireNonNull(filetype);
			this.configFilePath = requireNonNull(path);
			replaceStep(createStep());
		}

		public FormatterStep createStep() {
			final Project project = getProject();

			return TsFmtFormatterStep.create(
					versions,
					GradleProvisioner.fromProject(project),
					project.getBuildDir(),
					npmFileOrNull(),
					project.getProjectDir(),
					typedConfigFile(),
					config);
		}

		private TypedTsFmtConfigFile typedConfigFile() {
			if (this.configFileType != null && this.configFilePath != null) {
				return new TypedTsFmtConfigFile(this.configFileType, getProject().file(this.configFilePath));
			}
			return null;
		}
	}

	@Override
	public PrettierConfig prettier() {
		PrettierConfig prettierConfig = new TypescriptPrettierConfig();
		addStep(prettierConfig.createStep());
		return prettierConfig;
	}

	@Override
	public PrettierConfig prettier(String prettierVersion) {
		PrettierConfig prettierConfig = new TypescriptPrettierConfig(prettierVersion);
		addStep(prettierConfig.createStep());
		return prettierConfig;
	}

	/**
	 * Overrides the parser to be set to typescript, no matter what the user's config says.
	 */
	public class TypescriptPrettierConfig extends PrettierConfig {

		TypescriptPrettierConfig() {
			super();
		}

		TypescriptPrettierConfig(String prettierVersion) {
			super(prettierVersion);
		}

		@Override
		FormatterStep createStep() {
			fixParserToTypescript();
			return super.createStep();
		}

		private void fixParserToTypescript() {
			if (this.prettierConfig == null) {
				this.prettierConfig = Collections.singletonMap("parser", "typescript");
			} else {
				final Object replaced = this.prettierConfig.put("parser", "typescript");
				if (replaced != null) {
					getProject().getLogger().warn("overriding parser option to 'typescript'. Was set to '{}'", replaced);
				}
			}
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		// defaults to all typescript files
		if (target == null) {
			target = parseTarget("**/*.ts");
		}
		super.setupTask(task);
	}
}
