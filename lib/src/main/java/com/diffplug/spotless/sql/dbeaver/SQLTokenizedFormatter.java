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
package com.diffplug.spotless.sql.dbeaver;

import static java.lang.System.lineSeparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.diffplug.spotless.annotations.Internal;

/**
 * **Warning:** Use this class at your own risk. It is an implementation detail and is not
 * guaranteed to exist in future versions.
 * <p>
 * Forked from
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * <p>
 * Based on SQLTokenizedFormatter from https://github.com/serge-rider/dbeaver,
 * which itself is licensed under the Apache 2.0 license.
 */
@Internal
public class SQLTokenizedFormatter {

	private static final String[] JOIN_BEGIN = {"LEFT", "RIGHT", "INNER", "OUTER", "JOIN"};
	private static final SQLDialect sqlDialect = SQLDialect.INSTANCE;
	private DBeaverSQLFormatterConfiguration formatterCfg;
	private List<Boolean> functionBracket = new ArrayList<>();
	private List<String> statementDelimiters = new ArrayList<>(2);

	public SQLTokenizedFormatter(DBeaverSQLFormatterConfiguration formatterCfg) {
		this.formatterCfg = formatterCfg;
	}

	public String format(final String argSql) {

		statementDelimiters.add(formatterCfg.getStatementDelimiter());
		SQLTokensParser fParser = new SQLTokensParser();

		functionBracket.clear();

		boolean isSqlEndsWithNewLine = false;
		if (argSql.endsWith("\n")) {
			isSqlEndsWithNewLine = true;
		}

		List<FormatterToken> list = fParser.parse(argSql);
		list = format(list);

		StringBuilder after = new StringBuilder(argSql.length() + 20);
		for (FormatterToken token : list) {
			after.append(token.getString());
		}

		if (isSqlEndsWithNewLine) {
			after.append(lineSeparator());
		}

		return after.toString();
	}

	private List<FormatterToken> format(final List<FormatterToken> argList) {
		if (argList.isEmpty()) {
			return argList;
		}

		FormatterToken token = argList.get(0);
		if (token.getType() == TokenType.SPACE) {
			argList.remove(0);
			if (argList.isEmpty()) {
				return argList;
			}
		}

		token = argList.get(argList.size() - 1);
		if (token.getType() == TokenType.SPACE) {
			argList.remove(argList.size() - 1);
			if (argList.isEmpty()) {
				return argList;
			}
		}

		final KeywordCase keywordCase = formatterCfg.getKeywordCase();
		for (FormatterToken anArgList : argList) {
			token = anArgList;
			if (token.getType() == TokenType.KEYWORD) {
				token.setString(keywordCase.transform(token.getString()));
			}
		}

		// Remove extra tokens (spaces, etc)
		for (int index = argList.size() - 1; index >= 1; index--) {
			token = argList.get(index);
			FormatterToken prevToken = argList.get(index - 1);
			if (token.getType() == TokenType.SPACE && (prevToken.getType() == TokenType.SYMBOL || prevToken.getType() == TokenType.COMMENT)) {
				argList.remove(index);
			} else if ((token.getType() == TokenType.SYMBOL || token.getType() == TokenType.COMMENT) && prevToken.getType() == TokenType.SPACE) {
				argList.remove(index - 1);
			} else if (token.getType() == TokenType.SPACE) {
				token.setString(" ");
			}
		}

		for (int index = 0; index < argList.size() - 2; index++) {
			FormatterToken t0 = argList.get(index);
			FormatterToken t1 = argList.get(index + 1);
			FormatterToken t2 = argList.get(index + 2);

			String tokenString = t0.getString().toUpperCase(Locale.ENGLISH);
			String token2String = t2.getString().toUpperCase(Locale.ENGLISH);
			// Concatenate tokens
			if (t0.getType() == TokenType.KEYWORD && t1.getType() == TokenType.SPACE && t2.getType() == TokenType.KEYWORD) {
				if (((tokenString.equals("ORDER") || tokenString.equals("GROUP") || tokenString.equals("CONNECT")) && token2String.equals("BY")) ||
						((tokenString.equals("START")) && token2String.equals("WITH"))) {
					t0.setString(t0.getString() + " " + t2.getString());
					argList.remove(index + 1);
					argList.remove(index + 1);
				}
			}

			// Oracle style joins
			if (tokenString.equals("(") && t1.getString().equals("+") && token2String.equals(")")) {  //$NON-NLS-2$ //$NON-NLS-3$
				t0.setString("(+)");
				argList.remove(index + 1);
				argList.remove(index + 1);
			}

			// JDBI bind list
			if (tokenString.equals("<") && t1.getType() == TokenType.NAME && token2String.equals(">")) {
				t0.setString(t0.getString() + t1.getString() + t2.getString());
				argList.remove(index + 1);
				argList.remove(index + 1);
			}
		}

		int indent = 0;
		final List<Integer> bracketIndent = new ArrayList<>();
		FormatterToken prev = new FormatterToken(TokenType.SPACE, " ");
		boolean encounterBetween = false;
		for (int index = 0; index < argList.size(); index++) {
			token = argList.get(index);
			String tokenString = token.getString().toUpperCase(Locale.ENGLISH);
			if (token.getType() == TokenType.SYMBOL) {
				if (tokenString.equals("(")) {
					functionBracket.add(isFunction(prev.getString()) ? Boolean.TRUE : Boolean.FALSE);
					bracketIndent.add(indent);
					indent++;
					index += insertReturnAndIndent(argList, index + 1, indent);
				} else if (tokenString.equals(")") && !bracketIndent.isEmpty() && !functionBracket.isEmpty()) {
					indent = bracketIndent.remove(bracketIndent.size() - 1);
					index += insertReturnAndIndent(argList, index, indent);
					functionBracket.remove(functionBracket.size() - 1);
				} else if (tokenString.equals(",")) {
					index += insertReturnAndIndent(argList, index + 1, indent);
				} else if (statementDelimiters.contains(tokenString)) {
					indent = 0;
					index += insertReturnAndIndent(argList, index, indent);
				}
			} else if (token.getType() == TokenType.KEYWORD) {
				switch (tokenString) {
				case "DELETE":
				case "SELECT":
				case "UPDATE":
				case "INSERT":
				case "INTO":
				case "CREATE":
				case "DROP":
				case "TRUNCATE":
				case "TABLE":
				case "CASE":
					indent++;
					index += insertReturnAndIndent(argList, index + 1, indent);
					break;
				case "FROM":
				case "WHERE":
				case "SET":
				case "START WITH":
				case "CONNECT BY":
				case "ORDER BY":
				case "GROUP BY":
				case "HAVING":
					index += insertReturnAndIndent(argList, index, indent - 1);
					index += insertReturnAndIndent(argList, index + 1, indent);
					break;
				case "LEFT":
				case "RIGHT":
				case "INNER":
				case "OUTER":
				case "JOIN":
					if (isJoinStart(argList, index)) {
						index += insertReturnAndIndent(argList, index, indent - 1);
					}
					break;
				case "VALUES":
				case "END":
					indent--;
					index += insertReturnAndIndent(argList, index, indent);
					break;
				case "OR":
				case "WHEN":
				case "ELSE":
					index += insertReturnAndIndent(argList, index, indent);
					break;
				case "ON":
					//indent++;
					index += insertReturnAndIndent(argList, index + 1, indent);
					break;
				case "USING":   //$NON-NLS-2$
					index += insertReturnAndIndent(argList, index, indent + 1);
					break;
				case "TOP":   //$NON-NLS-2$
					// SQL Server specific
					index += insertReturnAndIndent(argList, index, indent);
					if (argList.size() < index + 3) {
						index += insertReturnAndIndent(argList, index + 3, indent);
					}
					break;
				case "UNION":
				case "INTERSECT":
				case "EXCEPT":
					indent -= 2;
					index += insertReturnAndIndent(argList, index, indent);
					//index += insertReturnAndIndent(argList, index + 1, indent);
					indent++;
					break;
				case "BETWEEN":
					encounterBetween = true;
					break;
				case "AND":
					if (!encounterBetween) {
						index += insertReturnAndIndent(argList, index, indent);
					}
					encounterBetween = false;
					break;
				default:
					break;
				}
			} else if (token.getType() == TokenType.COMMENT) {
				boolean isComment = false;
				String[] slComments = sqlDialect.getSingleLineComments();
				for (String slc : slComments) {
					if (token.getString().startsWith(slc)) {
						isComment = true;
						break;
					}
				}
				if (!isComment) {
					Pair<String, String> mlComments = sqlDialect.getMultiLineComments();
					if (token.getString().startsWith(mlComments.getFirst())) {
						index += insertReturnAndIndent(argList, index + 1, indent);
					}
				}
			} else if (token.getType() == TokenType.COMMAND) {
				indent = 0;
				if (index > 0) {
					index += insertReturnAndIndent(argList, index, 0);
				}
				index += insertReturnAndIndent(argList, index + 1, 0);
			} else if (token.getType() == TokenType.NAME && index > 0 && argList.get(index - 1).getType() == TokenType.COMMENT) {
				index += insertReturnAndIndent(argList, index, indent);
			} else {
				if (statementDelimiters.contains(tokenString)) {
					indent = 0;
					index += insertReturnAndIndent(argList, index + 1, indent);
				}
			}
			prev = token;
		}

		for (int index = argList.size() - 1; index >= 4; index--) {
			if (index >= argList.size()) {
				continue;
			}

			FormatterToken t0 = argList.get(index);
			FormatterToken t1 = argList.get(index - 1);
			FormatterToken t2 = argList.get(index - 2);
			FormatterToken t3 = argList.get(index - 3);
			FormatterToken t4 = argList.get(index - 4);

			if (t4.getString().equals("(")
					&& t3.getString().trim().isEmpty()
					&& t1.getString().trim().isEmpty()
					&& t0.getString().equalsIgnoreCase(")")) {
				t4.setString(t4.getString() + t2.getString() + t0.getString());
				argList.remove(index);
				argList.remove(index - 1);
				argList.remove(index - 2);
				argList.remove(index - 3);
			}
		}

		for (int index = 1; index < argList.size(); index++) {
			prev = argList.get(index - 1);
			token = argList.get(index);

			if (prev.getType() != TokenType.SPACE &&
					token.getType() != TokenType.SPACE &&
					!token.getString().startsWith("(")) {
				if (token.getString().equals(",") || statementDelimiters.contains(token.getString())) {
					continue;
				}
				if (isFunction(prev.getString())
						&& token.getString().equals("(")) {
					continue;
				}
				if (token.getType() == TokenType.VALUE && prev.getType() == TokenType.NAME) {
					// Do not add space between name and value [JDBC:MSSQL]
					continue;
				}
				if (token.getType() == TokenType.SYMBOL && isEmbeddedToken(token) ||
						prev.getType() == TokenType.SYMBOL && isEmbeddedToken(prev)) {
					// Do not insert spaces around colons
					continue;
				}
				if (token.getType() == TokenType.SYMBOL && prev.getType() == TokenType.SYMBOL) {
					// Do not add space between symbols
					continue;
				}
				if (prev.getType() == TokenType.COMMENT) {
					// Do not add spaces to comments
					continue;
				}
				argList.add(index, new FormatterToken(TokenType.SPACE, " "));
			}
		}

		return argList;
	}

	private static boolean isEmbeddedToken(FormatterToken token) {
		return ":".equals(token.getString()) || ".".equals(token.getString());
	}

	private boolean isJoinStart(List<FormatterToken> argList, int index) {
		// Keyword sequence must start from LEFT, RIGHT, INNER, OUTER or JOIN and must end with JOIN
		// And we must be in the beginning of sequence

		// check current token
		if (!contains(JOIN_BEGIN, argList.get(index).getString())) {
			return false;
		}
		// check previous token
		for (int i = index - 1; i >= 0; i--) {
			FormatterToken token = argList.get(i);
			if (token.getType() == TokenType.SPACE) {
				continue;
			}
			if (contains(JOIN_BEGIN, token.getString())) {
				// It is not the begin of sequence
				return false;
			} else {
				break;
			}
		}
		// check last token
		for (int i = index; i < argList.size(); i++) {
			FormatterToken token = argList.get(i);
			if (token.getType() == TokenType.SPACE) {
				continue;
			}
			if (token.getString().equals("JOIN")) {
				return true;
			}
			if (!contains(JOIN_BEGIN, token.getString())) {
				// It is not the begin of sequence
				return false;
			}
		}
		return false;
	}

	private boolean isFunction(String name) {
		return sqlDialect.getKeywordType(name) == DBPKeywordType.FUNCTION;
	}

	private int insertReturnAndIndent(final List<FormatterToken> argList, final int argIndex, final int argIndent) {
		if (functionBracket.contains(Boolean.TRUE))
			return 0;
		try {
			final String defaultLineSeparator = lineSeparator();
			StringBuilder s = new StringBuilder(defaultLineSeparator);
			for (int index = 0; index < argIndent; index++) {
				s.append(formatterCfg.getIndentString());
			}
			if (argIndex > 0) {
				final FormatterToken token = argList.get(argIndex);
				final FormatterToken prevToken = argList.get(argIndex - 1);
				if (token.getType() == TokenType.COMMENT &&
						isCommentLine(sqlDialect, token.getString()) &&
						prevToken.getType() != TokenType.END) {
					s.setCharAt(0, ' ');
					s.setLength(1);

					final String comment = token.getString();
					final String withoutTrailingWhitespace = comment.replaceFirst("\\s*$", "");
					token.setString(withoutTrailingWhitespace);
				}
			}

			FormatterToken token = argList.get(argIndex);
			if (token.getType() == TokenType.SPACE) {
				token.setString(s.toString());
				return 0;
			}
			boolean isDelimiter = statementDelimiters.contains(token.getString().toUpperCase(Locale.ENGLISH));

			if (!isDelimiter) {
				token = argList.get(argIndex - 1);
				if (token.getType() == TokenType.SPACE) {
					token.setString(s.toString());
					return 0;
				}
			}

			if (isDelimiter) {
				if (argList.size() > argIndex + 1) {
					String string = s.toString();
					argList.add(argIndex + 1, new FormatterToken(TokenType.SPACE, string + string));
				}
			} else {
				argList.add(argIndex, new FormatterToken(TokenType.SPACE, s.toString()));
			}
			return 1;
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static boolean isCommentLine(SQLDialect dialect, String line) {
		for (String slc : dialect.getSingleLineComments()) {
			if (line.startsWith(slc)) {
				return true;
			}
		}
		return false;
	}

	private static <OBJECT_TYPE> boolean contains(OBJECT_TYPE[] array, OBJECT_TYPE value) {
		if (array == null || array.length == 0)
			return false;
		for (OBJECT_TYPE anArray : array) {
			if (Objects.equals(value, anArray))
				return true;
		}
		return false;
	}

}
