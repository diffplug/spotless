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
import java.util.Objects;

public class AdocfmtConfig implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	public boolean normalizeSetextHeadings = false;
	public boolean collapseConsecutiveBlankLines = true;
	public boolean oneSentencePerLine = false;
	public boolean normalizeBlockDelimiters = true;
	public boolean removeTrailingHeaderEqualsSign = true;
	public boolean titleCase = false;
	public boolean removeTrailingWhitespace = true;
	public boolean normalizeListBullets = false;
	public boolean normalizeOrderedListMarkers = false;
	public boolean ensureHeadingBlankLines = true;
	public boolean ensureSourceDelimiters = false;

	public AdocfmtConfig() {}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AdocfmtConfig other))
			return false;
		return normalizeSetextHeadings == other.normalizeSetextHeadings
				&& collapseConsecutiveBlankLines == other.collapseConsecutiveBlankLines
				&& oneSentencePerLine == other.oneSentencePerLine
				&& normalizeBlockDelimiters == other.normalizeBlockDelimiters
				&& removeTrailingHeaderEqualsSign == other.removeTrailingHeaderEqualsSign
				&& titleCase == other.titleCase
				&& removeTrailingWhitespace == other.removeTrailingWhitespace
				&& normalizeListBullets == other.normalizeListBullets
				&& normalizeOrderedListMarkers == other.normalizeOrderedListMarkers
				&& ensureHeadingBlankLines == other.ensureHeadingBlankLines
				&& ensureSourceDelimiters == other.ensureSourceDelimiters;
	}

	@Override
	public int hashCode() {
		return Objects.hash(normalizeSetextHeadings, collapseConsecutiveBlankLines, oneSentencePerLine,
				normalizeBlockDelimiters, removeTrailingHeaderEqualsSign, titleCase, removeTrailingWhitespace,
				normalizeListBullets, normalizeOrderedListMarkers, ensureHeadingBlankLines, ensureSourceDelimiters);
	}
}
