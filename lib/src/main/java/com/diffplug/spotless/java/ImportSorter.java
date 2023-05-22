/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * @author Vojtech Krasa
 */
// Based on ImportSorterStep from https://github.com/krasa/EclipseCodeFormatter,
// which itself is licensed under the Apache 2.0 license.
final class ImportSorter {
	private static final int START_INDEX_OF_IMPORTS_PACKAGE_DECLARATION = 7;
	static final String N = "\n";

	private final List<String> importsOrder;
	private final boolean wildcardsLast;
	private final boolean semanticSort;
	private final Set<String> treatAsPackage;
	private final Set<String> treatAsClass;

	ImportSorter(List<String> importsOrder, boolean wildcardsLast, boolean semanticSort, Set<String> treatAsPackage,
			Set<String> treatAsClass) {
		this.importsOrder = new ArrayList<>(importsOrder);
		this.wildcardsLast = wildcardsLast;
		this.semanticSort = semanticSort;
		this.treatAsPackage = treatAsPackage;
		this.treatAsClass = treatAsClass;
	}

	String format(String raw, String lineFormat) {
		// parse file
		Scanner scanner = new Scanner(raw);
		int firstImportLine = 0;
		int lastImportLine = 0;
		int line = 0;
		boolean isMultiLineComment = false;
		List<String> imports = new ArrayList<>();
		while (scanner.hasNext()) {
			line++;
			String next = scanner.nextLine();
			if (next == null) {
				break;
			}
			//Since we have no AST, we only consider the most common use cases.
			isMultiLineComment |= next.contains("/*");
			if (isMultiLineComment && next.contains("*/")) {
				isMultiLineComment = false;
				if (!next.contains("/*")) {
					continue;
				}
			}

			if (next.startsWith("import ")) {
				int i = next.indexOf(".");
				if (isNotValidImport(i)) {
					continue;
				}
				if (firstImportLine == 0) {
					firstImportLine = line;
				}
				lastImportLine = line;
				int endIndex = next.indexOf(";");

				String imprt = next.substring(START_INDEX_OF_IMPORTS_PACKAGE_DECLARATION, endIndex != -1 ? endIndex : next.length());
				if (!isMultiLineComment && !imports.contains(imprt)) {
					imports.add(imprt);
				}
			}
			if (!isMultiLineComment && isBeginningOfScope(next)) {
				break; //Don't dare to touch lines after a scope started
			}
		}
		scanner.close();

		List<String> sortedImports = ImportSorterImpl.sort(imports, importsOrder, wildcardsLast, semanticSort,
				treatAsPackage, treatAsClass, lineFormat);
		return applyImportsToDocument(raw, firstImportLine, lastImportLine, sortedImports);
	}

	private static boolean isBeginningOfScope(String line) {
		int scope = line.indexOf("{");
		if (0 <= scope) {
			return !line.substring(0, scope).contains("//");
		}
		return false;
	}

	private static String applyImportsToDocument(final String document, int firstImportLine, int lastImportLine, List<String> strings) {
		if (document.isEmpty()) {
			return document;
		}
		boolean importsAlreadyAppended = false;
		Scanner scanner = new Scanner(document);
		int curentLine = 0;
		final StringBuilder sb = new StringBuilder();
		while (scanner.hasNext()) {
			curentLine++;
			String next = scanner.nextLine();
			if (next == null) {
				break;
			}
			if (curentLine >= firstImportLine && curentLine <= lastImportLine) {
				if (!importsAlreadyAppended) {
					for (String string : strings) {
						sb.append(string);
					}
				}
				importsAlreadyAppended = true;
			} else {
				append(sb, next);
			}
		}
		scanner.close();
		if (!document.endsWith("\n")) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	private static void append(StringBuilder sb, String next) {
		sb.append(next);
		sb.append(N);
	}

	private static boolean isNotValidImport(int i) {
		return i <= START_INDEX_OF_IMPORTS_PACKAGE_DECLARATION;
	}
}
