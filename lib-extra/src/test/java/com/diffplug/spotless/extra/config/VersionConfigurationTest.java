/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.extra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.diffplug.spotless.SerializableEqualityTester;

public class VersionConfigurationTest {

	@Test
	public void userArgumentRobustness() {
		List<String> major = Arrays.asList("1", " 1 ", "1. 0", "1.00", "01.00", "01.00.00");
		List<String> majorMinor = Arrays.asList("1.2", " 1 .2 ", "1.2.0", "01.02.0", "01.02.00");
		List<String> majorMinorPatch = Arrays.asList("1.2.3", "1.2. 3", "01.02.03");
		Arrays.asList(major, majorMinor, majorMinorPatch).forEach(validArgumentSet -> {
			SemanticVersion expected = null;
			for (String validArgument : validArgumentSet) {
				if (null == expected) {
					expected = new SemanticVersion(validArgument);
				}
				SemanticVersion current = new SemanticVersion(validArgument);
				assertThat(current).isEqualTo(expected);
			}
		});
	}

	@Test
	public void userArgumentValidation() {
		Arrays.asList(null, ".", "a", "1.", "1.2.", "1.2.a").forEach(invalidArgument -> {
			assertThatExceptionOfType(UserArgumentException.class).isThrownBy(() -> new SemanticVersion(invalidArgument));
		});
	}

	@Test
	public void equality() {

		new SerializableEqualityTester() {
			String version;

			@Override
			protected void setupTest(API api) {
				version = "1";
				api.areDifferentThan();
				version = "2";
				api.areDifferentThan();
				version = "2.1";
				api.areDifferentThan();
				version = "2.1.1";
			}

			@Override
			protected SemanticVersion create() {
				return new SemanticVersion(version);
			}
		}.testEquals();
	}

	@Test
	public void comparison() {
		SemanticVersion previous = new SemanticVersion("0");
		Arrays.asList("1", "1.1", "1.1.1", "1.1.2").forEach(ascending -> {
			SemanticVersion current = new SemanticVersion(ascending);
			assertThat(current.compareTo(current)).isZero();
			assertThat(previous.compareTo(current)).isNegative();
			assertThat(current.compareTo(previous)).isPositive();
		});
	}

}
