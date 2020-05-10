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
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * You give it a root directory and an object which contains FileSignatures - this class will now implement `Serializable`
 * and `equalsTo` in terms of content hashes and relative path names, which makes it possible to relocate up-to-date checks
 * between machines.  If you pass a file which is not a subfile of `rootDir`, it will use the file's name as its full path (likely
 * to be jar files resolved as dependencies).
 */
public class FileSignatureRelocatable extends LazyForwardingEquality<byte[]> implements FileSignatureRelocatableApi {
	private static final long serialVersionUID = 7134847654770187954L;

	private transient final String rootDir;
	private transient final Object containsFileSignatures;

	public FileSignatureRelocatable(File rootDir, Object containsFileSignatures) throws IOException {
		this.rootDir = rootDir.getCanonicalPath().replace('\\', '/') + "/";
		this.containsFileSignatures = Objects.requireNonNull(containsFileSignatures);
	}

	@Override
	protected byte[] calculateState() throws Exception {
		api.set(this);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutput = new ObjectOutputStream(output)) {
			objectOutput.writeObject(containsFileSignatures);
		} finally {
			api.set(null);
		}
		return output.toByteArray();
	}

	private static final ThreadLocal<FileSignatureRelocatableApi> api = new ThreadLocal<FileSignatureRelocatableApi>();

	static @Nullable FileSignatureRelocatableApi api() {
		return api.get();
	}

	@Override
	public void writeRelocatable(ObjectOutputStream outputStream, String[] unixPaths) throws IOException {
		try {
			for (String unixPath : unixPaths) {
				String pathToWrite;
				if (unixPath.startsWith(rootDir)) {
					// relocate files in the root dir
					pathToWrite = unixPath.substring(rootDir.length());
				} else {
					// files outside the root dir are referenced based only on their name, not path
					int lastSlash = unixPath.lastIndexOf('/');
					pathToWrite = lastSlash == -1 ? unixPath : unixPath.substring(lastSlash + 1);
				}
				outputStream.writeBytes(pathToWrite);

				MessageDigest md = MessageDigest.getInstance("SHA-256");
				byte[] hash = md.digest(Files.readAllBytes(Paths.get(unixPath)));
				outputStream.write(hash);
			}
		} catch (NoSuchAlgorithmException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}
}
