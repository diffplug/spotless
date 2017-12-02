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
package com.diffplug.spotless.sql.dbeaver;

import java.util.Properties;

/**
 * SQLFormatterConfiguration
 */
public class SQLFormatterConfiguration {

	/**
	 * UPPER, LOWER or ORIGINAL
	 */
	public static final String SQL_FORMATTER_KEYWORD_CASE = "sql.formatter.keyword.case";

	/**
	 * ';' by default
	 */
	public static final String SQL_FORMATTER_STATEMENT_DELIMITER = "sql.formatter.statement.delimiter";
	/**
	 * space or tag
	 */
	public static final String SQL_FORMATTER_INDENT_TYPE = "sql.formatter.indent.type";
	/**
	 * 4 by default
	 */
	public static final String SQL_FORMATTER_INDENT_SIZE = "sql.formatter.indent.size";

	private String statementDelimiters;
	private KeywordCase keywordCase;
	private String indentString;

	public SQLFormatterConfiguration(Properties properties) {
		this.keywordCase = KeywordCase.valueOf(properties.getProperty(SQL_FORMATTER_KEYWORD_CASE, "UPPER"));
		this.statementDelimiters = properties.getProperty(SQL_FORMATTER_STATEMENT_DELIMITER, SQLDialect.INSTANCE
				.getScriptDelimiter());
		String indentType = properties.getProperty(SQL_FORMATTER_INDENT_TYPE, "space");
		int indentSize = Integer.parseInt(properties.getProperty(SQL_FORMATTER_INDENT_SIZE, "4"));
		indentString = getIndentString(indentType, indentSize);
	}

	private String getIndentString(String indentType, int indentSize) {
		char indentChar = indentType.equals("space") ? ' ' : '\t';
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < indentSize; i++) {
			stringBuilder.append(indentChar);
		}
		return stringBuilder.toString();
	}

	String getStatementDelimiter() {
		return statementDelimiters;
	}

	String getIndentString() {
		return indentString;
	}

	KeywordCase getKeywordCase() {
		return keywordCase;
	}

}
