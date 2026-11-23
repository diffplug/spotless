/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.glue.ktlint.compat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.ktlint.core.cli.ruleset.core.api.RuleSetV2Provider;
import io.github.ktlint.core.rule.engine.api.Code;
import io.github.ktlint.core.rule.engine.api.EditorConfigDefaults;
import io.github.ktlint.core.rule.engine.api.EditorConfigOverride;
import io.github.ktlint.core.rule.engine.api.KtLintRuleEngine;
import io.github.ktlint.core.rule.engine.api.LintError;
import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision;
import io.github.ktlint.core.rule.engine.core.api.RuleId;
import io.github.ktlint.core.rule.engine.core.api.RuleSetId;
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider;
import io.github.ktlint.core.rule.engine.core.api.RuleV2ProviderKt;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.CodeStyleEditorConfigPropertyKtKt;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfigProperty;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EndOfLinePropertyKt;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.IndentSizeEditorConfigPropertyKt;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.IndentStyleEditorConfigPropertyKt;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.InsertFinalNewLineEditorConfigPropertyKt;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.MaxLineLengthEditorConfigPropertyKt;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.RuleExecution;
import io.github.ktlint.core.rule.engine.core.api.editorconfig.RuleExecutionEditorConfigPropertyKtKt;
import kotlin.Pair;

/**
 * Adapter for ktlint 2.x, which moved from {@code com.pinterest.ktlint} to {@code io.github.ktlint.core}.
 */
public class KtLintCompat2Dot0Dot0Adapter implements KtLintCompatAdapter {

	private static final List<EditorConfigProperty<?>> DEFAULT_EDITOR_CONFIG_PROPERTIES = List.of(
			CodeStyleEditorConfigPropertyKtKt.getCODE_STYLE_PROPERTY(),
			EndOfLinePropertyKt.getEND_OF_LINE_PROPERTY(),
			IndentSizeEditorConfigPropertyKt.getINDENT_SIZE_PROPERTY(),
			IndentStyleEditorConfigPropertyKt.getINDENT_STYLE_PROPERTY(),
			InsertFinalNewLineEditorConfigPropertyKt.getINSERT_FINAL_NEWLINE_PROPERTY(),
			MaxLineLengthEditorConfigPropertyKt.getMAX_LINE_LENGTH_PROPERTY(),
			RuleExecutionEditorConfigPropertyKtKt.getEXPERIMENTAL_RULES_EXECUTION_PROPERTY());

	@Override
	public String format(
			String unix,
			Path path,
			Path editorConfigPath,
			Map<String, Object> editorConfigOverrideMap) {
		Set<RuleV2Provider> allRuleProviders = loadRuleProviders();

		EditorConfigDefaults editorConfig = EditorConfigDefaults.Companion.load(editorConfigPath, RuleV2ProviderKt.propertyTypes(allRuleProviders));
		EditorConfigOverride editorConfigOverride;
		if (editorConfigOverrideMap.isEmpty()) {
			editorConfigOverride = EditorConfigOverride.Companion.getEMPTY_EDITOR_CONFIG_OVERRIDE();
		} else {
			editorConfigOverride = createEditorConfigOverride(editorConfig, allRuleProviders, editorConfigOverrideMap);
		}

		// create Code and then set the content to match previous steps in the Spotless pipeline
		Code code = Code.Companion.fromPath(path);
		KtLintCompatAdapter.setCodeContent(code, unix);

		// ktlint 2.x asks for the autocorrect decision while a rule is still executing, and wraps any exception thrown there,
		// so violations are collected and only reported once formatting has finished
		List<LintError> uncorrected = new ArrayList<>();
		String formatted = new KtLintRuleEngine(
				allRuleProviders,
				editorConfig,
				editorConfigOverride,
				path.getFileSystem())
				.format(
						code,
						true,
						lint -> {
							uncorrected.add(lint);
							return kotlin.Unit.INSTANCE;
						},
						lint -> {
							if (!lint.getCanBeAutoCorrected()) {
								uncorrected.add(lint);
							}
							return AutocorrectDecision.ALLOW_AUTOCORRECT;
						});
		if (!uncorrected.isEmpty()) {
			LintError lint = uncorrected.get(0);
			KtLintCompatReporting.report(lint.getLine(), lint.getCol(), lint.getRuleId().getValue(), lint.getDetail());
		}
		return formatted;
	}

	private static Set<RuleV2Provider> loadRuleProviders() {
		return ServiceLoader.load(RuleSetV2Provider.class, RuleSetV2Provider.class.getClassLoader())
				.stream()
				.flatMap(loader -> loader.get().getRuleProviders().stream())
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Create EditorConfigOverride from user provided parameters.
	 */
	private static EditorConfigOverride createEditorConfigOverride(final EditorConfigDefaults editorConfig, final Set<RuleV2Provider> ruleProviders, Map<String, Object> editorConfigOverrideMap) {
		// Create a mapping of properties to their names based on rule properties and default properties
		Map<String, EditorConfigProperty<?>> supportedProperties = Stream
				.concat(ruleProviders.stream().flatMap(provider -> provider.getUsesEditorConfigProperties().stream()), DEFAULT_EDITOR_CONFIG_PROPERTIES.stream())
				.distinct()
				.collect(Collectors.toMap(EditorConfigProperty::getName, property -> property, (first, second) -> first));

		Map<String, Object> overrides = new LinkedHashMap<>(editorConfigOverrideMap);
		// Spotless keeps intellij_idea as default code style, as it did for ktlint 1.x
		boolean isCodeStyleDefinedInEditorConfig = editorConfig.getValue().getSections().stream()
				.anyMatch(section -> section.getProperties().containsKey("ktlint_code_style"));
		if (!isCodeStyleDefinedInEditorConfig && !overrides.containsKey("ktlint_code_style")) {
			overrides.put("ktlint_code_style", "intellij_idea");
		}

		// Create config properties based on provided property names and values
		@SuppressWarnings("unchecked")
		Pair<EditorConfigProperty<?>, ?>[] properties = overrides.entrySet().stream()
				.map(entry -> {
					EditorConfigProperty<?> property = supportedProperties.get(entry.getKey());

					if (property == null && entry.getKey().startsWith("ktlint_")) {
						String[] parts = entry.getKey().substring(7).split("_", 2);
						if (parts.length == 1) {
							// convert ktlint_{ruleset} to RuleSetId
							RuleSetId id = new RuleSetId(parts[0]);
							property = RuleExecutionEditorConfigPropertyKtKt.createRuleSetExecutionEditorConfigProperty(id, RuleExecution.ENABLED);
						} else {
							// convert ktlint_{ruleset}_{rulename} to RuleId
							RuleId id = new RuleId(parts[0] + ":" + parts[1]);
							property = RuleExecutionEditorConfigPropertyKtKt.createRuleExecutionEditorConfigProperty(id, RuleExecution.ENABLED);
						}
					}

					if (property == null) {
						return null;
					} else {
						return new Pair<>(property, entry.getValue());
					}
				})
				.filter(Objects::nonNull)
				.toArray(Pair[]::new);

		return EditorConfigOverride.Companion.from(properties);
	}
}
