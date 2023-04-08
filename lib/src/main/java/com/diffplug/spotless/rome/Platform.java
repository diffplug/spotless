/*
 * Copyright 2016-2022 DiffPlug
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
 * Represents a platform where code is run, consisting of an operating system
 * and an architecture.
 */
class Platform {
	/**
	 * Attempts to guess the platform of the hosting environment running the JVM
	 * machine.
	 * 
	 * @return The best guess for the current OS and architecture.
	 * @throws IllegalStateException When no OS information is available, or when
	 *                               the OS or architecture is unsupported.
	 */
	public static Platform guess() {
		var os = OS.guess();
		var architecture = Architecture.guess();
		return new Platform(os, architecture);
	}

	private final Architecture architecture;

	private final OS os;

	/**
	 * Creates a new Platform descriptor for the given OS and architecture.
	 * 
	 * @param os           Operating system of the platform.
	 * @param architecture Architecture of the platform.
	 */
	public Platform(OS os, Architecture architecture) {
		this.os = os;
		this.architecture = architecture;
	}

	/**
	 * @return The architecture of this platform.
	 */
	public Architecture getArchitecture() {
		return architecture;
	}

	/**
	 * @return The operating system of this platform.
	 */
	public OS getOs() {
		return os;
	}

	/**
	 * @return Whether the operating system is Linux.
	 */
	public boolean isLinux() {
		return os == OS.LINUX;
	}

	/**
	 * @return Whether the operating system is Mac.
	 */
	public boolean isMac() {
		return os == OS.MAC_OS;
	}

	/**
	 * @return Whether the operating system is Windows.
	 */
	public boolean isWindows() {
		return os == OS.WINDOWS;
	}

	@Override
	public String toString() {
		return String.format("Platform[os=%s,architecture=%s]", os, architecture);
	}
}
