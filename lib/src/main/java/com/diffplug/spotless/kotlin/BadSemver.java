/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.kotlin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BadSemver {
	protected static int version(String input) {
		Matcher matcher = BAD_SEMVER.matcher(input);
		if (!matcher.find() || matcher.start() != 0) {
			throw new IllegalArgumentException("Version must start with " + BAD_SEMVER.pattern());
		}
		String major = matcher.group(1);
		String minor = matcher.group(2);
		String patch = matcher.group(3);
		return version(Integer.parseInt(major), Integer.parseInt(minor), patch != null ? Integer.parseInt(patch) : 0);
	}

	/** Ambiguous after 2147.483647.blah-blah */
	protected static int version(int major, int minor, int patch) {
		return major * 1_000_000 + minor * 1_000 + patch;
	}

	protected static int version(int major, int minor) {
		return version(major, minor, 0);
	}

	private static final Pattern BAD_SEMVER = Pattern.compile("(\\d+)\\.(\\d+)\\.*(\\d+)*");

	private BadSemver() {}
}
