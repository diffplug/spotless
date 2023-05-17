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

import java.util.Locale;

/**
 * Enumeration of possible computer operation systems.
 */
enum OS {
	/** Any derivate of a Linux operation system. */
	LINUX,
	/** The Macintosh operating system/ */
	MAC_OS,
	/** The Microsoft Windows operating system. */
	WINDOWS,;

	/**
	 * Attempts to guess the OS of the environment running the JVM.
	 *
	 * @return The best guess for the architecture.
	 * @throws IllegalStateException When the OS is either unsupported or no
	 *                               information about the OS could be retrieved.
	 */
	public static OS guess() {
		var osName = System.getProperty("os.name");
		if (osName == null || osName.isBlank()) {
			throw new IllegalStateException("No OS information is available, specify the Rome executable manually");
		}
		var osNameUpper = osName.toUpperCase(Locale.ROOT);
		if (osNameUpper.contains("SUNOS") || osName.contains("AIX")) {
			throw new IllegalStateException(
					"Unsupported OS " + osName + ", specify the path to the Rome executable manually");
		}
		if (osNameUpper.contains("WINDOWS")) {
			return OS.WINDOWS;
		} else if (osNameUpper.contains("MAC")) {
			return OS.MAC_OS;
		} else {
			return OS.LINUX;
		}
	}
}
