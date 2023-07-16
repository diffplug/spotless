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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.EslintStyleGuide;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class TypescriptExtensionTest extends GradleIntegrationHarness {

	private static String styleGuideMapString(String styleGuideName) {
		return EslintStyleGuide.fromNameOrNull(styleGuideName).asGradleMapStringMergedWith(EslintFormatterStep.defaultDevDependencies());
	}

	@Test
	void allowToSpecifyFormatterVersion() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def tsfmtconfig = [:]",
				"tsfmtconfig['indentSize'] = 1",
				"tsfmtconfig['convertTabsToSpaces'] = true",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        tsfmt('7.2.1').config(tsfmtconfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
	}

	@Test
	void allowToSpecifyMultipleVersionStrings() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def tsfmtconfig = [:]",
				"tsfmtconfig['indentSize'] = 1",
				"tsfmtconfig['convertTabsToSpaces'] = true",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        tsfmt(['typescript-formatter': '7.2.1', 'tslint': '5.1.0', 'typescript': '2.9.2']).config(tsfmtconfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
	}

	@Test
	void useTsfmtInlineConfig() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def tsfmtconfig = [:]",
				"tsfmtconfig['indentSize'] = 1",
				"tsfmtconfig['convertTabsToSpaces'] = true",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        tsfmt().config(tsfmtconfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
	}

	@Test
	void useTsfmtFileConfig() throws IOException {
		setFile("tsfmt.json").toResource("npm/tsfmt/tsfmt/tsfmt.json");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        tsfmt().tsfmtFile('tsfmt.json')",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
	}

	@Test
	void useTsConfigFileConfig() throws IOException {
		setFile("tsconfig.json").toResource("npm/tsfmt/tsconfig/tsconfig.json");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    typescript {",
				"        target 'src/**/*.ts'",
				"        tsfmt().tsconfigFile('tsconfig.json')",
				"    }",
				"}");
		setFile("src/main/typescript/test.ts").toResource("npm/tsfmt/tsconfig/tsconfig.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("src/main/typescript/test.ts").sameAsResource("npm/tsfmt/tsconfig/tsconfig.clean");
	}

	@Test
	void usePrettier() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        prettier()",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/filetypes/typescript/typescript.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/prettier/filetypes/typescript/typescript.clean");
	}

	@Test
	void useEslint() throws IOException {
		setFile(".eslintrc.js").toResource("npm/eslint/typescript/custom_rules/.eslintrc.js");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        eslint().configFile('.eslintrc.js')",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/eslint/typescript/custom_rules/typescript.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/eslint/typescript/custom_rules/typescript.clean");
	}

	@Test
	@Disabled("https://github.com/diffplug/spotless/issues/1756")
	void useEslintXoStandardRules() throws IOException {
		setFile(".eslintrc.js").toResource("npm/eslint/typescript/styleguide/xo/.eslintrc.js");
		setFile("tsconfig.json").toResource("npm/eslint/typescript/styleguide/xo/tsconfig.json");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        eslint(" + styleGuideMapString("xo-typescript") + ").configFile('.eslintrc.js')",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/eslint/typescript/styleguide/xo/typescript.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/eslint/typescript/styleguide/xo/typescript.clean");
	}

	@Test
	void useEslintStandardWithTypescriptRules() throws IOException {
		setFile(".eslintrc.js").toResource("npm/eslint/typescript/styleguide/standard_with_typescript/.eslintrc.js");
		setFile("tsconfig.json").toResource("npm/eslint/typescript/styleguide/standard_with_typescript/tsconfig.json");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        eslint(" + styleGuideMapString("standard-with-typescript") + ").configFile('.eslintrc.js')",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/eslint/typescript/styleguide/standard_with_typescript/typescript.dirty");
		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertFile("test.ts").sameAsResource("npm/eslint/typescript/styleguide/standard_with_typescript/typescript.clean");
	}
}
