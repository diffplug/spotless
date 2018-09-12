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
package com.diffplug.spotless.npm;

import java.util.Arrays;

public enum TsConfigFileType {
	TSCONFIG, TSLINT, VSCODE, TSFMT;

	public static TsConfigFileType forNameIgnoreCase(String name) {
		return Arrays.stream(values())
				.filter(type -> type.name().equalsIgnoreCase(name))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Config file type " + name + " is not supported. Supported values (case is ignored): " + Arrays.toString(values())));
	}
}
