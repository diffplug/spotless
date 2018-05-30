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
package com.diffplug.spotless.extra.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

/** Semantic version configuration */
class SemanticVersion implements Serializable, Comparable<SemanticVersion> {

	public static final String FORMAT = "<major>[.minor][.patch]";
	private static final int MAJOR = 0;
	private static final int MINOR = 1;
	private static final int PATCH = 2;

	// Not used, only the serialization output is required to determine whether the object has changed
	private static final long serialVersionUID = 1L;

	private final int[] version;

	/** Convert string into semantic version */
	SemanticVersion(final String version) {
		if (null == version) {
			throw new UserArgumentException(null, "Version information");
		}
		LinkedList<String> versionParts = new LinkedList<String>(
				Arrays.asList(version.split("\\.", -1)));
		if (versionParts.size() <= MAJOR || versionParts.size() > PATCH + 1) {
			throw new UserArgumentException(version,
					String.format("Value does not corrsepond to the format '%s'.", FORMAT));
		}
		while (versionParts.size() <= PATCH) {
			versionParts.addLast("0");
		}
		this.version = new int[PATCH + 1];
		for (int i = MAJOR; i <= PATCH; i++) {
			this.version[i] = convertVersionNumber(version, versionParts.poll());
		}
	}

	private static int convertVersionNumber(String version, String versionPart) {
		versionPart = versionPart.trim();
		if (versionPart.isEmpty()) {
			throw new UserArgumentException(version,
					String.format("Version format is '%s'. The major version is mandatory. None of the version information after a ' must be empty.", FORMAT));
		}
		try {
			int versionInformation = Integer.parseInt(versionPart);
			if (versionInformation < 0) {
				throw new NumberFormatException("Value is negative.");
			}
			return versionInformation;
		} catch (NumberFormatException e) {
			throw new UserArgumentException(Optional.of(version),
					String.format("Version information '%s' is not a valid number.", versionPart), e);
		}
	}

	@Override
	public String toString() {
		return String.format("%d.%d.%d", version[MAJOR], version[MINOR], version[PATCH]);
	}

	@Override
	public int compareTo(SemanticVersion o) {
		int result = 0;
		for (int i = MAJOR; i <= PATCH; i++) {
			result = Integer.signum(this.version[i] - o.version[i]);
			if (0 != result) {
				break;
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SemanticVersion) {
			return 0 == compareTo((SemanticVersion) obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(version[MAJOR], version[MINOR], version[PATCH]);
	}

}
