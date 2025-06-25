/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.generic;

import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;

import java.io.File;

import com.diffplug.spotless.FormatterStep;

public final class ReplaceObsoletesStep implements FormatterStep {

	private static final long serialVersionUID = -6643164760547140534L;

	private ReplaceObsoletesStep() {}

	public static FormatterStep forJava() {
		return new ReplaceObsoletesStep();
	}

	@Override
	public String getName() {
		return "replaceObsoletes";
	}

	@Override
	public String format(String rawUnix, File file) throws Exception {
		return removeRedundantAbstractInInterfaces(
				removeRedundantPublicStaticInEnumsAndInterfaces(
						removeRedundantInitializations("float|double", "0(?:\\.0)?(?:f|d)?",
								removeRedundantInitializations("int|long|short|byte", "0(?:L)?",
										removeRedundantInitializations("String|\\w+", "null",
												removeRedundantInitializations("boolean", "false",
														replaceLineSeparator(rawUnix)))))));
	}

	private static String replaceLineSeparator(String input) {
		return compile(
				"System\\.getProperty\\(\"line\\.separator\"(?:\\s*,\\s*\"\\\\n\")?\\)|" +
						"String\\s+\\w+\\s*=\\s*System\\.getProperty\\(\"line\\.separator\"(?:\\s*,\\s*\"\\\\n\")?\\)\\s*;",
				MULTILINE)
				.matcher(input)
				.replaceAll(match -> match.group().contains("String")
						? "String " + match.group().split("\\s+")[1].split("=")[0].trim() + " = System.lineSeparator();"
						: "System.lineSeparator()");
	}

	private String removeRedundantInitializations(String typePattern, String defaultValuePattern, String input) {
		return compile(
				"([a-zA-Z]+\\s+(?:" + typePattern + ")\\s+\\w+)\\s*=\\s*" + defaultValuePattern + "\\s*;",
				MULTILINE)
				.matcher(input)
				.replaceAll("$1;");
	}

	private String removeRedundantPublicStaticInEnumsAndInterfaces(String input) {
		// Handle enum constants
		String processed = compile(
				"(enum\\s+\\w+\\s*\\{[^}]*?)(public\\s+static\\s+)(\\w+\\s*,\\s*|\\w+\\s*;)",
				MULTILINE)
				.matcher(input)
				.replaceAll("$1$3");

		// Handle enum methods
		processed = compile(
				"(enum\\s+\\w+\\s*\\{[^}]*?)(public\\s+static\\s+)(\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*(?:throws\\s+[\\w.]+(?:\\s*,\\s*[\\w.]+)*\\s*)?\\{)",
				MULTILINE)
				.matcher(processed)
				.replaceAll("$1$3");

		// Handle interface constants - keep 'final' but remove 'public static'
		processed = compile(
				"(interface\\s+\\w+\\s*\\{[^}]*?)(public\\s+static\\s+)(final\\s+)(\\w+\\s+\\w+\\s*=)",
				MULTILINE)
				.matcher(processed)
				.replaceAll("$1$3$4");

		// Handle interface methods
		processed = compile(
				"(interface\\s+\\w+\\s*\\{[^}]*?)(public\\s+static\\s+)(\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*(?:throws\\s+[\\w.]+(?:\\s*,\\s*[\\w.]+)*\\s*)?;)",
				MULTILINE)
				.matcher(processed)
				.replaceAll("$1$3");

		// Handle interface inner classes
		processed = compile(
				"(interface\\s+\\w+\\s*\\{[^}]*?)(public\\s+static\\s+)(class\\s+\\w+\\s*\\{)",
				MULTILINE)
				.matcher(processed)
				.replaceAll("$1$3");

		return processed;
	}

	private String removeRedundantAbstractInInterfaces(String input) {
		// Handle abstract methods in interfaces
		return compile(
				"(interface\\s+\\w+\\s*\\{[^}]*?)(?:public\\s+)?abstract\\s+(\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*(?:throws\\s+[\\w.]+(?:\\s*,\\s*[\\w.]+)*\\s*)?;)",
				MULTILINE)
				.matcher(input)
				.replaceAll("$1$2");
	}

	@Override
	public void close() throws Exception {}
}
