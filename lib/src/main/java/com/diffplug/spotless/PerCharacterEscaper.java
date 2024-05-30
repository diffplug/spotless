/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless;

class PerCharacterEscaper {
	/**
	 * If your escape policy is "'a1b2c3d", it means this:
	 *
	 * ```
	 * abc->abc
	 * 123->'b'c'd
	 * I won't->I won'at
	 * ```
	 */
	public static PerCharacterEscaper specifiedEscape(String escapePolicy) {
		int[] codePoints = escapePolicy.codePoints().toArray();
		if (codePoints.length % 2 != 0) {
			throw new IllegalArgumentException();
		}
		int escapeCodePoint = codePoints[0];
		int[] escapedCodePoints = new int[codePoints.length / 2];
		int[] escapedByCodePoints = new int[codePoints.length / 2];
		for (int i = 0; i < escapedCodePoints.length; ++i) {
			escapedCodePoints[i] = codePoints[2 * i];
			escapedByCodePoints[i] = codePoints[2 * i + 1];
		}
		return new PerCharacterEscaper(escapeCodePoint, escapedCodePoints, escapedByCodePoints);
	}

	private final int escapeCodePoint;
	private final int[] escapedCodePoints;
	private final int[] escapedByCodePoints;

	/** The first character in the string will be uses as the escape character, and all characters will be escaped. */
	private PerCharacterEscaper(int escapeCodePoint, int[] escapedCodePoints, int[] escapedByCodePoints) {
		this.escapeCodePoint = escapeCodePoint;
		this.escapedCodePoints = escapedCodePoints;
		this.escapedByCodePoints = escapedByCodePoints;
	}

	public boolean needsEscaping(String input) {
		return firstOffsetNeedingEscape(input) != -1;
	}

	private int firstOffsetNeedingEscape(String input) {
		final int length = input.length();
		int firstOffsetNeedingEscape = -1;
		outer: for (int offset = 0; offset < length;) {
			int codepoint = input.codePointAt(offset);
			for (int escaped : escapedCodePoints) {
				if (codepoint == escaped) {
					firstOffsetNeedingEscape = offset;
					break outer;
				}
			}
			offset += Character.charCount(codepoint);
		}
		return firstOffsetNeedingEscape;
	}

	public String escape(String input) {
		final int noEscapes = firstOffsetNeedingEscape(input);
		if (noEscapes == -1) {
			return input;
		} else {
			final int length = input.length();
			final int needsEscapes = length - noEscapes;
			StringBuilder builder = new StringBuilder(noEscapes + 4 + (needsEscapes * 5 / 4));
			builder.append(input, 0, noEscapes);
			for (int offset = noEscapes; offset < length;) {
				final int codepoint = input.codePointAt(offset);
				offset += Character.charCount(codepoint);
				int idx = indexOf(escapedCodePoints, codepoint);
				if (idx == -1) {
					builder.appendCodePoint(codepoint);
				} else {
					builder.appendCodePoint(escapeCodePoint);
					builder.appendCodePoint(escapedByCodePoints[idx]);
				}
			}
			return builder.toString();
		}
	}

	private int firstOffsetNeedingUnescape(String input) {
		final int length = input.length();
		int firstOffsetNeedingEscape = -1;
		for (int offset = 0; offset < length;) {
			int codepoint = input.codePointAt(offset);
			if (codepoint == escapeCodePoint) {
				firstOffsetNeedingEscape = offset;
				break;
			}
			offset += Character.charCount(codepoint);
		}
		return firstOffsetNeedingEscape;
	}

	public String unescape(String input) {
		final int noEscapes = firstOffsetNeedingUnescape(input);
		if (noEscapes == -1) {
			return input;
		} else {
			final int length = input.length();
			final int needsEscapes = length - noEscapes;
			StringBuilder builder = new StringBuilder(noEscapes + 4 + (needsEscapes * 5 / 4));
			builder.append(input, 0, noEscapes);
			for (int offset = noEscapes; offset < length;) {
				int codepoint = input.codePointAt(offset);
				offset += Character.charCount(codepoint);
				// if we need to escape something, escape it
				if (codepoint == escapeCodePoint) {
					if (offset < length) {
						codepoint = input.codePointAt(offset);
						int idx = indexOf(escapedByCodePoints, codepoint);
						if (idx != -1) {
							codepoint = escapedCodePoints[idx];
						}
						offset += Character.charCount(codepoint);
					} else {
						throw new IllegalArgumentException("Escape character '" + new String(new int[]{escapeCodePoint}, 0, 1) + "' can't be the last character in a string.");
					}
				}
				// we didn't escape it, append it raw
				builder.appendCodePoint(codepoint);
			}
			return builder.toString();
		}
	}

	private static int indexOf(int[] array, int value) {
		for (int i = 0; i < array.length; ++i) {
			if (array[i] == value) {
				return i;
			}
		}
		return -1;
	}
}
