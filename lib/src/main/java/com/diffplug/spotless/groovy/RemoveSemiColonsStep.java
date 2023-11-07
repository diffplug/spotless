package com.diffplug.spotless.groovy;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;

/**
 * Removes all semicolons from the end of lines.
 *
 * @author Jose Luis Badano
 */
public final class RemoveSemiColonsStep {

	private RemoveSemiColonsStep() {
		// prevent instantiation
	}

	static final String NAME = "Remove unnecessary semicolons"; 

	public static FormatterStep create() {
		return FormatterStep.createLazy(NAME,
			State::new,
			RemoveSemiColonsStep.State::toFormatter);
	}


	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		FormatterFunc toFormatter() {
			return raw -> {
				try (BufferedReader reader = new BufferedReader(new StringReader(raw))) {
					StringBuilder result = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						result.append(removeSemiColon(line));
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
		private String removeSemiColon(String line) {
			// find last semicolon in a string a remove it
			int lastSemiColon = line.lastIndexOf(";");
			if (lastSemiColon != -1 && lastSemiColon == line.length() - 1) {
				return line.substring(0, lastSemiColon);
			} else {
				return line;
			}
		}
	}
}
