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
package com.diffplug.gradle.spotless;

import static com.diffplug.gradle.spotless.FormatExtension.NpmStepConfig.SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.tag.NpmTest;

@TestMethodOrder(OrderAnnotation.class)
@NpmTest
class NpmInstallCacheIntegrationTests extends GradleIntegrationHarness {

	static File pertainingCacheDir;

	private static final File DEFAULT_DIR_FOR_NPM_INSTALL_CACHE_DO_NEVER_WRITE_TO_THIS = new File(".");

	@BeforeAll
	static void beforeAll(@TempDir File pertainingCacheDir) {
		NpmInstallCacheIntegrationTests.pertainingCacheDir = Errors.rethrow().get(pertainingCacheDir::getCanonicalFile);
	}

	@Test
	void prettierCachesNodeModulesToADefaultFolderWhenCachingEnabled() throws IOException {
		File dir1 = newFolder("npm-prettier-1");
		File cacheDir = DEFAULT_DIR_FOR_NPM_INSTALL_CACHE_DO_NEVER_WRITE_TO_THIS;
		BuildResult result = runPhpPrettierOnDir(dir1, cacheDir);
		assertThat(result.getOutput())
				.doesNotContain("Using cached node_modules for")
				.contains("Caching node_modules for ")
				.contains(Path.of(dir1.getAbsolutePath(), "build", SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME).toString());

	}

	@Test
	void prettierCachesAndReusesNodeModulesInSpecificInstallCacheFolder() throws IOException {
		File dir1 = newFolder("npm-prettier-1");
		File cacheDir = newFolder("npm-prettier-cache");
		BuildResult result = runPhpPrettierOnDir(dir1, cacheDir);
		assertThat(result.getOutput()).doesNotContainPattern("Using cached node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E");
		File dir2 = newFolder("npm-prettier-2");
		BuildResult result2 = runPhpPrettierOnDir(dir2, cacheDir);
		assertThat(result2.getOutput()).containsPattern("Using cached node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E");
	}

	@Test
	void prettierDoesNotCacheNodeModulesIfNotExplicitlyEnabled() throws IOException {
		File dir2 = newFolder("npm-prettier-1");
		BuildResult result = runPhpPrettierOnDir(dir2, null);
		assertThat(result.getOutput())
				.doesNotContainPattern("Using cached node_modules for .*")
				.doesNotContainPattern("Caching node_modules for .*");
	}

	@Test
	@Order(1)
	void prettierCachesNodeModuleInGlobalInstallCacheDir() throws IOException {
		File dir1 = newFolder("npm-prettier-global-1");
		File cacheDir = pertainingCacheDir;
		BuildResult result = runPhpPrettierOnDir(dir1, cacheDir);
		assertThat(result.getOutput())
				.doesNotContainPattern("Using cached node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E")
				.containsPattern("Caching node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E");
	}

	@Test
	@Order(2)
	void prettierUsesCachedNodeModulesFromGlobalInstallCacheDir() throws IOException {
		File dir2 = newFolder("npm-prettier-global-2");
		File cacheDir = pertainingCacheDir;
		BuildResult result = runPhpPrettierOnDir(dir2, cacheDir);
		assertThat(result.getOutput())
				.containsPattern("Using cached node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E")
				.doesNotContainPattern("Caching node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E");
	}

	private BuildResult runPhpPrettierOnDir(File projDir, File cacheDir) throws IOException {
		String baseDir = projDir.getName();
		String cacheDirEnabled = cacheDirEnabledStringForCacheDir(cacheDir);
		setFile(baseDir + "/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['tabWidth'] = 3",
				"prettierConfig['parser'] = 'php'",
				"def prettierPackages = [:]",
				"prettierPackages['prettier'] = '2.8.8'",
				"prettierPackages['@prettier/plugin-php'] = '0.19.6'",
				"spotless {",
				"    format 'php', {",
				"        target 'php-example.php'",
				"        prettier(prettierPackages).config(prettierConfig)" + cacheDirEnabled,
				"    }",
				"}");
		setFile(baseDir + "/php-example.php").toResource("npm/prettier/plugins/php.dirty");
		final BuildResult spotlessApply = gradleRunner().withProjectDir(projDir).withArguments("--stacktrace", "--info", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile(baseDir + "/php-example.php").sameAsResource("npm/prettier/plugins/php.clean");
		return spotlessApply;
	}

	@Test
	@Order(3)
	void tsfmtCachesNodeModuleInGlobalInstallCacheDir() throws IOException {
		File dir1 = newFolder("npm-tsfmt-global-1");
		File cacheDir = pertainingCacheDir;
		BuildResult result = runTsfmtOnDir(dir1, cacheDir);
		assertThat(result.getOutput())
				.doesNotContainPattern("Using cached node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E")
				.containsPattern("Caching node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E");
	}

	@Test
	@Order(4)
	void tsfmtUsesCachedNodeModulesFromGlobalInstallCacheDir() throws IOException {
		File dir2 = newFolder("npm-tsfmt-global-2");
		File cacheDir = pertainingCacheDir;
		BuildResult result = runTsfmtOnDir(dir2, cacheDir);
		assertThat(result.getOutput())
				.containsPattern("Using cached node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E")
				.doesNotContainPattern("Caching node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E");
	}

	@Test
	void tsfmtDoesNotCacheNodeModulesIfNotExplicitlyEnabled() throws IOException {
		File dir2 = newFolder("npm-tsfmt-1");
		BuildResult result = runTsfmtOnDir(dir2, null);
		assertThat(result.getOutput())
				.doesNotContainPattern("Using cached node_modules for .*")
				.doesNotContainPattern("Caching node_modules for .*");
	}

	private BuildResult runTsfmtOnDir(File projDir, File cacheDir) throws IOException {
		String baseDir = projDir.getName();
		String cacheDirEnabled = cacheDirEnabledStringForCacheDir(cacheDir);
		setFile(baseDir + "/build.gradle").toLines(
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
				"        tsfmt().config(tsfmtconfig)" + cacheDirEnabled,
				"    }",
				"}");
		setFile(baseDir + "/test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		final BuildResult spotlessApply = gradleRunner().withProjectDir(projDir).withArguments("--stacktrace", "--info", "spotlessApply").build();
		assertFile(baseDir + "/test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
		return spotlessApply;
	}

	@Test
	@Order(5)
	void eslintCachesNodeModuleInGlobalInstallCacheDir() throws IOException {
		File dir1 = newFolder("npm-eslint-global-1");
		File cacheDir = pertainingCacheDir;
		BuildResult result = runEslintOnDir(dir1, cacheDir);
		assertThat(result.getOutput())
				.doesNotContainPattern("Using cached node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E")
				.containsPattern("Caching node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E");
	}

	@Test
	@Order(6)
	void eslintUsesCachedNodeModulesFromGlobalInstallCacheDir() throws IOException {
		File dir2 = newFolder("npm-eslint-global-2");
		File cacheDir = pertainingCacheDir;
		BuildResult result = runEslintOnDir(dir2, cacheDir);
		assertThat(result.getOutput())
				.containsPattern("Using cached node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E")
				.doesNotContainPattern("Caching node_modules for .*\\Q" + cacheDir.getAbsolutePath() + "\\E");
	}

	@Test
	void eslintDoesNotCacheNodeModulesIfNotExplicitlyEnabled() throws IOException {
		File dir2 = newFolder("npm-eslint-1");
		File cacheDir = null;
		BuildResult result = runEslintOnDir(dir2, cacheDir);
		assertThat(result.getOutput())
				.doesNotContainPattern("Using cached node_modules for .*")
				.doesNotContainPattern("Caching node_modules for .*");
	}

	private BuildResult runEslintOnDir(File projDir, File cacheDir) throws IOException {
		String baseDir = projDir.getName();
		String cacheDirEnabled = cacheDirEnabledStringForCacheDir(cacheDir);

		setFile(baseDir + "/.eslintrc.js").toResource("npm/eslint/typescript/custom_rules/.eslintrc.js");
		setFile(baseDir + "/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    typescript {",
				"        target 'test.ts'",
				"        eslint().configFile('.eslintrc.js')" + cacheDirEnabled,
				"    }",
				"}");
		setFile(baseDir + "/test.ts").toResource("npm/eslint/typescript/custom_rules/typescript.dirty");
		BuildResult spotlessApply = gradleRunner().withProjectDir(projDir).withArguments("--stacktrace", "--info", "spotlessApply").build();
		assertFile(baseDir + "/test.ts").sameAsResource("npm/eslint/typescript/custom_rules/typescript.clean");
		return spotlessApply;
	}

	private static String cacheDirEnabledStringForCacheDir(File cacheDir) {
		String cacheDirEnabled;
		if (cacheDir == null) {
			cacheDirEnabled = "";
		} else if (cacheDir == DEFAULT_DIR_FOR_NPM_INSTALL_CACHE_DO_NEVER_WRITE_TO_THIS) {
			cacheDirEnabled = ".npmInstallCache()";
		} else {
			cacheDirEnabled = ".npmInstallCache('" + cacheDir.getAbsolutePath() + "')";
		}
		return cacheDirEnabled;
	}
}
