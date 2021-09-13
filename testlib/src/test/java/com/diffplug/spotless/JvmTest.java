/*
 * Copyright 2021 DiffPlug
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

public class JvmTest {

	private static final String TEST_NAME = "My Test Formatter";
	private Jvm.Support<String> testSupport;

	@Before
	public void initialize() {
		testSupport = Jvm.support(TEST_NAME);
	}

	@Test
	public void supportAdd() {
		Integer differentVersions[] = {0, 1, 2};
		Arrays.asList(differentVersions).stream().forEach(v -> testSupport.add(v + Jvm.version(), v.toString()));
		Arrays.asList(differentVersions).stream().forEach(v -> assertThat(testSupport.toString(), containsString(String.format("Version %d", v))));
		assertThat(testSupport.toString(), containsString(String.format("%s alternatives", TEST_NAME)));
	}

	@Test
	public void supportAddVerification() {
		Arrays.<Consumer<Jvm.Support<String>>> asList(
				s -> {
					s.add(1, "1.a");
				}, //Not a semantic version
				s -> {
					s.add(1, "0.1").add(1, "1.0");
				}, //forgot to adapt JVM version
				s -> {
					s.add(1, "0.1").add(2, "0.1");
				}, //forgot to adapt formatter version
				s -> {
					s.add(1, "1.0").add(2, "0.1");
				}, //higher formatter version requires lower Java version
				s -> {
					s.add(2, "0.1").add(1, "1.0");
				} //lower formatter version requires higher Java version
		).stream().forEach(configuration -> {
			Jvm.Support<String> support = Jvm.support(TEST_NAME);
			assertThrows(IllegalArgumentException.class, () -> {
				configuration.accept(support);
			});
		});
	}

	@Test
	public void supportEmptyConfiguration() {
		assertNull("No formatter version is configured", testSupport.getRecommendedFormatterVersion());

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
	public void supportListsMinimumJvmIfOnlyHigherJvmSupported() {
		int higherJvmVersion = Jvm.version() + 1;
		Exception testException = new Exception("Some test exception");
		testSupport.add(higherJvmVersion, "1.2.3");
		testSupport.add(higherJvmVersion + 1, "2.2.3");

		assertNull("No formatter version is supported", testSupport.getRecommendedFormatterVersion());

		for (String fmtVersion : Arrays.asList("1.2", "1.2.3")) {
			String proposal = assertThrows(Exception.class, () -> {
				testSupport.assertFormatterSupported(fmtVersion);
			}).getMessage();
			assertThat(proposal, containsString(String.format("on JVM %d", Jvm.version())));
			assertThat(proposal, containsString(String.format("%s %s requires JVM %d+", TEST_NAME, fmtVersion, higherJvmVersion)));
			assertThat(proposal, containsString(String.format("try %s alternatives", TEST_NAME)));

			proposal = assertThrows(Exception.class, () -> {
				testSupport.suggestLaterVersionOnError(fmtVersion, unused -> {
					throw testException;
				}).apply("");
			}).getMessage();
			assertThat(proposal, containsString(String.format("on JVM %d", Jvm.version())));
			assertThat(proposal, containsString(String.format("%s %s requires JVM %d+", TEST_NAME, fmtVersion, higherJvmVersion)));
			assertThat(proposal, containsString(String.format("try %s alternatives", TEST_NAME)));
		}

		for (String fmtVersion : Arrays.asList("1.2.4", "2")) {
			String proposal = assertThrows(Exception.class, () -> {
				testSupport.assertFormatterSupported(fmtVersion);
			}).getMessage();
			assertThat(proposal, containsString(String.format("%s %s requires JVM %d+", TEST_NAME, fmtVersion, higherJvmVersion + 1)));

			proposal = assertThrows(Exception.class, () -> {
				testSupport.suggestLaterVersionOnError(fmtVersion, unused -> {
					throw testException;
				}).apply("");
			}).getMessage();
			assertThat(proposal, containsString(String.format("%s %s requires JVM %d+", TEST_NAME, fmtVersion, higherJvmVersion + 1)));
		}
	}

	@Test
	public void supportProposesFormatterUpgrade() {
		int requiredJvm = Jvm.version() - 1;
		testSupport.add(Jvm.version() - 2, "1");
		testSupport.add(requiredJvm, "2");
		testSupport.add(Jvm.version() + 1, "3");
		for (String fmtVersion : Arrays.asList("0", "1", "1.9")) {
			testSupport.assertFormatterSupported(fmtVersion);

			String proposal = assertThrows(Exception.class, () -> {
				testSupport.suggestLaterVersionOnError(fmtVersion, unused -> {
					throw new Exception("Some test exception");
				}).apply("");
			}).getMessage();
			assertThat(proposal, containsString("not using latest version"));
			assertThat(proposal, containsString(String.format("on JVM %d+", requiredJvm)));
			assertThat(proposal, containsString(String.format("upgrade to %s %s", TEST_NAME, "2")));
		}
	}

	@Test
	public void supportProposesJvmUpgrade() {
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
			assertThat(proposal, containsString(String.format("on JVM %d", Jvm.version())));
			assertThat(proposal, containsString(String.format("limits you to %s %s", TEST_NAME, "1")));
			assertThat(proposal, containsString(String.format("upgrade your JVM to %d+", higherJvm)));
			assertThat(proposal, containsString(String.format("then you can use %s %s", TEST_NAME, "2")));
		}
	}

	@Test
	public void supportAllowsExperimentalVersions() {
		testSupport.add(Jvm.version(), "1.0");
		for (String fmtVersion : Arrays.asList("1", "2.0")) {
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
