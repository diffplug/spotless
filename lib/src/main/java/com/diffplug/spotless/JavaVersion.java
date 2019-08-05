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

/**
 * Helper class providing Java version information
 *
 */
public final class JavaVersion {

	private static final String JAVA_VERSION;

	private static final int MAJOR_VERSION;

	static {
		JAVA_VERSION = System.getProperty("java.version");
		final String[] versionStrings = JAVA_VERSION.split("\\.");
		int major = Integer.parseInt(versionStrings[0]);
		// Java version prior to 10 used 1.x versioning
		if (major == 1) {
			major = Integer.parseInt(versionStrings[1]);
		}
		MAJOR_VERSION = major;
	}

	/**
	 * @return the full Java version (e.g. 1.8.0_211)
	 */
	public static String getJavaVersion() {
		return JAVA_VERSION;
	}

	/**
	 * @return the major version of the java release (e.g. 8 or 11)
	 */
	public static int getMajorVersion() {
		return MAJOR_VERSION;
	}

}
