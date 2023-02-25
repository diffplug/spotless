/*
 * Copyright 2016-2021 DiffPlug
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.io.Files;

class SerializableMisc {
	static void toFile(Serializable obj, File file) {
		try {
			java.nio.file.Files.createDirectories(file.getParentFile().toPath());
			try (OutputStream output = Files.asByteSink(file).openBufferedStream()) {
				toStream(obj, output);
			}
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
	}

	static <T> T fromFile(Class<T> clazz, File file) {
		try (InputStream input = Files.asByteSource(file).openBufferedStream()) {
			return fromStream(clazz, input);
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
	}

	static void toStream(Serializable obj, OutputStream stream) {
		try (ObjectOutputStream objectOutput = new ObjectOutputStream(stream)) {
			objectOutput.writeObject(obj);
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
	}

	@SuppressWarnings("unchecked")
	static <T> T fromStream(Class<T> clazz, InputStream stream) {
		try (ObjectInputStream objectInput = new ObjectInputStream(stream)) {
			T object = (T) objectInput.readObject();
			Preconditions.checkArgument(clazz.isInstance(object), "Requires class %s, was %s", clazz, object);
			return object;
		} catch (ClassNotFoundException | IOException e) {
			throw Errors.asRuntime(e);
		}
	}
}
