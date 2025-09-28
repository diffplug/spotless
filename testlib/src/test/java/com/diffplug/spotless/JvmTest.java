/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JvmTest {

	private static final String TEST_NAME = "My Test Formatter";
	private Jvm.Support<String> testSupport;

	@BeforeEach
	void initialize() {
		testSupport = Jvm.support(TEST_NAME);
	}

	@Test
	void supportAdd() {
		Integer differentVersions[] = {0, 1, 2};
		Arrays.asList(differentVersions).forEach(v -> testSupport.add(v + Jvm.version(), v.toString()));
		Arrays.asList(differentVersions).forEach(v -> assertThat(testSupport.toString()).contains("Version %d".formatted(v)));
		assertThat(testSupport.toString()).contains("%s alternatives".formatted(TEST_NAME));
	}

	@ParameterizedTest(name = "{index} {1}")
	@MethodSource
	void supportAddFailsFor(Consumer<Jvm.Support<String>> configuration, String nameNotUsed) {
		assertThrows(IllegalArgumentException.class, () -> {
			configuration.accept(testSupport);
		});
	}

	private static Stream<Arguments> supportAddFailsFor() {
		return Stream.of(
				testCase(s -> s.add(1, "1.a"), "Non-semantic version"),
				testCase(s -> s.add(1, "1").add(1, "2"), "Duplicated JVM version"),
				testCase(s -> s.add(1, "1").add(2, "1"), "Duplicated formatter version"),
				testCase(s -> s.add(1, "1").add(2, "0"), "Higher JVM for lower formatter version"),
				testCase(s -> s.add(2, "0").add(1, "1"), "Lower JVM for higher formatter version"));
	}

	private static Arguments testCase(Consumer<Jvm.Support<String>> config, String name) {
		return Arguments.of(config, name);
	}

	@Test
	void supportEmptyConfiguration() {
		assertNull(testSupport.getRecommendedFormatterVersion(), "No formatter version is configured");

		testSupport.assertFormatterSupported("0.1");

		Exception expected = new Exception("Some test exception");
		Exception actual = assertThrows(Exception.class, () -> {
			testSupport.suggestLaterVersionOnError("0.0", unused -> {
				throw expected;
			}).apply("");
		});
		assertEquals(expected, actual);
	}

	@Test
	void supportListsMinimumJvmIfOnlyHigherJvmSupported() {
		int higherJvmVersion = Jvm.version() + 1;
		Exception testException = new Exception("Some test exception");
		testSupport.add(higherJvmVersion, "1.2.3");
		testSupport.add(higherJvmVersion + 1, "2.2.3");

		assertNull(testSupport.getRecommendedFormatterVersion(), "No formatter version is supported");

		for (String fmtVersion : Arrays.asList("1.2", "1.2.3", "1.2-SNAPSHOT", "1.2.3-SNAPSHOT")) {
			String proposal = assertThrows(Lint.ShortcutException.class, () -> {
				testSupport.assertFormatterSupported(fmtVersion);
			}).getLints().get(0).getDetail();
			assertThat(proposal).contains(String.format("on JVM %d", Jvm.version()));
			assertThat(proposal).contains("%s %s requires JVM %d+".formatted(TEST_NAME, fmtVersion, higherJvmVersion));
			assertThat(proposal).contains("try %s alternatives".formatted(TEST_NAME));

			proposal = assertThrows(Exception.class, () -> {
				testSupport.suggestLaterVersionOnError(fmtVersion, unused -> {
					throw testException;
				}).apply("");
			}).getMessage();
			assertThat(proposal).contains(String.format("on JVM %d", Jvm.version()));
			assertThat(proposal).contains("%s %s requires JVM %d+".formatted(TEST_NAME, fmtVersion, higherJvmVersion));
			assertThat(proposal).contains("try %s alternatives".formatted(TEST_NAME));
		}

		for (String fmtVersion : Arrays.asList("1.2.4", "2", "1.2.5-SNAPSHOT")) {
			String proposal = assertThrows(Lint.ShortcutException.class, () -> {
				testSupport.assertFormatterSupported(fmtVersion);
			}).getLints().get(0).getDetail();
			assertThat(proposal).contains("%s %s requires JVM %d+".formatted(TEST_NAME, fmtVersion, higherJvmVersion + 1));

			proposal = assertThrows(Exception.class, () -> {
				testSupport.suggestLaterVersionOnError(fmtVersion, unused -> {
					throw testException;
				}).apply("");
			}).getMessage();
			assertThat(proposal).contains("%s %s requires JVM %d+".formatted(TEST_NAME, fmtVersion, higherJvmVersion + 1));
		}
	}

	@Test
	void supportProposesFormatterUpgrade() {
		int requiredJvm = Jvm.version() - 1;
		testSupport.add(Jvm.version() - 2, "1");
		testSupport.add(requiredJvm, "2");
		testSupport.add(Jvm.version() + 1, "3");
		for (String fmtVersion : Arrays.asList("0", "1", "1.9", "1.9-SNAPSHOT")) {
			testSupport.assertFormatterSupported(fmtVersion);

			String proposal = assertThrows(Exception.class, () -> {
				testSupport.suggestLaterVersionOnError(fmtVersion, unused -> {
					throw new Exception("Some test exception");
				}).apply("");
			}).getMessage();
			assertThat(proposal.replace("\r", "")).isEqualTo("My Test Formatter " + fmtVersion + " is currently being used, but outdated.\n" +
					"My Test Formatter 2 is the recommended version, which may have fixed this problem.\n" +
					"My Test Formatter 2 requires JVM " + requiredJvm + "+.");
		}
	}

	@Test
	void supportProposesJvmUpgrade() {
		testSupport.add(Jvm.version(), "1");
		int higherJvm = Jvm.version() + 3;
		testSupport.add(higherJvm, "2");
		testSupport.add(higherJvm + 1, "3");
		for (String fmtVersion : Arrays.asList("1", "1.0")) {
			String proposal = assertThrows(Exception.class, () -> {
				testSupport.suggestLaterVersionOnError(fmtVersion, unused -> {
					throw new Exception("Some test exception");
				}).apply("");
			}).getMessage();
			assertThat(proposal).contains(String.format("on JVM %d", Jvm.version()));
			assertThat(proposal).contains("limits you to %s %s".formatted(TEST_NAME, "1"));
			assertThat(proposal).contains("upgrade your JVM to %d+".formatted(higherJvm));
			assertThat(proposal).contains("then you can use %s %s".formatted(TEST_NAME, "2"));
		}
	}

	@Test
	void supportAllowsExperimentalVersions() {
		testSupport.add(Jvm.version(), "1.0");
		for (String fmtVersion : Arrays.asList("1", "2.0", "1.1-SNAPSHOT", "2.0-SNAPSHOT")) {
			testSupport.assertFormatterSupported(fmtVersion);

			Exception testException = new Exception("Some test exception");
			Exception exception = assertThrows(Exception.class, () -> {
				testSupport.suggestLaterVersionOnError(fmtVersion, unused -> {
					throw testException;
				}).apply("");
			});
			assertEquals(testException, exception);
		}
	}

}
