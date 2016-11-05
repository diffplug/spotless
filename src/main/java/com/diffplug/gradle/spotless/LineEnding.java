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
package com.diffplug.gradle.spotless;

import java.io.File;

import com.diffplug.common.base.StandardSystemProperty;

/**
 * Represents the line endings which should be written by the tool.
 */
public enum LineEnding {
	// @formatter:off
	/** Uses the same line endings as Git, using `.gitattributes` and the `core.eol` property. */
	GIT_ATTRIBUTES,
	/** `\n` on unix systems, `\r\n` on windows systems. */
	PLATFORM_NATIVE,
	/** `\r\n` */
	WINDOWS,
	/** `\n` */
	UNIX;
	// @formatter:on

	/** Returns a {@link Policy} appropriate for files which are contained within the given rootFolder. */
	public Policy createPolicy(File rootFolder) {
		if (this == GIT_ATTRIBUTES) {
			return GitAttributesLineEndings.create(rootFolder);
		} else {
			return createPolicy();
		}
	}

	/** Should use {@link #createPolicy(File)} instead, but this will work iff its a path-independent LineEnding policy. */
	public Policy createPolicy() {
		switch (this) {
		case PLATFORM_NATIVE:
			return file -> _platformNative;
		case WINDOWS:
			return file -> WINDOWS.str();
		case UNIX:
			return UNIX_POLICY;
		default:
			throw new UnsupportedOperationException(this + " is a path-specific line ending.");
		}
	}

	static final Policy UNIX_POLICY = file -> UNIX.str();

	/** Returns the standard line ending for this policy. */
	public String str() {
		switch (this) {
		case PLATFORM_NATIVE:
			return _platformNative;
		case WINDOWS:
			return "\r\n";
		case UNIX:
			return "\n";
		default:
			throw new UnsupportedOperationException(this + " is a path-specific line ending.");
		}
	}

	private static final String _platformNative = StandardSystemProperty.LINE_SEPARATOR.value();

	/** A policy for line endings which can vary based on the specific file being requested. */
	public interface Policy {
		/** Returns the line ending appropriate for the given file. */
		String getEndingFor(File file);

		/** Returns true iff this file has unix line endings. */
		default boolean isUnix(File file) {
			String ending = getEndingFor(file);
			return ending.equals(UNIX.str());
		}
	}

	/** Returns a string with exclusively unix line endings. */
	public static String toUnix(String input) {
		return input.replace("\r", "");
	}
}
