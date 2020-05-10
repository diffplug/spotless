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
package com.diffplug.spotless;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class FileSignatureRelocatableTest extends ResourceHarness {
	@Test
	public void relocateWorks() throws IOException {
		// paths much match
		FileSignature local = FileSignature.signAsSet(setFile("local/test").toContent("123"));
		FileSignature remote = FileSignature.signAsSet(setFile("remote/test").toContent("123"));
		assertNotEqual(local, remote);

		// but can be relocated
		FileSignatureRelocatable localRelocatable = new FileSignatureRelocatable(newFile("local"), local);
		FileSignatureRelocatable remoteRelocatable = new FileSignatureRelocatable(newFile("remote"), remote);
		assertAreEqual(localRelocatable, remoteRelocatable);

		// root directory matters
		FileSignatureRelocatable localFromRoot = new FileSignatureRelocatable(this.rootFolder(), local);
		assertNotEqual(localFromRoot, localRelocatable);
		assertNotEqual(localFromRoot, remoteRelocatable);

		// and so does content
		FileSignature content = FileSignature.signAsSet(setFile("content/test").toContent("abc"));
		FileSignatureRelocatable contentRelocatable = new FileSignatureRelocatable(newFile("content"), content);
		assertNotEqual(localRelocatable, contentRelocatable);
	}

	private void assertNotEqual(Object a, Object b) {
		Assertions.assertThat(serialize(a)).isNotEqualTo(serialize(b));
	}

	private void assertAreEqual(Object a, Object b) {
		Assertions.assertThat(serialize(a)).isEqualTo(serialize(b));
	}

	private byte[] serialize(Object in) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (ObjectOutputStream outputObj = new ObjectOutputStream(output)) {
			outputObj.writeObject(in);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		return output.toByteArray();
	}
}
