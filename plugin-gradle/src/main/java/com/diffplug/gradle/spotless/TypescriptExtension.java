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

import static com.diffplug.spotless.npm.TsFmtFormatterStep.NPM_PKG_TS;
import static com.diffplug.spotless.npm.TsFmtFormatterStep.NPM_PKG_TSFMT;
import static com.diffplug.spotless.npm.TsFmtFormatterStep.NPM_PKG_TSFMT_DEFAULT_VERSION;
import static com.diffplug.spotless.npm.TsFmtFormatterStep.NPM_PKG_TSLINT;
import static com.diffplug.spotless.npm.TsFmtFormatterStep.NPM_PKG_TSLINT_DEFAULT_VERSION;
import static com.diffplug.spotless.npm.TsFmtFormatterStep.NPM_PKG_TS_DEFAULT_VERSION;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.gradle.api.Project;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.npm.PrettierFormatterStep;
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

	/**
	 * Creates a {@code TypescriptFormatExtension} using the {@code typescript-formatter} npm package with the
	 * specified version.
	 * @param formatterVersion The version of the {@code typescript-formatter} npm package to use.
	 */
	public TypescriptFormatExtension tsfmt(String formatterVersion) {
		return tsfmt(formatterVersion, null, null);
	}

	/**
	 * Creates a {@code TypescriptFormatExtension} using the specified npm package versions.
	 * Map of version numbers for the following npm packages may be supplied:
	 * <ul>
	 *     <li>{@code formatterVersion} - the version of the npm package {@code typescript-formatter}</li>
	 *     <li>{@code typescriptVersion} - the version of the npm package {@code typescript}</li>
	 *     <li>{@code tslintVersion} - the version of the npm package {@code tslint}</li>
	 * </ul>
	 * @param versions The specified versions for the respective npm packages.
	 */
	public TypescriptFormatExtension tsfmt(Map<String, String> versions) {
		return tsfmt(versions.get(NPM_PKG_TSFMT), versions.get(NPM_PKG_TS), versions.get(NPM_PKG_TSLINT));
	}

	/**
	 * Creates a {@code TypescriptFormatExtension} using the specified npm package versions.
	 * @param formatterVersion the version of the npm package {@code typescript-formatter}
	 * @param typescriptVersion the version of the npm package {@code typescript}
	 * @param tslintVersion the version of the npm package {@code tslint}
	 */
	public TypescriptFormatExtension tsfmt(String formatterVersion, String typescriptVersion, String tslintVersion) {
		TypescriptFormatExtension tsfmt = new TypescriptFormatExtension(Optional.ofNullable(formatterVersion).orElse(NPM_PKG_TSFMT_DEFAULT_VERSION),
				Optional.ofNullable(typescriptVersion).orElse(NPM_PKG_TS_DEFAULT_VERSION),
				Optional.ofNullable(tslintVersion).orElse(NPM_PKG_TSLINT_DEFAULT_VERSION));
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
			this(NPM_PKG_TSFMT_DEFAULT_VERSION, NPM_PKG_TS_DEFAULT_VERSION, NPM_PKG_TSLINT_DEFAULT_VERSION);
		}

		public TypescriptFormatExtension(String formatterVersion, String typescriptVersion, String tslintVersion) {
			this.versions.put(NPM_PKG_TSFMT, requireNonNull(formatterVersion));
			this.versions.put(NPM_PKG_TS, requireNonNull(typescriptVersion));
			this.versions.put(NPM_PKG_TSLINT, requireNonNull(tslintVersion));
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
			this(PrettierFormatterStep.defaultVersion());
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
