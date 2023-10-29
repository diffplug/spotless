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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Forked from
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * <p>
 * Based on SQLDialect from https://github.com/serge-rider/dbeaver,
 * which itself is licensed under the Apache 2.0 license.
 */
class SQLDialect {

	private static final String[] DEFAULT_LINE_COMMENTS = {SQLConstants.SL_COMMENT};
	private static final String[] EXEC_KEYWORDS = new String[0];

	private static final String[][] DEFAULT_QUOTE_STRINGS = {{"\"", "\""}};

	// Keywords
	private TreeMap<String, DBPKeywordType> allKeywords = new TreeMap<>();

	private final TreeSet<String> functions = new TreeSet<>();
	private final TreeSet<String> types = new TreeSet<>();
	// Comments
	private Pair<String, String> multiLineComments = new Pair<>(SQLConstants.ML_COMMENT_START, SQLConstants.ML_COMMENT_END);

	static final SQLDialect INSTANCE = new SQLDialect();

	private SQLDialect() {
		loadStandardKeywords();
	}

	String[][] getIdentifierQuoteStrings() {
		return DEFAULT_QUOTE_STRINGS;
	}

	private String[] getExecuteKeywords() {
		return EXEC_KEYWORDS;
	}

	private void addSQLKeyword(String keyword) {
		allKeywords.put(keyword, DBPKeywordType.KEYWORD);
	}

	/**
	 * Add keywords.
	 * @param set     keywords. Must be in upper case.
	 * @param type    keyword type
	 */
	private void addKeywords(Collection<String> set, DBPKeywordType type) {
		for (String keyword : set) {
			keyword = keyword.toUpperCase(Locale.ENGLISH);
			DBPKeywordType oldType = allKeywords.get(keyword);
			if (oldType != DBPKeywordType.KEYWORD) {
				// We can't mark keywords as functions or types because keywords are reserved and
				// if some identifier conflicts with keyword it must be quoted.
				allKeywords.put(keyword, type);
			}
		}
	}

	DBPKeywordType getKeywordType(String word) {
		return allKeywords.get(word.toUpperCase(Locale.ENGLISH));
	}

	String getCatalogSeparator() {
		return String.valueOf(SQLConstants.STRUCT_SEPARATOR);
	}

	char getStructSeparator() {
		return SQLConstants.STRUCT_SEPARATOR;
	}

	String getScriptDelimiter() {
		return ";";
	}

	Pair<String, String> getMultiLineComments() {
		return multiLineComments;
	}

	String[] getSingleLineComments() {
		return DEFAULT_LINE_COMMENTS;
	}

	private void loadStandardKeywords() {
		// Add default set of keywords
		Set<String> all = new HashSet<>();
		Collections.addAll(all, SQLConstants.SQL2003_RESERVED_KEYWORDS);
		Collections.addAll(all, SQLConstants.SQL_EX_KEYWORDS);
		Collections.addAll(functions, SQLConstants.SQL2003_FUNCTIONS);

		for (String executeKeyword : getExecuteKeywords()) {
			addSQLKeyword(executeKeyword);
		}

		// Add default types
		Collections.addAll(types, SQLConstants.DEFAULT_TYPES);

		addKeywords(all, DBPKeywordType.KEYWORD);
		addKeywords(types, DBPKeywordType.TYPE);
		addKeywords(functions, DBPKeywordType.FUNCTION);
	}

}
