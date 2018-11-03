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
package com.diffplug.spotless.json;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Common utilities for Json */
public class JsonDefaults {
	//Prevent instantiation
	private JsonDefaults() {};

	/**
	 * Filter based on Eclipse-WTP <code>org.eclipse.core.contenttype.contentTypes</code>
	 * extension <code>org.eclipse.wst.json.core</code>.
	 */
	public static final List<String> FILE_FILTER = Collections.unmodifiableList(
			Arrays.asList("**/*.json"));

	/**
	 * JSON itself does not accept comments. However, commenting JSON
	 * sources is good practice and projects like JSON.minify allow commenting.
	 * Hence also headers can be supported. Everything before the first object
	 * is considered to be part of the header.
	 */
	public static final String DELIMITER_EXPR = "\\{";
}
