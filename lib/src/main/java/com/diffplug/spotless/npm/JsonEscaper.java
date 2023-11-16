/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.npm;

import static java.util.Objects.requireNonNull;

/**
 * Simple implementation on how to escape values when printing json.
 * Implementation is partly based on https://github.com/stleary/JSON-java
 */
final class JsonEscaper {
	private JsonEscaper() {
		// no instance
	}

	public static String jsonEscape(Object val) {
		requireNonNull(val);
		if (val instanceof JsonRawValue) {
			return jsonEscape((JsonRawValue) val);
		}
		if (val instanceof String) {
			return jsonEscape((String) val);
		}
		if (ListableAdapter.canAdapt(val)) {
			// create an array
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			boolean first = true;
			for (Object o : ListableAdapter.adapt(val)) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(jsonEscape(o));
			}
			sb.append(']');
			return sb.toString();
		}
		return val.toString();
	}

	private static String jsonEscape(JsonRawValue jsonRawValue) {
		return jsonRawValue.getRawJson();
	}

	private static String jsonEscape(String unescaped) {
		/**
		 * the following characters are reserved in JSON and must be properly escaped to be used in strings:
		 * <p>
		 * Backspace is replaced with \b
		 * Form feed is replaced with \f
		 * Newline is replaced with \n
		 * Carriage return is replaced with \r
		 * Tab is replaced with \t
		 * Double quote is replaced with \"
		 * Backslash is replaced with \\
		 * <p>
		 * additionally we handle xhtml '</bla>' string
		 * and non-ascii chars
		 */
		StringBuilder escaped = new StringBuilder();
		escaped.append('"');
		char b;
		char c = 0;
		for (int i = 0; i < unescaped.length(); i++) {
			b = c;
			c = unescaped.charAt(i);
			switch (c) {
			case '\"':
				escaped.append('\\').append('"');
				break;
			case '\n':
				escaped.append('\\').append('n');
				break;
			case '\r':
				escaped.append('\\').append('r');
				break;
			case '\t':
				escaped.append('\\').append('t');
				break;
			case '\b':
				escaped.append('\\').append('b');
				break;
			case '\f':
				escaped.append('\\').append('f');
				break;
			case '\\':
				escaped.append('\\').append('\\');
				break;
			case '/':
				if (b == '<') {
					escaped.append('\\');
				}
				escaped.append(c);
				break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
						|| (c >= '\u2000' && c < '\u2100')) {
					escaped.append('\\').append('u');
					String hexString = Integer.toHexString(c);
					escaped.append("0000", 0, 4 - hexString.length());
					escaped.append(hexString);
				} else {
					escaped.append(c);
				}
			}
		}
		escaped.append('"');
		return escaped.toString();
	}

}
