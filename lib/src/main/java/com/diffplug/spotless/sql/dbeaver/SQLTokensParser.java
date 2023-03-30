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
package com.diffplug.spotless.sql.dbeaver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Forked from
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Based on SQLTokensParser from https://github.com/serge-rider/dbeaver,
 * which itself is licensed under the Apache 2.0 license.
 */
class SQLTokensParser {

	private static final String[] twoCharacterSymbol = {"<>", "<=", ">=", "||", "()", "!=", ":=", ".*"};
	private static final SQLDialect sqlDialect = SQLDialect.INSTANCE;

	private final String[][] quoteStrings;
	private String fBefore = null;
	private int fPos;
	private char structSeparator;
	private String catalogSeparator;
	private Set<String> commands = new HashSet<>();
	private String[] singleLineComments;
	private char[] singleLineCommentStart;

	SQLTokensParser() {
		this.structSeparator = sqlDialect.getStructSeparator();
		this.catalogSeparator = sqlDialect.getCatalogSeparator();
		this.quoteStrings = sqlDialect.getIdentifierQuoteStrings();
		this.singleLineComments = sqlDialect.getSingleLineComments();
		this.singleLineCommentStart = new char[this.singleLineComments.length];
		for (var i = 0; i < singleLineComments.length; i++) {
			if (singleLineComments[i].isEmpty())
				singleLineCommentStart[i] = 0;
			else
				singleLineCommentStart[i] = singleLineComments[i].charAt(0);
		}
	}

	private static boolean isSpace(final char argChar) {
		return Character.isWhitespace(argChar);
	}

	private static boolean isLetter(final char argChar) {
		return !isSpace(argChar) && !isDigit(argChar) && !isSymbol(argChar);
	}

	private static boolean isDigit(final char argChar) {
		return Character.isDigit(argChar);
	}

	private static boolean isSymbol(final char argChar) {
		switch (argChar) {
		case '"': // double quote
		case '?': // question mark
		case '%': // percent
		case '&': // ampersand
		case '\'': // quote
		case '(': // left paren
		case ')': // right paren
		case '|': // vertical bar
		case '*': // asterisk
		case '+': // plus sign
		case ',': // comma
		case '-': // minus sign
		case '.': // period
		case '/': // solidus
		case ':': // colon
		case ';': // semicolon
		case '<': // less than operator
		case '=': // equals operator
		case '>': // greater than operator
		case '!': // greater than operator
		case '~': // greater than operator
		case '`': // apos
		case '[': // bracket open
		case ']': // bracket close
			return true;
		default:
			return false;
		}
	}

	private FormatterToken nextToken() {
		var start_pos = fPos;
		if (fPos >= fBefore.length()) {
			fPos++;
			return new FormatterToken(TokenType.END, "", start_pos);
		}

		var fChar = fBefore.charAt(fPos);

		if (isSpace(fChar)) {
			var workString = new StringBuilder();
			for (;;) {
				workString.append(fChar);
				fChar = fBefore.charAt(fPos);
				if (!isSpace(fChar)) {
					return new FormatterToken(TokenType.SPACE, workString.toString(), start_pos);
				}
				fPos++;
				if (fPos >= fBefore.length()) {
					return new FormatterToken(TokenType.SPACE, workString.toString(), start_pos);
				}
			}
		} else if (fChar == ';') {
			fPos++;
			return new FormatterToken(TokenType.SYMBOL, ";", start_pos);
		} else if (isDigit(fChar)) {
			var s = new StringBuilder();
			while (isDigit(fChar) || fChar == '.' || fChar == 'e' || fChar == 'E') {
				// if (ch == '.') type = Token.REAL;
				s.append(fChar);
				fPos++;

				if (fPos >= fBefore.length()) {
					break;
				}

				fChar = fBefore.charAt(fPos);
			}
			return new FormatterToken(TokenType.VALUE, s.toString(), start_pos);
		}
		// single line comment
		else if (contains(singleLineCommentStart, fChar)) {
			fPos++;
			String commentString = null;
			for (String slc : singleLineComments) {
				if (fBefore.length() >= start_pos + slc.length() && slc.equals(fBefore.substring(start_pos, start_pos + slc.length()))) {
					commentString = slc;
					break;
				}
			}
			if (commentString == null) {
				return new FormatterToken(TokenType.SYMBOL, String.valueOf(fChar), start_pos);
			}
			fPos += commentString.length() - 1;
			while (fPos < fBefore.length()) {
				fPos++;
				if (fBefore.charAt(fPos - 1) == '\n') {
					break;
				}
			}
			commentString = fBefore.substring(start_pos, fPos);
			return new FormatterToken(TokenType.COMMENT, commentString, start_pos);
		} else if (isLetter(fChar)) {
			var s = new StringBuilder();
			while (isLetter(fChar) || isDigit(fChar) || fChar == '*' || structSeparator == fChar || catalogSeparator.indexOf(fChar) != -1) {
				s.append(fChar);
				fPos++;
				if (fPos >= fBefore.length()) {
					break;
				}

				fChar = fBefore.charAt(fPos);
			}
			var word = s.toString();
			if (commands.contains(word.toUpperCase(Locale.ENGLISH))) {
				s.setLength(0);
				for (; fPos < fBefore.length(); fPos++) {
					fChar = fBefore.charAt(fPos);
					if (fChar == '\n' || fChar == '\r') {
						break;
					} else {
						s.append(fChar);
					}
				}
				return new FormatterToken(TokenType.COMMAND, word + s.toString(), start_pos);
			}
			if (sqlDialect.getKeywordType(word) != null) {
				return new FormatterToken(TokenType.KEYWORD, word, start_pos);
			}
			return new FormatterToken(TokenType.NAME, word, start_pos);
		} else if (fChar == '/') {
			fPos++;
			var ch2 = fBefore.charAt(fPos);
			if (ch2 != '*') {
				return new FormatterToken(TokenType.SYMBOL, "/", start_pos);
			}

			var s = new StringBuilder("/*");
			fPos++;
			for (;;) {
				int ch0 = fChar;
				fChar = fBefore.charAt(fPos);
				s.append(fChar);
				fPos++;
				if (ch0 == '*' && fChar == '/') {
					return new FormatterToken(TokenType.COMMENT, s.toString(), start_pos);
				}
			}
		} else {
			if (fChar == '\'' || isQuoteChar(fChar)) {
				fPos++;
				var endQuoteChar = fChar;
				// Close quote char may differ
				if (quoteStrings != null) {
					for (String[] quoteString : quoteStrings) {
						if (quoteString[0].charAt(0) == endQuoteChar) {
							endQuoteChar = quoteString[1].charAt(0);
							break;
						}
					}
				}

				var s = new StringBuilder();
				s.append(fChar);
				for (;;) {
					fChar = fBefore.charAt(fPos);
					s.append(fChar);
					fPos++;
					var fNextChar = fPos >= fBefore.length() - 1 ? 0 : fBefore.charAt(fPos);
					if (fChar == endQuoteChar && fNextChar == endQuoteChar) {
						// Escaped quote
						s.append(fChar);
						fPos++;
						continue;
					}
					if (fChar == endQuoteChar) {
						return new FormatterToken(TokenType.VALUE, s.toString(), start_pos);
					}
				}
			}

			else if (isSymbol(fChar)) {
				var s = new StringBuilder(String.valueOf(fChar));
				fPos++;
				if (fPos >= fBefore.length()) {
					return new FormatterToken(TokenType.SYMBOL, s.toString(), start_pos);
				}
				var ch2 = fBefore.charAt(fPos);
				for (String aTwoCharacterSymbol : twoCharacterSymbol) {
					if (aTwoCharacterSymbol.charAt(0) == fChar && aTwoCharacterSymbol.charAt(1) == ch2) {
						fPos++;
						s.append(ch2);
						break;
					}
				}
				return new FormatterToken(TokenType.SYMBOL, s.toString(), start_pos);
			} else {
				fPos++;
				return new FormatterToken(TokenType.UNKNOWN, String.valueOf(fChar), start_pos);
			}
		}
	}

	private boolean isQuoteChar(char fChar) {
		if (quoteStrings != null) {
			for (String[] quoteString : quoteStrings) {
				if (quoteString[0].charAt(0) == fChar) {
					return true;
				}
			}
		}
		return false;
	}

	List<FormatterToken> parse(final String argSql) {
		fPos = 0;
		fBefore = argSql;

		final List<FormatterToken> list = new ArrayList<>();
		for (;;) {
			final FormatterToken token = nextToken();
			if (token.getType() == TokenType.END) {
				break;
			}

			list.add(token);
		}
		return list;
	}

	private static boolean contains(char[] array, char value) {
		if (array == null || array.length == 0)
			return false;
		for (char aChar : array) {
			if (aChar == value)
				return true;
		}
		return false;
	}
}
