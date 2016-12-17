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
package com.diffplug.spotless.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Vojtech Krasa
 */
// Based on ImportSorterStep from https://github.com/krasa/EclipseCodeFormatter,
// which itself is licensed under the Apache 2.0 license.
final class ImportSorter {
	private static final int START_INDEX_OF_IMPORTS_PACKAGE_DECLARATION = 7;
	static final String N = "\n";

	private final List<String> importsOrder;

	ImportSorter(List<String> importsOrder) {
		this.importsOrder = new ArrayList<>(importsOrder);
	}

	public String format(String raw) {
		// parse file
		Scanner scanner = new Scanner(raw);
		int firstImportLine = 0;
		int lastImportLine = 0;
		int line = 0;
		List<String> imports = new ArrayList<>();
		while (scanner.hasNext()) {
			line++;
			String next = scanner.nextLine();
			if (next == null) {
				break;
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
				if (!imports.contains(imprt)) {
					imports.add(imprt);
				}
			}
		}
		scanner.close();

		List<String> sortedImports = ImportSorterImpl.sort(imports, importsOrder);
		return applyImportsToDocument(raw, firstImportLine, lastImportLine, sortedImports);
	}

	private static String applyImportsToDocument(final String document, int firstImportLine, int lastImportLine, List<String> strings) {
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
