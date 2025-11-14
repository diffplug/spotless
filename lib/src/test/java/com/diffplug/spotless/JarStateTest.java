/*
 * Copyright 2025 DiffPlug
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JarStateTest {

	@TempDir
	Path tempDir;

	File a;

	File b;

	Provisioner provisioner = (withTransitives, deps) -> {
		Set<File> files = deps.stream().map(name -> name.equals("a") ? a : b).collect(Collectors.toSet());
		return LazyFiles.of(files);
	};

	@BeforeEach
	void setUp() throws IOException {
		a = Files.createTempFile(tempDir, "a", ".class").toFile();
		Files.writeString(a.toPath(), "a");
		b = Files.createTempFile(tempDir, "b", ".class").toFile();
		Files.writeString(b.toPath(), "b");
	}

	@AfterEach
	void tearDown() {
		JarState.setForcedClassLoader(null);
	}

	@Test
	void itCreatesClassloaderWhenForcedClassLoaderNotSet() throws IOException {
		JarState state1 = JarState.from(a.getName(), provisioner);
		JarState state2 = JarState.from(b.getName(), provisioner);

		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(state1.getClassLoader()).isNotNull();
			softly.assertThat(state2.getClassLoader()).isNotNull();
		});
	}

	@Test
	void itReturnsForcedClassloaderIfSetNoMatterIfSetBeforeOrAfterCreation() throws IOException {
		JarState stateA = JarState.from(a.getName(), provisioner);
		ClassLoader forcedClassLoader = new URLClassLoader(new URL[0]);
		JarState.setForcedClassLoader(forcedClassLoader);
		JarState stateB = JarState.from(b.getName(), provisioner);

		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(stateA.getClassLoader()).isSameAs(forcedClassLoader);
			softly.assertThat(stateB.getClassLoader()).isSameAs(forcedClassLoader);
		});
	}

	@Test
	void itReturnsForcedClassloaderEvenWhenRountripSerialized() throws IOException, ClassNotFoundException {
		JarState stateA = JarState.from(a.getName(), provisioner);
		ClassLoader forcedClassLoader = new URLClassLoader(new URL[0]);
		JarState.setForcedClassLoader(forcedClassLoader);
		JarState stateB = JarState.from(b.getName(), provisioner);

		JarState stateARoundtripSerialized = roundtripSerialize(stateA);
		JarState stateBRoundtripSerialized = roundtripSerialize(stateB);

		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(stateARoundtripSerialized.getClassLoader()).isSameAs(forcedClassLoader);
			softly.assertThat(stateBRoundtripSerialized.getClassLoader()).isSameAs(forcedClassLoader);
		});
	}

	private JarState roundtripSerialize(JarState state) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (ObjectOutputStream oOut = new ObjectOutputStream(outputStream)) {
			oOut.writeObject(state);
		}
		try (ObjectInputStream oIn = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
			return (JarState) oIn.readObject();
		}
	}

}
