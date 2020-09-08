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

import com.diffplug.common.base.StandardSystemProperty;

public class JreVersion {
	private JreVersion() {}

	/** Returns the major version of this VM, e.g. 8, 9, 10, 11, 13, etc. */
	public static int thisVm() {
		String jvmVersion = StandardSystemProperty.JAVA_VERSION.value();
		if (jvmVersion.startsWith("1.8.")) {
			return 8;
		} else {
			return Integer.parseInt(jvmVersion.substring(0, jvmVersion.indexOf('.')));
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
}
