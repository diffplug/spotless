/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.asciidoc;

import java.io.Serial;
import java.io.Serializable;

public class AsciidocFormatterConfig implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private boolean normalizeSetextHeadings = true;
	private boolean collapseConsecutiveBlankLines = true;
	private boolean oneSentencePerLine = true;
	private boolean normalizeBlockDelimiters = true;
	private boolean removeTrailingHeaderEqualsSign = true;
	private boolean titleCase = false;
	private boolean removeTrailingWhitespace = true;
	private boolean normalizeListBullets = false;
	private boolean normalizeOrderedListMarkers = false;
	private boolean ensureHeadingBlankLines = true;
	private boolean ensureSourceDelimiters = false;

	public boolean isNormalizeSetextHeadings() {
		return normalizeSetextHeadings;
	}

	public void setNormalizeSetextHeadings(boolean normalizeSetextHeadings) {
		this.normalizeSetextHeadings = normalizeSetextHeadings;
	}

	public boolean isCollapseConsecutiveBlankLines() {
		return collapseConsecutiveBlankLines;
	}

	public void setCollapseConsecutiveBlankLines(boolean collapseConsecutiveBlankLines) {
		this.collapseConsecutiveBlankLines = collapseConsecutiveBlankLines;
	}

	public boolean isOneSentencePerLine() {
		return oneSentencePerLine;
	}

	public void setOneSentencePerLine(boolean oneSentencePerLine) {
		this.oneSentencePerLine = oneSentencePerLine;
	}

	public boolean isNormalizeBlockDelimiters() {
		return normalizeBlockDelimiters;
	}

	public void setNormalizeBlockDelimiters(boolean normalizeBlockDelimiters) {
		this.normalizeBlockDelimiters = normalizeBlockDelimiters;
	}

	public boolean isRemoveTrailingHeaderEqualsSign() {
		return removeTrailingHeaderEqualsSign;
	}

	public void setRemoveTrailingHeaderEqualsSign(boolean removeTrailingHeaderEqualsSign) {
		this.removeTrailingHeaderEqualsSign = removeTrailingHeaderEqualsSign;
	}

	public boolean isTitleCase() {
		return titleCase;
	}

	public void setTitleCase(boolean titleCase) {
		this.titleCase = titleCase;
	}

	public boolean isRemoveTrailingWhitespace() {
		return removeTrailingWhitespace;
	}

	public void setRemoveTrailingWhitespace(boolean removeTrailingWhitespace) {
		this.removeTrailingWhitespace = removeTrailingWhitespace;
	}

	public boolean isNormalizeListBullets() {
		return normalizeListBullets;
	}

	public void setNormalizeListBullets(boolean normalizeListBullets) {
		this.normalizeListBullets = normalizeListBullets;
	}

	public boolean isNormalizeOrderedListMarkers() {
		return normalizeOrderedListMarkers;
	}

	public void setNormalizeOrderedListMarkers(boolean normalizeOrderedListMarkers) {
		this.normalizeOrderedListMarkers = normalizeOrderedListMarkers;
	}

	public boolean isEnsureHeadingBlankLines() {
		return ensureHeadingBlankLines;
	}

	public void setEnsureHeadingBlankLines(boolean ensureHeadingBlankLines) {
		this.ensureHeadingBlankLines = ensureHeadingBlankLines;
	}

	public boolean isEnsureSourceDelimiters() {
		return ensureSourceDelimiters;
	}

	public void setEnsureSourceDelimiters(boolean ensureSourceDelimiters) {
		this.ensureSourceDelimiters = ensureSourceDelimiters;
	}
}
