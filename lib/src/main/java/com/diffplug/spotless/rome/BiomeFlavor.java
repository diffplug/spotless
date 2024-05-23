/*
 * Copyright 2023-2024 DiffPlug
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
 * The flavor of Biome to use. Exists for compatibility reason, may be removed
 * shortly.
 * <p>
 * Will be removed once the old Rome project is not supported anymore.
 */
public enum BiomeFlavor {
	/** The new forked Biome project. */
	BIOME("biome", "1.2.0", "biome.json", "biome-%s-%s-%s",
			"https://github.com/biomejs/biome/releases/download/cli%%2Fv%s/biome-%s");

	private final String configName;
	private final String defaultVersion;
	private final String downloadFilePattern;
	private final String shortName;
	private final String urlPattern;

	BiomeFlavor(String shortName, String defaultVersion, String configName, String downloadFilePattern,
			String urlPattern) {
		this.shortName = shortName;
		this.defaultVersion = defaultVersion;
		this.configName = configName;
		this.downloadFilePattern = downloadFilePattern;
		this.urlPattern = urlPattern;
	}

	/**
	 * @return The name of the default config file.
	 */
	public String configName() {
		return configName;
	}

	/**
	 * @return Default version to use when no version was set explicitly.
	 */
	public String defaultVersion() {
		return defaultVersion;
	}

	/**
	 * @return The pattern for {@link String#format(String, Object...)
	 *         String.format()} for the file name of a Biome executable for a
	 *         certain version and architecure. The first parameter is the platform,
	 *         the second is the OS, the third is the architecture.
	 */
	public String getDownloadFilePattern() {
		return downloadFilePattern;
	}

	/**
	 * @return The pattern for {@link String#format(String, Object...)
	 *         String.format()} for the URL where the executables can be downloaded.
	 *         The first parameter is the version, the second parameter is the OS /
	 *         platform.
	 */
	public String getUrlPattern() {
		return urlPattern;
	}

	/**
	 * @return The short name of this flavor, i.e. <code>rome</code> or
	 *         <code>biome</code>.
	 */
	public String shortName() {
		return shortName;
	}
}
