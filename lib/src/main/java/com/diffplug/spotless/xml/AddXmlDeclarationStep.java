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
package com.diffplug.spotless.xml;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

public class AddXmlDeclarationStep {

	public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	public static FormatterStep create() {
		return FormatterStep.create("addXmlDeclaration",
				AddXmlDeclarationStep.class,
				unused -> AddXmlDeclarationStep::format);
	}

	private static String format(String rawString) {
		String replacement = rawString;
		if (!(rawString.startsWith(XML_DECLARATION))) {
			replacement = XML_DECLARATION + LineEnding.UNIX.str() + replacement;
		}
		return replacement;
	}
}
