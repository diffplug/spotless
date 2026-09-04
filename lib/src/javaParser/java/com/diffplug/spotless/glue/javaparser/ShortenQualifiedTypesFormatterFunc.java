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
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import com.diffplug.spotless.FormatterFunc;

/**
 * Shortens fully-qualified type references and adds the corresponding imports.
 *
 * <h3>Goal: never introduce a compile error</h3>
 *
 * <p>Spotless runs without a full classpath or type-resolution, so the formatter
 * cannot know every type visible to the compiler. The guiding principle is:
 * <em>"when in doubt, leave it alone."</em> An un-shortened reference is merely
 * verbose; a wrongly-shortened one breaks the build.
 *
 * <h4>What we can check exactly</h4>
 * <ul>
 *   <li><b>Type context (AST).</b> JavaParser tells us exactly which tokens are
 *       {@code ClassOrInterfaceType} nodes, so we never touch strings, comments,
 *       or non-type expressions when shortening type references.</li>
 *   <li><b>Import collisions.</b> If the simple name is already imported to a
 *       <em>different</em> FQN, we leave the reference qualified.</li>
 *   <li><b>Declared-type collisions.</b> If the simple name matches a class,
 *       enum, or record declared in the same file, we leave it.</li>
 *   <li><b>Unqualified-reference collisions.</b> If the simple name is already
 *       used unqualified elsewhere (possibly resolving to a same-package type),
 *       adding an import could silently change what it resolves to.</li>
 * </ul>
 *
 * <h4>Expression-context heuristics</h4>
 *
 * <p>FQTs also appear in expression context — static method calls
 * ({@code java.lang.management.ManagementFactory.getPlatformMXBeans(…)}),
 * static field / enum-constant access
 * ({@code java.util.concurrent.TimeUnit.SECONDS}), and nested-type member
 * access ({@code pkg.models.CustomTypeProperty.TypeEnum.STRING}).
 * JavaParser sees these as {@code FieldAccessExpr}/{@code MethodCallExpr},
 * not type nodes, so we apply two heuristics to avoid false positives:
 *
 * <ol>
 *   <li><b>Known-package check.</b> If the candidate FQN's package already
 *       appears in the file's imports, own package declaration, or is
 *       {@code java.lang}, we trust it.</li>
 *   <li><b>Minimum-depth fallback.</b> Otherwise we require at least two
 *       lowercase (package) segments before the first uppercase (type) segment.
 *       This filters out {@code variable.Field} patterns that look superficially
 *       like a FQT but are really field access on a local variable.
 *       <br>In theory this could skip a legitimate single-segment package
 *       (e.g. {@code a.MyType.method()}) that has no import in the file yet,
 *       but single-segment packages are virtually non-existent in practice.</li>
 * </ol>
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

		// 3. Collect type names declared in this file (top-level + nested)
		Set<String> declaredTypeNames = new LinkedHashSet<>();
		cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> declaredTypeNames.add(c.getNameAsString()));
		cu.findAll(EnumDeclaration.class).forEach(c -> declaredTypeNames.add(c.getNameAsString()));
		cu.findAll(RecordDeclaration.class).forEach(c -> declaredTypeNames.add(c.getNameAsString()));

		// 4. Collect unqualified type references, which may resolve to types in the same package
		Set<String> unqualifiedTypeNames = new LinkedHashSet<>();
		cu.findAll(ClassOrInterfaceType.class).stream()
				.filter(type -> type.getScope().isEmpty())
				.forEach(type -> unqualifiedTypeNames.add(type.getNameAsString()));

		// 5. Build set of known packages from existing imports, own package, and java.lang
		Set<String> knownPackages = new LinkedHashSet<>();
		knownPackages.add("java.lang");
		if (!packageName.isEmpty()) {
			knownPackages.add(packageName);
		}
		for (String fqn : existingImportFqns) {
			int lastDot = fqn.lastIndexOf('.');
			if (lastDot > 0) {
				knownPackages.add(fqn.substring(0, lastDot));
			}
		}

		// 6. Walk the AST to find outermost fully-qualified type nodes
		Map<String, Set<String>> simpleToFqns = new LinkedHashMap<>();
		List<QualifiedTypeRef> qualifiedRefs = new ArrayList<>();

		cu.accept(new CollectQualifiedTypesVisitor(simpleToFqns, qualifiedRefs, knownPackages), null);

		if (qualifiedRefs.isEmpty()) {
			return rawUnix;
		}

		// 7. Determine which FQNs are safe to shorten
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
			// Skip if simple name clashes with a type declared or already referenced in this file
			if (declaredTypeNames.contains(simple)
					|| (existing == null && unqualifiedTypeNames.contains(simple) && !isImplicitlyImported(fqn, packageName))) {
				continue;
			}
			safeToShorten.add(fqn);
		}

		if (safeToShorten.isEmpty()) {
			return rawUnix;
		}

		// 8. Convert line/column positions to string offsets and replace
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

		// 9. Add missing imports
		Set<String> newImports = new TreeSet<>();
		for (String fqn : safeToShorten) {
			if (isImplicitlyImported(fqn, packageName)) {
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
		private final Set<String> knownPackages;

		CollectQualifiedTypesVisitor(Map<String, Set<String>> simpleToFqns, List<QualifiedTypeRef> qualifiedRefs,
				Set<String> knownPackages) {
			this.simpleToFqns = simpleToFqns;
			this.qualifiedRefs = qualifiedRefs;
			this.knownPackages = knownPackages;
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

		@Override
		public void visit(MethodCallExpr expr, Void arg) {
			super.visit(expr, arg);
			expr.getScope().ifPresent(this::processExpressionScope);
		}

		@Override
		public void visit(FieldAccessExpr expr, Void arg) {
			super.visit(expr, arg);
			if (expr.getParentNode().isPresent()) {
				Node parent = expr.getParentNode().get();
				if (parent instanceof FieldAccessExpr fa && fa.getScope() == expr) {
					return;
				}
				if (parent instanceof MethodCallExpr mc && mc.getScope().isPresent() && mc.getScope().get() == expr) {
					return;
				}
			}
			processExpressionScope(expr);
		}

		/** Extracts a fully-qualified type from an expression chain of FieldAccessExpr/NameExpr nodes. */
		private void processExpressionScope(Expression expr) {
			List<FieldAccessExpr> chain = new ArrayList<>();
			Expression current = expr;
			while (current instanceof FieldAccessExpr fa) {
				chain.add(0, fa);
				current = fa.getScope();
			}
			if (!(current instanceof NameExpr ne)) {
				return;
			}
			String rootName = ne.getNameAsString();
			if (rootName.isEmpty() || !Character.isLowerCase(rootName.charAt(0))) {
				return;
			}
			int typeIdx = -1;
			for (int i = 0; i < chain.size(); i++) {
				String name = chain.get(i).getNameAsString();
				if (!name.isEmpty() && Character.isUpperCase(name.charAt(0))) {
					typeIdx = i;
					break;
				}
			}
			if (typeIdx < 0) {
				return;
			}
			StringBuilder fqn = new StringBuilder(rootName);
			for (int i = 0; i <= typeIdx; i++) {
				fqn.append('.').append(chain.get(i).getNameAsString());
			}
			String fqnStr = fqn.toString();
			String candidatePackage = fqnStr.substring(0, fqnStr.lastIndexOf('.'));
			// Trust if package is known from imports; otherwise require ≥2 package segments
			if (!knownPackages.contains(candidatePackage) && (typeIdx + 1) < 2) {
				return;
			}
			String simple = chain.get(typeIdx).getNameAsString();
			FieldAccessExpr typeNode = chain.get(typeIdx);
			Expression typeScope = typeNode.getScope();
			if (typeScope.getBegin().isPresent() && typeNode.getName().getBegin().isPresent()) {
				simpleToFqns.computeIfAbsent(simple, k -> new LinkedHashSet<>()).add(fqnStr);
				qualifiedRefs.add(new QualifiedTypeRef(fqnStr, simple,
						typeScope.getBegin().get(), typeNode.getName().getBegin().get()));
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

	private static boolean isImplicitlyImported(String fqn, String packageName) {
		return fqn.startsWith("java.lang.") && fqn.indexOf('.', 10) == -1
				|| !packageName.isEmpty() && fqn.startsWith(packageName + ".")
						&& fqn.indexOf('.', packageName.length() + 1) == -1;
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
