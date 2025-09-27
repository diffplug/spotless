/*
 * Copyright 2016-2025 DiffPlug
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

import static java.util.Objects.requireNonNull;

import java.util.Locale;

final class PlatformInfo {
	private PlatformInfo() {
		// no instance
	}

	static OS normalizedOS() {
		final String osNameProperty = System.getProperty("os.name");
		if (osNameProperty == null) {
			throw new RuntimeException("No info about OS available, cannot decide which implementation of j2v8 to use");
		}
		final String normalizedOsName = osNameProperty.toLowerCase(Locale.ROOT);
		if (normalizedOsName.contains("win")) {
			return OS.WINDOWS;
		}
		if (normalizedOsName.contains("mac")) {
			return OS.MACOS;
		}
		if (normalizedOsName.contains("nix") || normalizedOsName.contains("nux") || normalizedOsName.contains("aix")) {
			return OS.LINUX;
		}
		throw new RuntimeException("Cannot handle os " + osNameProperty);
	}

	static String normalizedOSName() {
		return normalizedOS().normalizedOsName();
	}

	static String normalizedArchName() {
		final String osArchProperty = System.getProperty("os.arch");
		if (osArchProperty == null) {
			throw new RuntimeException("No info about ARCH available, cannot decide which implementation of j2v8 to use");
		}
		final String normalizedOsArch = osArchProperty.toLowerCase(Locale.ROOT);

		if (normalizedOsArch.contains("64")) {
			return "x86_64";
		}
		if (normalizedOsArch.contains("x86") || normalizedOsArch.contains("32")) {
			return "x86";
		}
		throw new RuntimeException("Cannot handle arch " + osArchProperty);
	}

	enum OS {
		WINDOWS("win32"), MACOS("macosx"), LINUX("linux");

		private final String normalizedOsName;

		OS(String normalizedOsName) {
			this.normalizedOsName = requireNonNull(normalizedOsName);
		}

		public String normalizedOsName() {
			return normalizedOsName;
		}
	}
}
