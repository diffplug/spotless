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
package com.diffplug.spotless.maven.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class UnifiedDiffTest {
	@Test
	void changedLineWithContext() {
		UnifiedDiff diff = UnifiedDiff.compute("a\nb\nc\n", "a\nB\nc\n", 0);
		assertThat(texts(diff)).containsExactly("@@ -1,3 +1,3 @@", " a", "-b", "+B", " c");
		assertThat(kinds(diff)).containsExactly(UnifiedDiff.Kind.HUNK, UnifiedDiff.Kind.CONTEXT, UnifiedDiff.Kind.REMOVED, UnifiedDiff.Kind.ADDED, UnifiedDiff.Kind.CONTEXT);
		assertThat(diff.added()).isEqualTo(1);
		assertThat(diff.removed()).isEqualTo(1);
		assertThat(diff.totalLines()).isEqualTo(5);
	}

	@Test
	void missingFinalNewlineIsShown() {
		UnifiedDiff diff = UnifiedDiff.compute("a", "a\n", 0);
		assertThat(texts(diff)).containsExactly("@@ -1 +1 @@", "-a", "\\ No newline at end of file", "+a");
		assertThat(kinds(diff)).contains(UnifiedDiff.Kind.NOTE);
	}

	@Test
	void distantChangesGetSeparateHunks() {
		StringBuilder before = new StringBuilder();
		StringBuilder after = new StringBuilder();
		for (int i = 0; i < 20; i++) {
			before.append("line ").append(i).append('\n');
			after.append(i == 1 || i == 18 ? "changed " : "line ").append(i).append('\n');
		}
		UnifiedDiff diff = UnifiedDiff.compute(before.toString(), after.toString(), 0);
		assertThat(kinds(diff).stream().filter(kind -> kind == UnifiedDiff.Kind.HUNK)).hasSize(2);
		assertThat(diff.added()).isEqualTo(2);
		assertThat(diff.removed()).isEqualTo(2);
	}

	@Test
	void keepsOnlyMaxLinesButCountsAll() {
		UnifiedDiff diff = UnifiedDiff.compute("a\nb\nc\n", "A\nB\nC\n", 2);
		assertThat(texts(diff)).containsExactly("@@ -1,3 +1,3 @@", "-a");
		assertThat(diff.totalLines()).isEqualTo(7);
		assertThat(diff.added()).isEqualTo(3);
		assertThat(diff.removed()).isEqualTo(3);
	}

	private static List<String> texts(UnifiedDiff diff) {
		return diff.lines().stream().map(line -> line.text).collect(Collectors.toList());
	}

	private static List<UnifiedDiff.Kind> kinds(UnifiedDiff diff) {
		return diff.lines().stream().map(line -> line.kind).collect(Collectors.toList());
	}
}
