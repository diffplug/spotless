package com.github.youribonnaffe.gradle.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * From https://github.com/krasa/EclipseCodeFormatter
 *
 * @author Vojtech Krasa
 */
public class ImportSorterAdapter {
    public static final int START_INDEX_OF_IMPORTS_PACKAGE_DECLARATION = 7;
    public static final String N = "\n";

    private List<String> importsOrder;

    public ImportSorterAdapter(List<String> importsOrder) {
        this.importsOrder = new ArrayList<String>(importsOrder);
    }

    public String sortImports(String document) {
        // parse file
        Scanner scanner = new Scanner(document);
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

        List<String> sortedImports = ImportsSorter.sort(imports, importsOrder);
        return applyImportsToDocument(document, firstImportLine, lastImportLine, sortedImports);
    }

    private String applyImportsToDocument(final String document, int firstImportLine, int lastImportLine,
                                          List<String> strings) {
        Scanner scanner;
        boolean importsAlreadyAppended = false;
        scanner = new Scanner(document);
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
        return sb.toString();
    }

    private void append(StringBuilder sb, String next) {
        sb.append(next);
        sb.append(N);
    }

    private boolean isNotValidImport(int i) {
        return i <= START_INDEX_OF_IMPORTS_PACKAGE_DECLARATION;
    }

}