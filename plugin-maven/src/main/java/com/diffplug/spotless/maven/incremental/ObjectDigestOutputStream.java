/*
 * Copyright 2021-2023 DiffPlug
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
package com.diffplug.spotless.maven.incremental;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class ObjectDigestOutputStream extends ObjectOutputStream {

	private final MessageDigest messageDigest;

	private ObjectDigestOutputStream(DigestOutputStream out) throws IOException {
		super(out);
		messageDigest = out.getMessageDigest();
	}

	static ObjectDigestOutputStream create() throws IOException {
		return new ObjectDigestOutputStream(createDigestOutputStream());
	}

	byte[] digest() {
		return messageDigest.digest();
	}

	private static DigestOutputStream createDigestOutputStream() {
		var nullOutputStream = new OutputStream() {
			@Override
			public void write(int b) {}
		};

		MessageDigest result;
		try {
			result = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 digest algorithm not available", e);
		}

		return new DigestOutputStream(nullOutputStream, result);
	}
}
