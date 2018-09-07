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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.Project;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.npm.TsFmtFormatterStep;

import static java.util.Objects.requireNonNull;

public class TypescriptExtension extends FormatExtension {

	static final String NAME = "typescript";

	public TypescriptExtension(SpotlessExtension root) {
		super(root);
	}

	public TypescriptFormatExtension tsfmt() {
		TypescriptFormatExtension tsfmt = new TypescriptFormatExtension();
		addStep(tsfmt.createStep());
		return tsfmt;
	}

	public class TypescriptFormatExtension extends NpmStepConfig<TypescriptFormatExtension> {

		protected Map<String, Object> tsFmtConfig = Collections.emptyMap();

		public TypescriptFormatExtension config(Map<String, Object> config) {
			this.tsFmtConfig = new TreeMap<>(requireNonNull(config));
			replaceStep(createStep());
			return this;
		}

		public FormatterStep createStep() {
			final Project project = getProject();
			Map<String, Object> config = new TreeMap<>(this.tsFmtConfig);
			if (!config.containsKey("basedir")) {
				config.put("basedir", project.getRootDir().getAbsolutePath()); //by default we use our project dir
			}
			return TsFmtFormatterStep.create(
					GradleProvisioner.fromProject(project),
					project.getBuildDir(),
					npmFileOrNull(),
					tsFmtConfig);
		}
	}

	@Override
	public PrettierConfig prettier() {
		PrettierConfig prettierConfig = new TypescriptPrettierConfig();
		addStep(prettierConfig.createStep());
		return prettierConfig;
	}

	/**
	 * Overrides the parser to be set to typescript, no matter what the user's config says.
	 */
	public class TypescriptPrettierConfig extends PrettierConfig {
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
