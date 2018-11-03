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
package com.diffplug.spotless.js;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Common utilities for JavaScript */
public class JsDefaults {
	//Prevent instantiation
	private JsDefaults() {};

	/**
	 * Filter based on Eclipse-WTP <code>org.eclipse.core.contenttype.contentTypes</code>
	 * extension <code>org.eclipse.wst.jsdt.core</code>.
	 */
	public static final List<String> FILE_FILTER = Collections.unmodifiableList(
			Arrays.asList("**/*.js"));

	/**
	 * Valid JS should start with variable/function definitions, closures or no-ops.
	 * Initial comment (one or multi-line) is considered header.
	 */
	public static final String DELIMITER_EXPR = "[A-Za-z\\{;]+";
}
