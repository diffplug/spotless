package com.diffplug.spotless;

/**
 * Enumeration of possible computer operation systems.
 */
public enum OS {
	Windows, Mac, Linux, SunOS, AIX;

	/**
	 * Attempts to guess the OS of the environment running the JVM.
	 * 
	 * @return The best guess for the architecture.
	 */
	public static OS guess() {
		var osName = System.getProperty("os.name");
		return osName.contains("Windows") ? OS.Windows
				: osName.contains("Mac") ? OS.Mac
						: osName.contains("SunOS") ? OS.SunOS
								: osName.toUpperCase().contains("AIX") ? OS.AIX : OS.Linux;
	}
}