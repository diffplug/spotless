/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.npm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import com.diffplug.spotless.ThrowingEx;

final class NpmResourceHelper {
	private NpmResourceHelper() {
		// no instance required
	}

	static void writeUtf8StringToFile(File file, String stringToWrite) throws IOException {
		Files.write(file.toPath(), stringToWrite.getBytes(StandardCharsets.UTF_8));
	}

	static void writeUtf8StringToOutputStream(String stringToWrite, OutputStream outputStream) throws IOException {
		final byte[] bytes = stringToWrite.getBytes(StandardCharsets.UTF_8);
		outputStream.write(bytes);
	}

	static void deleteFileIfExists(File file) throws IOException {
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException("Failed to delete " + file);
			}
		}
	}

	static String readUtf8StringFromClasspath(Class<?> clazz, String resourceName) {
		try (InputStream input = clazz.getResourceAsStream(resourceName)) {
			return readUtf8StringFromInputStream(input);
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	static String readUtf8StringFromFile(File file) {
		try {
			return String.join("\n", Files.readAllLines(file.toPath()));
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	static String readUtf8StringFromInputStream(InputStream input) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int numRead;
			while ((numRead = input.read(buffer)) != -1) {
				output.write(buffer, 0, numRead);
			}
			return output.toString(StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	static void assertDirectoryExists(File directory) throws IOException {
		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				throw new IOException("cannot create temp dir for node modules at " + directory);
			}
		}
	}

	static void awaitReadableFile(File file, Duration maxWaitTime) throws TimeoutException {
		final long startedAt = System.currentTimeMillis();
		while (!file.exists() || !file.canRead()) {
			// wait for at most maxWaitTime
			if ((System.currentTimeMillis() - startedAt) > maxWaitTime.toMillis()) {
				throw new TimeoutException("The file did not appear within " + maxWaitTime);
			}
		}
	}
}
