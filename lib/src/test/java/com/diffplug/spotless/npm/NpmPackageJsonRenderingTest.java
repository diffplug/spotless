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
package com.diffplug.spotless.npm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NpmPackageJsonRenderingTest {

	private static final String PRETTIER_TEMPLATE = "/com/diffplug/spotless/npm/prettier-package.json.template";

	private static String template(String path) {
		return NpmResourceHelper.readUtf8StringFromClasspath(NpmPackageJsonRenderingTest.class, path);
	}

	private static Map<String, String> sorted(Map<String, String> map) {
		return new TreeMap<>(map);
	}

	@Nested
	@DisplayName("given no dependency overrides")
	class NoOverrides {

		@Test
		@DisplayName("when rendering the package.json then it is byte-identical to the output before overrides existed")
		void rendersUnchangedPackageJson() {
			// given
			Map<String, String> devDependencies = sorted(Map.of("prettier", "3.0.0", "prettier-plugin-sql", "0.18.0"));

			// when
			String packageJson = NpmFormatterStepStateBase.replaceDependencies(template(PRETTIER_TEMPLATE), devDependencies, Map.of());

			// then
			assertThat(packageJson).isEqualTo("""
					{
						"name": "spotless-prettier",
						"version": "4.0.1",
						"description": "Spotless formatter step for running prettier as a rest service.",
						"repository": "https://github.com/diffplug/spotless",
						"license": "Apache-2.0",
						"scripts": {
							"start": "node serve.js"
						},
						"devDependencies": {
							"prettier": "3.0.0",
							"prettier-plugin-sql": "0.18.0",
							"express": "5.1.0",
							"http-graceful-shutdown": "3.1.14"
						},
						"dependencies": {},
						"engines": {
							"node": ">= 18"
						}
					}
					""".replace("\t\t\t\t\t", ""));
		}

		@Test
		@DisplayName("when using the legacy devDependencies-only method then the result equals rendering with empty overrides")
		void legacyMethodIsEquivalent() {
			Map<String, String> devDependencies = sorted(Map.of("prettier", "3.0.0"));

			assertThat(NpmFormatterStepStateBase.replaceDevDependencies(template(PRETTIER_TEMPLATE), devDependencies))
					.isEqualTo(NpmFormatterStepStateBase.replaceDependencies(template(PRETTIER_TEMPLATE), devDependencies, Map.of()));
		}
	}

	@Nested
	@DisplayName("given dependency overrides")
	class WithOverrides {

		@Test
		@DisplayName("when rendering then an overrides block is written with entries sorted by name")
		void rendersSortedOverridesBlock() {
			// given
			Map<String, String> devDependencies = sorted(Map.of("prettier", "3.0.0", "prettier-plugin-sql", "0.18.0"));
			Map<String, String> overrides = Map.of("sql-formatter", "15.0.2", "nan", "2.22.0");

			// when
			String packageJson = NpmFormatterStepStateBase.replaceDependencies(template(PRETTIER_TEMPLATE), devDependencies, overrides);

			// then
			assertThat(packageJson).contains("""
						"dependencies": {},
						"overrides": {
							"nan": "2.22.0",
							"sql-formatter": "15.0.2"
						},
						"engines": {
					""".replace("\t\t\t\t\t", ""));
		}

		@Test
		@DisplayName("when a value is an npm $reference then it is written verbatim")
		void keepsDollarReferences() {
			String packageJson = NpmFormatterStepStateBase.replaceDependencies(template(PRETTIER_TEMPLATE),
					sorted(Map.of("prettier", "3.0.0")), Map.of("prettier", "$prettier"));

			assertThat(packageJson).contains("\t\t\"prettier\": \"$prettier\"\n");
		}

		@Test
		@DisplayName("when a key or value contains JSON special characters then they are escaped")
		void escapesJsonSpecialCharacters() {
			String packageJson = NpmFormatterStepStateBase.replaceDependencies(template(PRETTIER_TEMPLATE),
					sorted(Map.of("prettier", "3.0.0")), Map.of("we\"ird", "1.0.0\\x"));

			assertThat(packageJson).contains("\t\t\"we\\\"ird\": \"1.0.0\\\\x\"\n");
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = {
				"/com/diffplug/spotless/npm/prettier-package.json.template",
				"/com/diffplug/spotless/npm/eslint-package.json.template",
				"/com/diffplug/spotless/npm/tsfmt-package.json.template"})
		@DisplayName("when rendering any npm formatter template then the overrides block is present and no placeholder is left")
		void supportedByAllNpmTemplates(String templatePath) {
			String packageJson = NpmFormatterStepStateBase.replaceDependencies(template(templatePath),
					sorted(Map.of("some-formatter", "1.0.0")), Map.of("transitive-lib", "2.0.0"));

			assertThat(packageJson)
					.contains("\t\"overrides\": {\n\t\t\"transitive-lib\": \"2.0.0\"\n\t},\n")
					.doesNotContain("${");
		}
	}
}
