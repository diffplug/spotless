/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.biome;

/**
 * Settings and constants for Biome to use.
 */
public final class BiomeSettings {
	private final static String CONFIG_NAME = "biome.json";
	private final static String DEFAULT_VERSION = "1.2.0";
	private final static String DOWNLOAD_FILE_PATTERN = "biome-%s-%s-%s";
	private final static String SHORT_NAME = "biome";
	private final static String URL_PATTERN_1X = "https://github.com/biomejs/biome/releases/download/cli%%2Fv%s/biome-%s";
	private final static String URL_PATTERN_2X = "https://github.com/biomejs/biome/releases/download/%%40biomejs%%2Fbiome%%40%s/biome-%s";

	private BiomeSettings() {}

	/**
	 * @return The name of the default config file.
	 */
	public static String configName() {
		return CONFIG_NAME;
	}

	/**
	 * @return Default version to use when no version was set explicitly.
	 */
	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	/**
	 * @return The pattern for {@link String#format(String, Object...)
	 *         String.format()} for the file name of a Biome executable for a
	 *         certain version and architecture. The first parameter is the platform,
	 *         the second is the OS, the third is the architecture.
	 */
	public static String getDownloadFilePattern() {
		return DOWNLOAD_FILE_PATTERN;
	}

	/**
	 * @param version The biome version for which to get the URL pattern, e.g. 1.2.0 or 2.0.6.
	 * @return The pattern for {@link String#format(String, Object...)
	 *         String.format()} for the URL where the executables can be downloaded.
	 *         The first parameter is the version, the second parameter is the OS /
	 *         platform.
	 */
	public static String getUrlPattern(String version) {
		if (version != null && version.startsWith("1.")) {
			return URL_PATTERN_1X;
		}
		return URL_PATTERN_2X;
	}

	/**
	 * @return The short name of this flavor, e.g. <code>biome</code>.
	 */
	public static String shortName() {
		return SHORT_NAME;
	}
}
