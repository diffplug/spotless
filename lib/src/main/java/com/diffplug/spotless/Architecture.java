package com.diffplug.spotless;

/**
 * Enumeration of possible computer architectures.
 *
 */
public enum Architecture {
	x86, x64, ppc64le, s390x, arm64, armv7l, ppc, ppc64;

	/**
	 * Attempts to guess the architecture of the environment running the JVM.
	 * 
	 * @return The best guess for the architecture.
	 */
	public static Architecture guess() {
		var arch = System.getProperty("os.arch");
		var version = System.getProperty("os.version");

		if (arch.equals("ppc64le")) {
			throw new IllegalArgumentException();
		} else if (arch.equals("aarch64")) {
			return arm64;
		} else if (arch.equals("s390x")) {
			return s390x;
		} else if (arch.equals("arm")) {
			if (version.contains("v7")) {
				return armv7l;
			} else {
				return arm64;
			}
		} else if (arch.equals("ppc64")) {
			return ppc64;
		} else if (arch.equals("ppc")) {
			return ppc;
		} else {
			return arch.contains("64") ? x64 : x86;
		}
	}
}
