/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.rome;

/**
 * Enumeration of possible computer architectures.
 */
enum Architecture {
	/** The arm64 architecture */
	ARM64,
	/** Either x64 or x64_32 architecture */
	X64;

	/**
	 * Attempts to guess the architecture of the environment running the JVM.
	 *
	 * @return The best guess for the architecture.
	 */
	public static Architecture guess() {
		var arch = System.getProperty("os.arch");
		var version = System.getProperty("os.version");

		if (arch == null || arch.isBlank()) {
			throw new IllegalStateException("No OS information is available, specify the Rome executable manually");
		}

		var msg = "Unsupported architecture " + arch + "/" + version
				+ ", specify the path to the Rome executable manually";

		if (arch.equals("ppc64le")) {
			throw new IllegalStateException(msg);
		}
		if (arch.equals("aarch64")) {
			throw new IllegalStateException(msg);
		}
		if (arch.equals("s390x")) {
			throw new IllegalStateException(msg);
		}
		if (arch.equals("ppc64")) {
			throw new IllegalStateException(msg);
		}
		if (arch.equals("ppc")) {
			throw new IllegalStateException(msg);
		}
		if (arch.equals("arm")) {
			if (version.contains("v7")) {
				throw new IllegalStateException(msg);
			}
			return ARM64;
		}
		if (arch.contains("64")) {
			return X64;
		}
		throw new IllegalStateException(msg);
	}
}
