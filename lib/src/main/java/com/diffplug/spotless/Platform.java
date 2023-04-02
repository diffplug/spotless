package com.diffplug.spotless;

/**
 * Represents a platform where code is run, consisting of an operating system
 * and an architecture.
 */
public class Platform {
	/**
	 * Attempts to guess the platform of the hosting environment running the JVM
	 * machine.
	 * 
	 * @return The best guess for the current OS and architecture.
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
	 * @param os Operating system of the platform.
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
	 * @return Whether the operating system is Aix.
	 */
	public boolean isAix() {
		return os == OS.AIX;
	}

	/**
	 * @return Whether the operating system is Linux.
	 */
	public boolean isLinux() {
		return os == OS.Linux;
	}

	/**
	 * @return Whether the operating system is Mac.
	 */
	public boolean isMac() {
		return os == OS.Mac;
	}

	/**
	 * @return Whether the operating system is SunOS.
	 */
	public boolean isSunos() {
		return os == OS.SunOS;
	}

	/**
	 * @return Whether the operating system is Windows.
	 */
	public boolean isWindows() {
		return os == OS.Windows;
	}

	@Override
	public String toString() {
		return String.format("Platform[os=%s,architecture=%s]", os, architecture);
	}
}
