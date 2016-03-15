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
package com.diffplug.gradle.spotless.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * From https://github.com/krasa/EclipseCodeFormatter
 *
 * @author Vojtech Krasa
 */
public class ImportSorterStep {
	public static final String NAME = "ImportSorter";

	public static final int START_INDEX_OF_IMPORTS_PACKAGE_DECLARATION = 7;
	public static final String N = "\n";

	private List<String> importsOrder;

	public ImportSorterStep(List<String> importsOrder) {
		this.importsOrder = new ArrayList<String>(importsOrder);
	}

	public ImportSorterStep(File importsFile) throws IOException {
		Map<Integer, String> orderToImport = Files.readAllLines(importsFile.toPath()).stream()
				// filter out comments
				.filter(line -> !line.startsWith("#"))
				// parse 0=input
				.map(line -> {
					String[] pieces = line.split("=");
					int idx = Integer.parseInt(pieces[0]);
					String name = pieces.length == 2 ? pieces[1] : "";
					return new AbstractMap.SimpleEntry<Integer, String>(idx, name);
				})
				// collect into map
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		// sort the entries by the key, save the values
		importsOrder = new ArrayList<>(new TreeMap<>(orderToImport).values());
	}

	public String format(String raw) {
		// parse file
		Scanner scanner = new Scanner(raw);
		int firstImportLine = 0;
		int lastImportLine = 0;
		int line = 0;
		List<String> imports = new ArrayList<String>();
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
				imports.add(next.substring(START_INDEX_OF_IMPORTS_PACKAGE_DECLARATION,
						endIndex != -1 ? endIndex : next.length()));
			}
		}
		scanner.close();

		List<String> sortedImports = ImportSorterImpl.sort(imports, importsOrder);
		return applyImportsToDocument(raw, firstImportLine, lastImportLine, sortedImports);
	}

	private static String applyImportsToDocument(final String document, int firstImportLine, int lastImportLine,
			List<String> strings) {
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
