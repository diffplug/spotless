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
package com.diffplug.gradle.spotless;

public class FileEndingStep {
	private LineEnding lineEnding;
	private LineEndingService lineEndingService;
	private boolean clobber = true;

	public FileEndingStep(LineEnding lineEnding) {
		this(lineEnding, new LineEndingService());
	}

	FileEndingStep(LineEnding lineEnding, LineEndingService lineEndingService) {
		this.lineEnding = lineEnding;
		this.lineEndingService = lineEndingService;
	}

	public void disableClobber() {
		this.clobber = false;
	}

	public String format(String input) {
		return clobber ? formatWithClobber(input) : formatWithoutClobber(input);
	}

	public String formatWithClobber(String input) {
		String lineSeparator = lineEndingService.getLineSeparatorOrDefault(lineEnding, input);
		int indexOfLastNonWhitespaceCharacter = lineEndingService.indexOfLastNonWhitespaceCharacter(input);

		if (indexOfLastNonWhitespaceCharacter == -1) {
			return lineSeparator;
		}

		StringBuilder builder = new StringBuilder(indexOfLastNonWhitespaceCharacter + 2);
		builder.append(input, 0, indexOfLastNonWhitespaceCharacter + 1);
		builder.append(lineSeparator);
		return builder.toString();
	}

	public String formatWithoutClobber(String input) {
		String lineSeparator = lineEndingService.getLineSeparatorOrDefault(lineEnding, input);
		return input.endsWith(lineSeparator) ? input : input + lineSeparator;
	}

}
