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

import java.util.function.Supplier;

public class LineEndingService {

	private Supplier<String> lineSeparator = () -> System.lineSeparator();

	void setLineSeparator(Supplier<String> lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	public LineEnding determineLineEnding(String raw) {
		return determineLineEnding(raw, LineEnding.UNCERTAIN);
	}

	private LineEnding determineLineEnding(String raw, LineEnding fallback) {
		int firstCR = raw.indexOf("\r");
		int firstLF = raw.indexOf("\n");

		if (isWindows(firstCR, firstLF)) {
			return LineEnding.WINDOWS;
		}

		if (isUnix(firstCR, firstLF)) {
			return LineEnding.UNIX;
		}

		return fallback;
	}

	private static boolean isWindows(int firstCR, int firstLF) {
		if (firstCR == -1 && firstLF == -1) {
			return false;
		}
		return firstCR + 1 == firstLF;
	}

	private static boolean isUnix(int firstCR, int firstLF) {
		if (firstLF == -1) {
			return false;
		}
		if (firstCR == -1) {
			return true;
		}
		return firstCR > firstLF;
	}

	public LineEnding getPlatformLineEnding() {
		String separator = getPlatformLineSeparator();

		switch (separator) {
		case "\r\n":
			return LineEnding.WINDOWS;
		case "\n":
			return LineEnding.UNIX;
		default:
			throw new InternalError("Shouldn't happen.");
		}
	}

	String getPlatformLineSeparator() {
		String separator = lineSeparator.get();

		if ("\r\n".equals(separator) || "\n".equals(separator)) {
			return separator;
		}

		throw new UnsupportedOperationException("Determined native line separator is not supported!");
	}

}
