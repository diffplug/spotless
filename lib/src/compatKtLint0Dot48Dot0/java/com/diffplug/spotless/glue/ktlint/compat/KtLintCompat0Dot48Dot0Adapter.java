/*
 * Copyright 2023-2025 DiffPlug
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.pinterest.ktlint.core.KtLintRuleEngine;
import com.pinterest.ktlint.core.LintError;
import com.pinterest.ktlint.core.Rule;
import com.pinterest.ktlint.core.RuleProvider;
import com.pinterest.ktlint.core.RuleSetProviderV2;
import com.pinterest.ktlint.core.api.EditorConfigDefaults;
import com.pinterest.ktlint.core.api.EditorConfigOverride;
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties;
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleEditorConfigPropertyKt;
import com.pinterest.ktlint.core.api.editorconfig.DisabledRulesEditorConfigPropertyKt;
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty;
import com.pinterest.ktlint.core.api.editorconfig.IndentSizeEditorConfigPropertyKt;
import com.pinterest.ktlint.core.api.editorconfig.IndentStyleEditorConfigPropertyKt;
import com.pinterest.ktlint.core.api.editorconfig.InsertFinalNewLineEditorConfigPropertyKt;
import com.pinterest.ktlint.core.api.editorconfig.MaxLineLengthEditorConfigPropertyKt;
import com.pinterest.ktlint.core.api.editorconfig.RuleExecutionEditorConfigPropertyKt;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class KtLintCompat0Dot48Dot0Adapter implements KtLintCompatAdapter {

	private static final List<EditorConfigProperty<?>> DEFAULT_EDITOR_CONFIG_PROPERTIES;

	static {
		//noinspection deprecation
		DEFAULT_EDITOR_CONFIG_PROPERTIES = List.of(
				CodeStyleEditorConfigPropertyKt.getCODE_STYLE_PROPERTY(),
				DisabledRulesEditorConfigPropertyKt.getDISABLED_RULES_PROPERTY(),
				DisabledRulesEditorConfigPropertyKt.getKTLINT_DISABLED_RULES_PROPERTY(),
				IndentStyleEditorConfigPropertyKt.getINDENT_STYLE_PROPERTY(),
				IndentSizeEditorConfigPropertyKt.getINDENT_SIZE_PROPERTY(),
				InsertFinalNewLineEditorConfigPropertyKt.getINSERT_FINAL_NEWLINE_PROPERTY(),
				MaxLineLengthEditorConfigPropertyKt.getMAX_LINE_LENGTH_PROPERTY());
	}

	static class FormatterCallback implements Function2<LintError, Boolean, Unit> {
		@Override
		public Unit invoke(LintError lint, Boolean corrected) {
			if (!corrected) {
				KtLintCompatReporting.report(lint.getLine(), lint.getCol(), lint.getRuleId(), lint.getDetail());
			}
			return Unit.INSTANCE;
		}
	}

	@Override
	public String format(
			String unix,
			Path path,
			Path editorConfigPath,
			Map<String, Object> editorConfigOverrideMap) {
		final FormatterCallback formatterCallback = new FormatterCallback();

		Set<RuleProvider> allRuleProviders = ServiceLoader.load(RuleSetProviderV2.class, RuleSetProviderV2.class.getClassLoader())
				.stream()
				.flatMap(loader -> loader.get().getRuleProviders().stream())
				.collect(toUnmodifiableSet());

		EditorConfigOverride editorConfigOverride;
		if (editorConfigOverrideMap.isEmpty()) {
			editorConfigOverride = EditorConfigOverride.Companion.getEMPTY_EDITOR_CONFIG_OVERRIDE();
		} else {
			editorConfigOverride = createEditorConfigOverride(allRuleProviders.stream().map(
					RuleProvider::createNewRuleInstance).collect(
							toList()),
					editorConfigOverrideMap);
		}
		EditorConfigDefaults editorConfig;
		if (editorConfigPath == null || !Files.exists(editorConfigPath)) {
			editorConfig = EditorConfigDefaults.Companion.getEMPTY_EDITOR_CONFIG_DEFAULTS();
		} else {
			editorConfig = EditorConfigDefaults.Companion.load(editorConfigPath);
		}

		return new KtLintRuleEngine(
				allRuleProviders,
				editorConfig,
				editorConfigOverride,
				false)
				.format(unix, path, formatterCallback);
	}

	/**
	 * Create EditorConfigOverride from user provided parameters.
	 */
	private static EditorConfigOverride createEditorConfigOverride(final List<Rule> rules, Map<String, Object> editorConfigOverrideMap) {
		// Get properties from rules in the rule sets
		Stream<EditorConfigProperty<?>> ruleProperties = rules.stream()
				.filter(UsesEditorConfigProperties.class::isInstance)
				.flatMap(rule -> ((UsesEditorConfigProperties) rule).getEditorConfigProperties().stream());

		// Create a mapping of properties to their names based on rule properties and default properties
		Map<String, EditorConfigProperty<?>> supportedProperties = Stream
				.concat(ruleProperties, DEFAULT_EDITOR_CONFIG_PROPERTIES.stream())
				.distinct()
				.collect(toMap(EditorConfigProperty::getName, property -> property));

		// Create config properties based on provided property names and values
		@SuppressWarnings("unchecked")
		Pair<EditorConfigProperty<?>, ?>[] properties = editorConfigOverrideMap.entrySet().stream()
				.map(entry -> {
					EditorConfigProperty<?> property = supportedProperties.get(entry.getKey());

					if (property == null && entry.getKey().startsWith("ktlint_")) {
						String[] parts = entry.getKey().substring(7).split("_", 2);
						if (parts.length == 1) {
							// convert ktlint_{ruleset} to {ruleset}
							String qualifiedRuleId = parts[0] + ":";
							property = RuleExecutionEditorConfigPropertyKt.createRuleSetExecutionEditorConfigProperty(qualifiedRuleId);
						} else {
							// convert ktlint_{ruleset}_{rulename} to {ruleset}:{rulename}
							String qualifiedRuleId = parts[0] + ":" + parts[1];
							property = RuleExecutionEditorConfigPropertyKt.createRuleExecutionEditorConfigProperty(qualifiedRuleId);
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
