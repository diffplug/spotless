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

import java.util.Properties;

import com.diffplug.spotless.annotations.Internal;

/**
 * **Warning:** Use this class at your own risk. It is an implementation detail and is not
 * guaranteed to exist in future versions.
 * <p>
 * Forked from
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * <p>
 * Based on SQLFormatterConfiguration from https://github.com/serge-rider/dbeaver,
 * which itself is licensed under the Apache 2.0 license.
 */
@Internal
public class DBeaverSQLFormatterConfiguration {

	/**
	 * UPPER, LOWER or ORIGINAL
	 */
	private static final String SQL_FORMATTER_KEYWORD_CASE = "sql.formatter.keyword.case";

	/**
	 * ';' by default
	 */
	private static final String SQL_FORMATTER_STATEMENT_DELIMITER = "sql.formatter.statement.delimiter";
	/**
	 * space or tab
	 */
	private static final String SQL_FORMATTER_INDENT_TYPE = "sql.formatter.indent.type";
	/**
	 * 4 by default
	 */
	private static final String SQL_FORMATTER_INDENT_SIZE = "sql.formatter.indent.size";

	private String statementDelimiters;
	private KeywordCase keywordCase;
	private String indentString;

	public DBeaverSQLFormatterConfiguration(Properties properties) {
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
		stringBuilder.append(String.valueOf(indentChar).repeat(Math.max(0, indentSize)));
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
