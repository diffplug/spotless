/*
 * Copyright 2016-2020 DiffPlug
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

import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JreVersion {
	private JreVersion() {}

	/** Returns the major version of this VM, e.g. 8, 9, 10, 11, 13, etc. */
	public static int thisVm() {
		String jre = System.getProperty("java.version");
		if (jre.startsWith("1.8")) {
			return 8;
		} else {
			Matcher matcher = Pattern.compile("(\\d+)").matcher(jre);
			if (!matcher.find()) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer");
			}
			int version = Integer.parseInt(matcher.group(1));
			if (version <= 8) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer greater than 8");
			}
			return version;
		}
	}

	private static void assume(IntPredicate versionTest) {
		org.junit.Assume.assumeTrue(versionTest.test(thisVm()));
	}

	public static void assume11OrGreater() {
		assume(v -> v >= 11);
	}

	public static void assume11OrLess() {
		assume(v -> v <= 11);
	}

	public static void assumeLessThan15() {
		assume(v -> v < 15);
	}
}
