/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.groovy;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

/**
 * Removes all semicolons from the end of lines.
 *
 * @author Jose Luis Badano
 */
public final class RemoveSemicolonsStep {

	private RemoveSemicolonsStep() {
		// prevent instantiation
	}

	static final String NAME = "Remove unnecessary semicolons";

	public static FormatterStep create() {
		return FormatterStep.createLazy(NAME,
				State::new,
				RemoveSemicolonsStep.State::toFormatter);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		FormatterFunc toFormatter() {
			return raw -> {
				try (BufferedReader reader = new BufferedReader(new StringReader(raw))) {
					StringBuilder result = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						result.append(removeSemicolon(line));
						result.append(System.lineSeparator());
					}
					return result.toString();
				}
			};
		}

		/**
		 * Removes the last semicolon in a line if it exists.
		 *
		 * @param line the line to remove the semicolon from
		 * @return the line without the last semicolon
		 */
		private String removeSemicolon(String line) {
			// find last semicolon in a string a remove it
			int lastSemicolon = line.lastIndexOf(";");
			if (lastSemicolon != -1 && lastSemicolon == line.length() - 1) {
				return line.substring(0, lastSemicolon);
			} else {
				return line;
			}
		}
	}
}
