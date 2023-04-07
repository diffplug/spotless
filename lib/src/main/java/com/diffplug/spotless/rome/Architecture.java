package com.diffplug.spotless.rome;

/**
 * Enumeration of possible computer architectures.
 */
public enum Architecture {
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
