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
package com.diffplug.spotless.extra.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ConfigurationCacheHackList;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.P2Provisioner;
import com.diffplug.spotless.generic.FenceStep;

/**
 * Regression tests for <a href="https://github.com/diffplug/spotless/issues/3119">#3119</a>.
 * <p>
 * A build that wraps {@code eclipse("4.19.0")} in {@code toggleOffOn()} failed to store the
 * configuration cache with {@code Failed to provision P2 dependencies}: serializing the
 * {@link FenceStep} serializes its nested Eclipse step, and the archived 4.19 P2 update site
 * could not be provisioned. Eclipse versions with an embedded lockfile must resolve from Maven
 * instead, so both Gradle serialization paths have to succeed without touching P2.
 */
class EclipseJdtFencedConfigurationCacheTest extends ResourceHarness {
	/** The exact pin from the report: trailing {@code .0}, normalized to {@code 4.19} by the builder. */
	private static final String REPORTED_VERSION = "4.19.0";

	private static final String UNFORMATTED = "package p;\n// spotless:off\nclass   Kept{}\n// spotless:on\nclass C{}";
	private static final String FORMATTED = "package p;\n// spotless:off\nclass   Kept{}\n// spotless:on\nclass C {\n}";

	private final AtomicInteger p2Calls = new AtomicInteger();

	/** Fails exactly like the reporter's build, so any P2 access breaks serialization. */
	private final P2Provisioner failingP2 = (model, maven, cacheDir) -> {
		p2Calls.incrementAndGet();
		throw new IOException("Failed to provision P2 dependencies");
	};

	@Test
	@DisplayName("given eclipse(\"4.19.0\") inside toggleOffOn, when the configuration cache stores and reloads the steps, then P2 is never queried and the reloaded step still formats outside the fence")
	void configurationCacheRoundtripDoesNotProvisionP2() throws Exception {
		// given
		ConfigurationCacheHackList roundtrip = ConfigurationCacheHackList.forRoundtrip();
		roundtrip.addAll(List.of(fencedEclipseStep()));

		// when
		ConfigurationCacheHackList reloaded = deserialize(serialize(roundtrip));

		// then
		assertThat(p2Calls).as("P2 provisioning attempts").hasValue(0);
		assertThat(reloaded.getSteps()).hasSize(1);
		StepHarnessWithFile.forStep(this, reloaded.getSteps().get(0)).test("test.java", UNFORMATTED, FORMATTED);
	}

	@Test
	@DisplayName("given two identically configured eclipse(\"4.19.0\") steps inside toggleOffOn, when Gradle fingerprints them for up-to-date checks, then P2 is never queried and the fingerprints are equal")
	void fingerprintDoesNotProvisionP2AndIsStable() throws Exception {
		// given
		ConfigurationCacheHackList first = ConfigurationCacheHackList.forEquality();
		first.addAll(List.of(fencedEclipseStep()));
		ConfigurationCacheHackList second = ConfigurationCacheHackList.forEquality();
		second.addAll(List.of(fencedEclipseStep()));

		// when
		byte[] firstFingerprint = serialize(first);
		byte[] secondFingerprint = serialize(second);

		// then
		assertThat(p2Calls).as("P2 provisioning attempts").hasValue(0);
		assertThat(firstFingerprint).isEqualTo(secondFingerprint);
	}

	/** Mirrors {@code java { eclipse("4.19.0"); toggleOffOn() }} in the Gradle plugin. */
	private FormatterStep fencedEclipseStep() {
		EclipseJdtFormatterStep.Builder builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral(), failingP2);
		builder.setVersion(REPORTED_VERSION);
		return FenceStep.named(FenceStep.defaultToggleName())
				.openClose(FenceStep.defaultToggleOff(), FenceStep.defaultToggleOn())
				.preserveWithin(List.of(builder.build()));
	}

	private static byte[] serialize(ConfigurationCacheHackList steps) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(steps);
		}
		return bytes.toByteArray();
	}

	private static ConfigurationCacheHackList deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return (ConfigurationCacheHackList) in.readObject();
		}
	}
}
