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
package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

/** String {@code targetExclude} patterns, see <a href="https://github.com/diffplug/spotless/issues/3147">#3147</a>. */
class TargetExcludePatternTest extends GradleIntegrationHarness {
	private void lowercaseFormat(String... targetAndExclude) throws IOException {
		String[] lines = new String[targetAndExclude.length + 6];
		lines[0] = "plugins { id 'com.diffplug.spotless' }";
		lines[1] = "spotless {";
		lines[2] = "  format 'toLower', {";
		System.arraycopy(targetAndExclude, 0, lines, 3, targetAndExclude.length);
		lines[lines.length - 3] = "    custom 'lowercase', { str -> str.toLowerCase() }";
		lines[lines.length - 2] = "  }";
		lines[lines.length - 1] = "}";
		setFile("build.gradle").toLines(lines);
	}

	@Test
	void excludePatternIsAppliedToTheTargetTree() throws IOException {
		// subtracting fileTree(projectDir).include('**/build/**') walks the whole project dir as soon as the
		// target is resolved, which happens at configuration time with the configuration cache
		lowercaseFormat(
				"    target 'src/**/*.md'",
				"    targetExclude '**/build/**'");
		setFile("build.gradle").toContent(read("build.gradle") + String.join("\n",
				"",
				"afterEvaluate {",
				"  def target = tasks.getByName('spotlessToLower').target",
				"  println('prunedTarget=' + (target instanceof ConfigurableFileTree && target.excludes.contains('**/build/**')))",
				"}"));
		setFile("src/manual.md").toLines("A B C");
		setFile("src/build/generated.md").toLines("A B C");

		BuildResult result = gradleRunner().withArguments("spotlessApply").build();

		assertThat(result.getOutput()).contains("prunedTarget=true");
		assertFile("src/manual.md").hasLines("a b c");
		assertFile("src/build/generated.md").hasLines("A B C");
	}

	@Test
	void mixOfDirectoryAndFilePatterns() throws IOException {
		lowercaseFormat(
				"    target '**/*.md'",
				"    targetExclude '**/build/**', 'docs/skip.md', ['**/gen/**']");
		setFile("a.md").toLines("A B C");
		setFile("docs/keep.md").toLines("A B C");
		setFile("docs/skip.md").toLines("A B C");
		setFile("module/build/out.md").toLines("A B C");
		setFile("module/gen/nested/out.md").toLines("A B C");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("a.md").hasLines("a b c");
		assertFile("docs/keep.md").hasLines("a b c");
		assertFile("docs/skip.md").hasLines("A B C");
		assertFile("module/build/out.md").hasLines("A B C");
		assertFile("module/gen/nested/out.md").hasLines("A B C");
	}

	@Test
	void patternTogetherWithDirectoryExclude() throws IOException {
		lowercaseFormat(
				"    target 'src/**/*.md', 'docs/**/*.md'",
				"    targetExclude '**/tmp/**', layout.projectDirectory.dir('docs/generated')");
		setFile("src/a.md").toLines("A B C");
		setFile("src/tmp/a.md").toLines("A B C");
		setFile("docs/b.md").toLines("A B C");
		setFile("docs/generated/b.md").toLines("A B C");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("src/a.md").hasLines("a b c");
		assertFile("src/tmp/a.md").hasLines("A B C");
		assertFile("docs/b.md").hasLines("a b c");
		assertFile("docs/generated/b.md").hasLines("A B C");
	}

	@Test
	void patternAgainstUserProvidedFileTree() throws IOException {
		// excludes stay relative to the project dir, even when the target tree has another root
		lowercaseFormat(
				"    target fileTree('src') { include '**/*.md' }",
				"    targetExclude 'src/build/**'");
		setFile("src/a.md").toLines("A B C");
		setFile("src/build/a.md").toLines("A B C");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("src/a.md").hasLines("a b c");
		assertFile("src/build/a.md").hasLines("A B C");
	}

	@Test
	void targetExcludeBeforeTarget() throws IOException {
		lowercaseFormat(
				"    targetExclude '**/build/**'",
				"    target 'src/**/*.md'");
		setFile("src/a.md").toLines("A B C");
		setFile("src/build/a.md").toLines("A B C");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("src/a.md").hasLines("a b c");
		assertFile("src/build/a.md").hasLines("A B C");
	}
}
