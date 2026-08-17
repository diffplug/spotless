/*
 * Copyright 2025-2026 DiffPlug
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
package com.diffplug.spotless.glue.javaparser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import com.diffplug.spotless.FormatterFunc;

/**
 * Uses JavaParser to identify fully qualified type references in the AST,
 * then performs text-level replacement to shorten them and add imports.
 *
 * <p>The parser gives us accurate type-context identification (no false positives
 * from strings, comments, or non-type contexts). Text-level replacement preserves
 * the original formatting exactly.
 */
public class ShortenQualifiedTypesFormatterFunc implements FormatterFunc {

	private final JavaParser parser = new JavaParser(
			new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));

	@Override
	public String apply(String rawUnix) throws Exception {
		ParseResult<CompilationUnit> parseResult = parser.parse(rawUnix);
		if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
			return rawUnix;
		}
		CompilationUnit cu = parseResult.getResult().get();

		// 1. Collect the package name
		String packageName = cu.getPackageDeclaration()
				.map(PackageDeclaration::getNameAsString)
				.orElse("");

		// 2. Collect existing non-static imports
		Map<String, String> existingImportsBySimple = new LinkedHashMap<>();
		Set<String> existingImportFqns = new LinkedHashSet<>();
		for (ImportDeclaration imp : cu.getImports()) {
			if (imp.isStatic() || imp.isAsterisk()) {
				continue;
			}
			String fqn = imp.getNameAsString();
			existingImportFqns.add(fqn);
			String simple = fqn.substring(fqn.lastIndexOf('.') + 1);
			existingImportsBySimple.put(simple, fqn);
		}

		// 3. Walk the AST to find outermost fully-qualified type nodes
		Map<String, Set<String>> simpleToFqns = new LinkedHashMap<>();
		List<QualifiedTypeRef> qualifiedRefs = new ArrayList<>();

		cu.accept(new CollectQualifiedTypesVisitor(simpleToFqns, qualifiedRefs), null);

		if (qualifiedRefs.isEmpty()) {
			return rawUnix;
		}

		// 4. Determine which FQNs are safe to shorten
		Set<String> safeToShorten = new LinkedHashSet<>();
		for (Map.Entry<String, Set<String>> entry : simpleToFqns.entrySet()) {
			String simple = entry.getKey();
			Set<String> fqns = entry.getValue();
			if (fqns.size() > 1) {
				continue;
			}
			String fqn = fqns.iterator().next();
			String existing = existingImportsBySimple.get(simple);
			if (existing != null && !existing.equals(fqn)) {
				continue;
			}
			safeToShorten.add(fqn);
		}

		if (safeToShorten.isEmpty()) {
			return rawUnix;
		}

		// 5. Convert line/column positions to string offsets and replace
		// Build line-start offset table
		int[] lineOffsets = buildLineOffsets(rawUnix);

		// Use a set keyed on start offset to deduplicate (JavaParser may visit the same node twice,
		// e.g. for instanceof pattern variables)
		Map<Integer, int[]> removalsByStart = new LinkedHashMap<>();
		for (QualifiedTypeRef ref : qualifiedRefs) {
			if (!safeToShorten.contains(ref.fqn)) {
				continue;
			}
			int scopeStartOffset = toOffset(lineOffsets, ref.scopeStart);
			int nameStartOffset = toOffset(lineOffsets, ref.nameStart);
			if (scopeStartOffset >= 0 && nameStartOffset > scopeStartOffset) {
				removalsByStart.putIfAbsent(scopeStartOffset, new int[]{scopeStartOffset, nameStartOffset});
			}
		}
		List<int[]> removals = new ArrayList<>(removalsByStart.values());

		// Sort removals in reverse order so we can apply them without invalidating offsets
		removals.sort(Comparator.comparingInt((int[] a) -> a[0]).reversed());

		StringBuilder sb = new StringBuilder(rawUnix);
		for (int[] removal : removals) {
			sb.delete(removal[0], removal[1]);
		}

		// 6. Add missing imports
		Set<String> newImports = new TreeSet<>();
		for (String fqn : safeToShorten) {
			if (fqn.startsWith("java.lang.") && fqn.indexOf('.', 10) == -1) {
				continue;
			}
			if (!packageName.isEmpty() && fqn.startsWith(packageName + ".")
					&& fqn.indexOf('.', packageName.length() + 1) == -1) {
				continue;
			}
			if (existingImportFqns.contains(fqn)) {
				continue;
			}
			newImports.add(fqn);
		}

		if (!newImports.isEmpty()) {
			String result = sb.toString();
			int insertPos = findImportInsertPosition(result);
			boolean afterExistingImport = IMPORT_LINE.matcher(result).find();

			StringBuilder importBlock = new StringBuilder();
			if (!afterExistingImport) {
				importBlock.append('\n');
			}
			for (String fqn : newImports) {
				importBlock.append("\nimport ").append(fqn).append(';');
			}
			sb = new StringBuilder(result);
			sb.insert(insertPos, importBlock);
		}

		return sb.toString();
	}

	private record QualifiedTypeRef(String fqn, String simpleName, Position scopeStart, Position nameStart) {}

	/** Collects the outermost fully-qualified type nodes, along with the text range of the scope to remove. */
	private static final class CollectQualifiedTypesVisitor extends VoidVisitorAdapter<Void> {
		private final Map<String, Set<String>> simpleToFqns;
		private final List<QualifiedTypeRef> qualifiedRefs;

		CollectQualifiedTypesVisitor(Map<String, Set<String>> simpleToFqns, List<QualifiedTypeRef> qualifiedRefs) {
			this.simpleToFqns = simpleToFqns;
			this.qualifiedRefs = qualifiedRefs;
		}

		@Override
		public void visit(ClassOrInterfaceType type, Void arg) {
			super.visit(type, arg);
			if (type.getScope().isEmpty()) {
				return;
			}
			// Skip types that are themselves the scope of a parent type
			if (type.getParentNode().isPresent()
					&& type.getParentNode().get() instanceof ClassOrInterfaceType parent
					&& parent.getScope().isPresent()
					&& parent.getScope().get() == type) {
				return;
			}
			String rawName = buildRawName(type);
			if (!startsWithPackage(rawName)) {
				return;
			}
			String simple = type.getNameAsString();
			simpleToFqns.computeIfAbsent(simple, k -> new LinkedHashSet<>()).add(rawName);

			// Record the text range of the scope (to be removed)
			ClassOrInterfaceType scope = type.getScope().get();
			if (scope.getBegin().isPresent() && type.getName().getBegin().isPresent()) {
				Position scopeStart = scope.getBegin().get();
				Position nameStart = type.getName().getBegin().get();
				qualifiedRefs.add(new QualifiedTypeRef(rawName, simple, scopeStart, nameStart));
			}
		}
	}

	private static String buildRawName(ClassOrInterfaceType type) {
		StringBuilder sb = new StringBuilder();
		buildRawNameRecursive(type, sb);
		return sb.toString();
	}

	private static void buildRawNameRecursive(ClassOrInterfaceType type, StringBuilder sb) {
		if (type.getScope().isPresent()) {
			buildRawNameRecursive(type.getScope().get(), sb);
			sb.append('.');
		}
		sb.append(type.getNameAsString());
	}

	private static boolean startsWithPackage(String rawName) {
		return !rawName.isEmpty() && Character.isLowerCase(rawName.charAt(0));
	}

	/** Builds an array where lineOffsets[line] is the char offset of the start of that line (1-indexed). */
	private static int[] buildLineOffsets(String text) {
		List<Integer> offsets = new ArrayList<>();
		offsets.add(0); // dummy for 0-index
		offsets.add(0); // line 1 starts at offset 0
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				offsets.add(i + 1);
			}
		}
		return offsets.stream().mapToInt(Integer::intValue).toArray();
	}

	/** Converts a JavaParser Position (1-indexed line/column) to a string offset. */
	private static int toOffset(int[] lineOffsets, Position pos) {
		if (pos.line < 1 || pos.line >= lineOffsets.length) {
			return -1;
		}
		return lineOffsets[pos.line] + pos.column - 1; // column is 1-indexed
	}

	private static final Pattern IMPORT_LINE = Pattern.compile("^[ \\t]*import\\s+[\\w.]+\\s*;", Pattern.MULTILINE);
	private static final Pattern PACKAGE_LINE = Pattern.compile("^\\s*package\\s+[\\w.]+\\s*;", Pattern.MULTILINE);

	/** Finds the best position to insert new import statements. */
	private static int findImportInsertPosition(String text) {
		Matcher m = IMPORT_LINE.matcher(text);
		int lastImportEnd = -1;
		while (m.find()) {
			lastImportEnd = m.end();
		}
		if (lastImportEnd >= 0) {
			return lastImportEnd;
		}
		Matcher pkg = PACKAGE_LINE.matcher(text);
		if (pkg.find()) {
			return pkg.end();
		}
		return 0;
	}
}
