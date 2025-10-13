/*
 * Copyright 2023-2024 DiffPlug
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3;
import com.pinterest.ktlint.rule.engine.api.Code;
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults;
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride;
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine;
import com.pinterest.ktlint.rule.engine.api.LintError;
import com.pinterest.ktlint.rule.engine.core.api.Rule;
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider;
import com.pinterest.ktlint.rule.engine.core.api.RuleProviderKt;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleEditorConfigPropertyKt;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EndOfLinePropertyKt;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.IndentSizeEditorConfigPropertyKt;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.IndentStyleEditorConfigPropertyKt;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.InsertFinalNewLineEditorConfigPropertyKt;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MaxLineLengthEditorConfigPropertyKt;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution;
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecutionEditorConfigPropertyKt;

import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class KtLintCompat0Dot49Dot0Adapter implements KtLintCompatAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(KtLintCompat0Dot49Dot0Adapter.class);

	private static final List<EditorConfigProperty<?>> DEFAULT_EDITOR_CONFIG_PROPERTIES;

	static {
		DEFAULT_EDITOR_CONFIG_PROPERTIES = List.of(
				CodeStyleEditorConfigPropertyKt.getCODE_STYLE_PROPERTY(),
				EndOfLinePropertyKt.getEND_OF_LINE_PROPERTY(),
				IndentSizeEditorConfigPropertyKt.getINDENT_SIZE_PROPERTY(),
				IndentStyleEditorConfigPropertyKt.getINDENT_STYLE_PROPERTY(),
				InsertFinalNewLineEditorConfigPropertyKt.getINSERT_FINAL_NEWLINE_PROPERTY(),
				MaxLineLengthEditorConfigPropertyKt.getMAX_LINE_LENGTH_PROPERTY(),
				RuleExecutionEditorConfigPropertyKt.getEXPERIMENTAL_RULES_EXECUTION_PROPERTY());
	}

	private static final Method RULEID_METHOD;
	private static final Method CREATE_RULESET_EXECUTION_METHOD;
	private static final Method CREATE_RULE_EXECUTION_METHOD;

	static {
		try {
			RULEID_METHOD = LintError.class.getMethod("getRuleId-6XN97os");
			CREATE_RULESET_EXECUTION_METHOD = RuleExecutionEditorConfigPropertyKt.class.getMethod("createRuleSetExecutionEditorConfigProperty-fqiwTpU", String.class, RuleExecution.class);
			CREATE_RULE_EXECUTION_METHOD = RuleExecutionEditorConfigPropertyKt.class.getMethod("createRuleExecutionEditorConfigProperty-U7AdEiY", String.class, RuleExecution.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getRuleId(LintError lint) {
		try {
			return (String) RULEID_METHOD.invoke(lint);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static EditorConfigProperty<?> createRuleSetExecution(String id, RuleExecution execution) {
		try {
			return (EditorConfigProperty<?>) CREATE_RULESET_EXECUTION_METHOD.invoke(null, id, execution);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static EditorConfigProperty<?> createRuleExecution(String id, RuleExecution execution) {
		try {
			return (EditorConfigProperty<?>) CREATE_RULE_EXECUTION_METHOD.invoke(null, id, execution);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	static class FormatterCallback implements Function2<LintError, Boolean, Unit> {

		@Override
		public Unit invoke(LintError lint, Boolean corrected) {
			if (!corrected) {
				KtLintCompatReporting.report(lint.getLine(), lint.getCol(), getRuleId(lint), lint.getDetail());
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

		Set<RuleProvider> allRuleProviders = ServiceLoader.load(RuleSetProviderV3.class, RuleSetProviderV3.class.getClassLoader())
				.stream()
				.flatMap(loader -> loader.get().getRuleProviders().stream())
				.collect(Collectors.toUnmodifiableSet());

		EditorConfigOverride editorConfigOverride;
		if (editorConfigOverrideMap.isEmpty()) {
			editorConfigOverride = EditorConfigOverride.Companion.getEMPTY_EDITOR_CONFIG_OVERRIDE();
		} else {
			editorConfigOverride = createEditorConfigOverride(allRuleProviders.stream().map(
					RuleProvider::createNewRuleInstance).collect(Collectors.toList()),
					editorConfigOverrideMap);
		}
		EditorConfigDefaults editorConfig;
		if (editorConfigPath == null || !Files.exists(editorConfigPath)) {
			editorConfig = EditorConfigDefaults.Companion.getEMPTY_EDITOR_CONFIG_DEFAULTS();
		} else {
			editorConfig = EditorConfigDefaults.Companion.load(editorConfigPath, RuleProviderKt.propertyTypes(allRuleProviders));
		}

		// create Code and then set the content to match previous steps in the Spotless pipeline
		Code code = Code.Companion.fromPath(path);
		KtLintCompatAdapter.setCodeContent(code, unix);
		return new KtLintRuleEngine(
				allRuleProviders,
				editorConfig,
				editorConfigOverride,
				false,
				path.getFileSystem())
				.format(code, formatterCallback);
	}

	/**
	 * Create EditorConfigOverride from user provided parameters.
	 */
	private static EditorConfigOverride createEditorConfigOverride(final List<Rule> rules, Map<String, Object> editorConfigOverrideMap) {
		// Get properties from rules in the rule sets
		Stream<EditorConfigProperty<?>> ruleProperties = rules.stream()
				.flatMap(rule -> rule.getUsesEditorConfigProperties().stream());

		// Create a mapping of properties to their names based on rule properties and default properties
		Map<String, EditorConfigProperty<?>> supportedProperties = Stream
				.concat(ruleProperties, DEFAULT_EDITOR_CONFIG_PROPERTIES.stream())
				.distinct()
				.collect(Collectors.toMap(EditorConfigProperty::getName, property -> property));

		// Create config properties based on provided property names and values
		@SuppressWarnings("unchecked")
		Pair<EditorConfigProperty<?>, ?>[] properties = editorConfigOverrideMap.entrySet().stream()
				.map(entry -> {
					EditorConfigProperty<?> property = supportedProperties.get(entry.getKey());

					if (property == null && entry.getKey().startsWith("ktlint_")) {
						String[] parts = entry.getKey().substring(7).split("_", 2);
						if (parts.length == 1) {
							// convert ktlint_{ruleset} to {ruleset}
							String id = parts[0];
							property = createRuleSetExecution(id, RuleExecution.enabled);
						} else {
							// convert ktlint_{ruleset}_{rulename} to {ruleset}:{rulename}
							String id = parts[0] + ":" + parts[1];
							property = createRuleExecution(id, RuleExecution.enabled);
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
