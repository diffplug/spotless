/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.glue.java;

import com.diffplug.spotless.LineEnding;

public class GoogleJavaFormatUtils {

	private static final boolean IS_WINDOWS = LineEnding.PLATFORM_NATIVE.str().equals("\r\n");

	/**
	 * google-java-format-1.1's removeUnusedImports does *wacky* stuff on Windows.
	 * The beauty of normalizing all line endings to unix!
	 */
	static String fixWindowsBug(String input, String version) {
		if (IS_WINDOWS && version.equals("1.1")) {
			int firstImport = input.indexOf("\nimport ");
			if (firstImport == 0) {
				return input;
			} else if (firstImport > 0) {
				int numToTrim = 0;
				char prevChar;
				do {
					++numToTrim;
					prevChar = input.charAt(firstImport - numToTrim);
				} while (Character.isWhitespace(prevChar) && (firstImport - numToTrim) > 0);
				if (firstImport - numToTrim == 0) {
					// import was the very first line, and we'd like to maintain a one-line gap
					++numToTrim;
				} else if (prevChar == ';' || prevChar == '/') {
					// import came after either license or a package declaration
					--numToTrim;
				}
				if (numToTrim > 0) {
					return input.substring(0, firstImport - numToTrim + 2) + input.substring(firstImport + 1);
				}
			}
		}
		return input;
	}
}
