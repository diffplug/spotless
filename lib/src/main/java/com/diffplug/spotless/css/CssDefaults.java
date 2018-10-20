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
package com.diffplug.spotless.css;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Common utilities for CSS */
public class CssDefaults {
	//Prevent instantiation
	private CssDefaults() {};

	/**
	 * Filter based on Eclipse-WTP <code>org.eclipse.core.contenttype.contentTypes</code>
	 * extension <code>org.eclipse.wst.css.core.csssource</code>.
	 */
	public static final List<String> FILE_FILTER = Collections.unmodifiableList(
			Arrays.asList("**/*.css"));

	/**
	 * Match line that starts with a selector. Selection is quite broad.
	 * Assure that multiline licenses have a proper indentation.
	 * Assure that your has been formatted before (no whitespace before first selector).
	 */
	public static final String DELIMITER_EXPR = "[A-Za-z\\.\\#]+";
}
