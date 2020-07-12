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

import com.diffplug.common.base.StandardSystemProperty;

public enum JreVersion {
	_8, _11;

	public static JreVersion thisVm() {
		String jvmVersion = StandardSystemProperty.JAVA_VERSION.value();
		if (jvmVersion.startsWith("1.8.")) {
			return _8;
		} else if (jvmVersion.startsWith("11.")) {
			return _11;
		} else {
			int version = Integer.parseInt(jvmVersion.substring(0, jvmVersion.indexOf('.')));
			JreVersion result = version > 11 ? _11 : _8;
			System.err.println("WARNING: Only JRE 8 and 11 are officially supported, pretending unsupported version " + jvmVersion + " is JDK" + result.name());
			return result;
		}
	}
}
