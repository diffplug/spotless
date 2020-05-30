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
package com.diffplug.spotless.npm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.IntStream;

import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ThrowingEx;

/** Shared config acress the NodeJS steps. */
public class NodeJsGlobal {
	static SharedLibFolder sharedLibs;

	static {
		sharedLibs = new SharedLibFolder(
				ThrowingEx.get(() -> Files.createTempDirectory("spotless-nodejs")));
		sharedLibs.root.deleteOnExit();
	}

	/**
	 * All of the NodeJS steps need to extract a bridge DLL for node.  By default this is
	 * a random location, but you can set it to be anywhere.
	 */
	public static void setSharedLibFolder(File sharedLibFolder) {
		sharedLibs = new SharedLibFolder(sharedLibFolder.toPath());
	}

	static class SharedLibFolder {
		private final File root;

		private SharedLibFolder(Path root) {
			this.root = ThrowingEx.get(() -> root.toFile().getCanonicalFile());
		}

		static final int MAX_CLASSLOADERS_PER_CLEAN = 1_000;

		synchronized File nextDynamicLib(ClassLoader loader, String resource) {
			// find a new unique file
			Optional<File> nextLibOpt = IntStream.range(0, MAX_CLASSLOADERS_PER_CLEAN)
					.mapToObj(i -> new File(root, i + "_" + resource))
					.filter(file -> !file.exists())
					.findFirst();
			if (!nextLibOpt.isPresent()) {
				throw new IllegalArgumentException("Overflow, delete the spotless nodeJs cache: " + root);
			}
			File nextLib = nextLibOpt.get();
			// copy the dll to it
			try {
				Files.createDirectories(nextLib.getParentFile().toPath());
				try (FileOutputStream fileOut = new FileOutputStream(nextLib);
						InputStream resourceIn = loader.loadClass("com.eclipsesource.v8.LibraryLoader").getResourceAsStream("/" + resource)) {
					byte[] buf = new byte[0x1000];
					while (true) {
						int r = resourceIn.read(buf);
						if (r == -1) {
							break;
						}
						fileOut.write(buf, 0, r);
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				throw ThrowingEx.asRuntime(e);
			}
			// make sure it is executable (on unix)
			if (LineEnding.PLATFORM_NATIVE.str().equals("\n")) {
				ThrowingEx.run(() -> {
					Runtime.getRuntime().exec(new String[]{"chmod", "755", nextLib.getAbsolutePath()}).waitFor(); //$NON-NLS-1$
				});
			}
			return nextLib;
		}
	}
}
