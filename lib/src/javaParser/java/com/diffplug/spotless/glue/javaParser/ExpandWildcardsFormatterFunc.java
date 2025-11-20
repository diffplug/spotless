/*
 * Copyright 2023-2025 DiffPlug
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import javassist.ClassPool;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Lint;

public class ExpandWildcardsFormatterFunc implements FormatterFunc.NeedsFile {

	private final JavaParser parser;
	static {
		// If ClassPool is allowed to cache class files, it does not free the file-lock
		ClassPool.cacheOpenedJarFile = false;
	}

	public ExpandWildcardsFormatterFunc(Collection<File> typeSolverClasspath) throws IOException {
		this.parser = new JavaParser();

		CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
		combinedTypeSolver.add(new ReflectionTypeSolver());
		for (File element : typeSolverClasspath) {
			if (element.isFile()) {
				combinedTypeSolver.add(new JarTypeSolver(element));
			} else if (element.isDirectory()) {
				combinedTypeSolver.add(new JavaParserTypeSolver(element));
			} // gracefully ignore non-existing src-directories
		}

		SymbolResolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
		parser.getParserConfiguration().setSymbolResolver(symbolSolver);
	}

	@Override
	public String applyWithFile(String rawUnix, File file) throws Exception {
		Optional<CompilationUnit> parseResult = parser.parse(rawUnix).getResult();
		if (parseResult.isEmpty()) {
			return rawUnix;
		}
		CompilationUnit cu = parseResult.get();
		Map<ImportDeclaration, Set<ImportDeclaration>> importMap = findWildcardImports(cu)
				.stream()
				.collect(toMap(Function.identity(),
						t -> new TreeSet<>(Comparator.comparing(ImportDeclaration::getNameAsString))));
		if (importMap.isEmpty()) {
			// No wildcards found => do not change anything
			return rawUnix;
		}

		cu.accept(new CollectImportedTypesVisitor(), importMap);
		for (var entry : importMap.entrySet()) {
			String pattern = Pattern.quote(LineEnding.toUnix(entry.getKey().toString()));
			String replacement = entry.getValue().stream().map(ImportDeclaration::toString).collect(joining());
			rawUnix = rawUnix.replaceAll(pattern, replacement);
		}

		return rawUnix;
	}

	private List<ImportDeclaration> findWildcardImports(CompilationUnit cu) {
		List<ImportDeclaration> wildcardImports = new ArrayList<>();
		for (ImportDeclaration importDeclaration : cu.getImports()) {
			if (importDeclaration.isAsterisk()) {
				wildcardImports.add(importDeclaration);
			}
		}
		return wildcardImports;
	}

	private static final class CollectImportedTypesVisitor
			extends VoidVisitorAdapter<Map<ImportDeclaration, Set<ImportDeclaration>>> {

		@Override
		public void visit(final ClassOrInterfaceType n,
				final Map<ImportDeclaration, Set<ImportDeclaration>> importMap) {
			// default imports
			ResolvedType resolvedType = wrapUnsolvedSymbolException(n, ClassOrInterfaceType::resolve);
			if (resolvedType.isReference()) {
				matchTypeName(importMap, resolvedType.asReferenceType().getQualifiedName(), false);
			}
			super.visit(n, importMap);
		}

		private void matchTypeName(Map<ImportDeclaration, Set<ImportDeclaration>> importMap, String qualifiedName,
				boolean isStatic) {
			for (var entry : importMap.entrySet()) {
				if (entry.getKey().isStatic() == isStatic
						&& qualifiedName.startsWith(entry.getKey().getName().asString())) {
					entry.getValue().add(new ImportDeclaration(qualifiedName, isStatic, false));
					break;
				}
			}
		}

		@Override
		public void visit(final MarkerAnnotationExpr n,
				final Map<ImportDeclaration, Set<ImportDeclaration>> importMap) {
			visitAnnotation(n, importMap);
			super.visit(n, importMap);
		}

		@Override
		public void visit(final SingleMemberAnnotationExpr n,
				final Map<ImportDeclaration, Set<ImportDeclaration>> importMap) {
			visitAnnotation(n, importMap);
			super.visit(n, importMap);
		}

		@Override
		public void visit(final NormalAnnotationExpr n,
				final Map<ImportDeclaration, Set<ImportDeclaration>> importMap) {
			visitAnnotation(n, importMap);
			super.visit(n, importMap);
		}

		private void visitAnnotation(final AnnotationExpr n,
				final Map<ImportDeclaration, Set<ImportDeclaration>> importMap) {
			ResolvedAnnotationDeclaration resolvedType = wrapUnsolvedSymbolException(n, AnnotationExpr::resolve);
			matchTypeName(importMap, resolvedType.getQualifiedName(), false);
		}

		@Override
		public void visit(final MethodCallExpr n, final Map<ImportDeclaration, Set<ImportDeclaration>> importMap) {
			// static imports
			ResolvedMethodDeclaration resolved = wrapUnsolvedSymbolException(n, MethodCallExpr::resolve);
			if (resolved.isStatic()) {
				matchTypeName(importMap, resolved.getQualifiedName(), true);
			}
			super.visit(n, importMap);
		}

		private static <T extends Node, R> R wrapUnsolvedSymbolException(T node, Function<T, R> func) {
			try {
				return func.apply(node);
			} catch (UnsolvedSymbolException ex) {
				if (node.getBegin().isPresent() && node.getEnd().isPresent()) {
					throw Lint.atLineRange(node.getBegin().get().line, node.getEnd().get().line, "UnsolvedSymbolException", ex.getMessage()).shortcut();
				}
				if (node.getBegin().isPresent()) {
					throw Lint.atLine(node.getBegin().get().line, "UnsolvedSymbolException", ex.getMessage()).shortcut();
				} else if (node.getEnd().isPresent()) {
					throw Lint.atLine(node.getEnd().get().line, "UnsolvedSymbolException", ex.getMessage()).shortcut();
				} else {
					throw Lint.atUndefinedLine("UnsolvedSymbolException", ex.getMessage()).shortcut();
				}
			}
		}

	}

}
