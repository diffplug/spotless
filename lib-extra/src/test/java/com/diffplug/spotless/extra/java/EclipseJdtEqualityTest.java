/*
 * Copyright 2024-2025 DiffPlug
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.ThrowingEx;

public class EclipseJdtEqualityTest extends ResourceHarness {
	@Test
	public void test() throws Exception {
		var settings1 = setFile("subfolder1/formatter.xml").toResource("java/eclipse/formatter.xml");
		var settings2 = setFile("subfolder2/formatter.xml").toResource("java/eclipse/formatter.xml");
		var step1 = withSettingsFile(settings1);
		var step2 = withSettingsFile(settings2);

		Assertions.assertTrue(step1.equals(step2));
		Assertions.assertTrue(step1.hashCode() == step2.hashCode());

		var serialized1 = toBytes(step1);
		var serialized2 = toBytes(step2);
		Assertions.assertFalse(Arrays.equals(serialized1, serialized2));
	}

	private static FormatterStep withSettingsFile(File settingsFile) {
		var builder = EclipseJdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
		builder.setPreferences(List.of(settingsFile));
		return builder.build();
	}

	private static byte[] toBytes(Serializable obj) {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput)) {
			objectOutput.writeObject(obj);
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
		return byteOutput.toByteArray();
	}
}
