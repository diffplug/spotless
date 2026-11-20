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
package com.diffplug.spotless.maven.report;

final class Html {
	private Html() {}

	/**
	 * Escapes text for HTML element content and quoted attribute values. The result is pure ASCII: {@code $} and
	 * every non-ASCII character become numeric references. That's because the site renderer replaces
	 * {@code $relativePath} in everything it writes, and writes raw HTML in the site's output encoding, which may not
	 * be able to represent every character.
	 */
	static String escape(String text) {
		StringBuilder escaped = new StringBuilder(text.length() + 16);
		for (int i = 0; i < text.length();) {
			int codePoint = text.codePointAt(i);
			i += Character.charCount(codePoint);
			switch (codePoint) {
			case '&':
				escaped.append("&amp;");
				break;
			case '<':
				escaped.append("&lt;");
				break;
			case '>':
				escaped.append("&gt;");
				break;
			case '"':
				escaped.append("&quot;");
				break;
			case '\'':
				escaped.append("&#39;");
				break;
			case '$':
				escaped.append("&#36;");
				break;
			default:
				if (codePoint > 0x7E) {
					escaped.append("&#x").append(Integer.toHexString(codePoint)).append(';');
				} else {
					escaped.append((char) codePoint);
				}
				break;
			}
		}
		return escaped.toString();
	}
}
