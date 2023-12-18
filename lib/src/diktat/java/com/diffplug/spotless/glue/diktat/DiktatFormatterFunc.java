/*
 * Copyright 2021-2023 DiffPlug
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
package com.diffplug.spotless.glue.diktat;

import java.io.File;

import com.diffplug.spotless.glue.diktat.compat.DiktatCompat1Dot2Dot5Adapter;
import com.diffplug.spotless.glue.diktat.compat.DiktatCompat2Dot0Dot0Adapter;
import com.diffplug.spotless.glue.diktat.compat.DiktatCompatAdapter;

import com.diffplug.spotless.FormatterFunc;

public class DiktatFormatterFunc implements FormatterFunc.NeedsFile {
	private final DiktatCompatAdapter adapter;
	private final boolean isScript;

	public DiktatFormatterFunc(
		String version,
		File configFile,
		boolean isScript
	) {
		if (version.startsWith("1.")) {
			this.adapter = new DiktatCompat1Dot2Dot5Adapter(configFile);
		} else {
			this.adapter = new DiktatCompat2Dot0Dot0Adapter(configFile);
		}
		this.isScript = isScript;
	}

	@Override
	public String applyWithFile(String unix, File file) {
		return adapter.format(file, unix, isScript);
	}
}
