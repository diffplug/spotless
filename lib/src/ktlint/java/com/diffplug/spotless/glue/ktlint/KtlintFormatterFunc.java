/*
 * Copyright 2021-2022 DiffPlug
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
package com.diffplug.spotless.glue.ktlint;

import java.io.File;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.glue.ktlint.compat.KtLintCompat0Dot31Dot0Adapter;
import com.diffplug.spotless.glue.ktlint.compat.KtLintCompat0Dot32Dot0Adapter;
import com.diffplug.spotless.glue.ktlint.compat.KtLintCompat0Dot34Dot2Adapter;
import com.diffplug.spotless.glue.ktlint.compat.KtLintCompat0Dot45Dot2Adapter;
import com.diffplug.spotless.glue.ktlint.compat.KtLintCompat0Dot46Dot0Adapter;
import com.diffplug.spotless.glue.ktlint.compat.KtLintCompat0Dot47Dot0Adapter;
import com.diffplug.spotless.glue.ktlint.compat.KtLintCompatAdapter;

public class KtlintFormatterFunc implements FormatterFunc.NeedsFile {

	private final Map<String, String> userData;
	private final boolean isScript;
	@NotNull
	private final KtLintCompatAdapter adapter;
	private final boolean useExperimental;
	private final Map<String, Object> editorConfigOverrideMap;

	public KtlintFormatterFunc(String version, boolean isScript, boolean useExperimental, Map<String, String> userData,
			Map<String, Object> editorConfigOverrideMap) {
		int minorVersion = Integer.parseInt(version.split("\\.")[1]);
		if (version.equals("0.45.2")) {
			this.adapter = new KtLintCompat0Dot45Dot2Adapter();
		} else if (minorVersion >= 47) {
			this.adapter = new KtLintCompat0Dot47Dot0Adapter();
		} else if (minorVersion >= 46) {
			this.adapter = new KtLintCompat0Dot46Dot0Adapter();
		} else if (minorVersion >= 34) {
			this.adapter = new KtLintCompat0Dot34Dot2Adapter();
		} else if (minorVersion >= 32) {
			this.adapter = new KtLintCompat0Dot32Dot0Adapter();
		} else {
			this.adapter = new KtLintCompat0Dot31Dot0Adapter();
		}
		this.useExperimental = useExperimental;
		this.editorConfigOverrideMap = editorConfigOverrideMap;
		this.userData = userData;
		this.isScript = isScript;
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		return adapter.format(unix, file.getName(), isScript, useExperimental, userData, editorConfigOverrideMap);
	}
}
