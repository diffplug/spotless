/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.glue.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocInlineTag;
import com.github.javaparser.javadoc.description.JavadocSnippet;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import com.diffplug.spotless.FormatterFunc;

/**
 * Remove imports from a Java source file by analyzing {@link ImportDeclaration}.
 * <p>
 * More precisely, it will analyze each Tokens beinh used in the code-base, and remove imports not matching given import.
 * <p>
 * One limitation is it will not strip away wildcard imports.
 */
// https://github.com/javaparser/javaparser/issues/1590
// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java
public class JavaparserRemoveUnusedImportsFunc implements FormatterFunc {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaparserRemoveUnusedImportsFunc.class);

	public String apply(String input) throws InterruptedException, IOException {
		// We consider the source is very recent, as source is retro-compatible (i.e. not feature get deprecated)
		ParserConfiguration.LanguageLevel languageLevel = ParserConfiguration.LanguageLevel.BLEEDING_EDGE;
		ParseResult<CompilationUnit> parseResult = new JavaParser(new ParserConfiguration().setLanguageLevel(languageLevel)).parse(input);
		CompilationUnit unit = parseResult.getResult().orElseThrow(() -> {
			return new IllegalArgumentException("Issue parsing the input: " + parseResult.getProblems());
		});
		if (!parseResult.isSuccessful()) {
			throw new IllegalArgumentException("Issue parsing the input: " + parseResult.getProblems());
		}

		NodeList<ImportDeclaration> importDeclarations = unit.getImports();
		if (importDeclarations.isEmpty()) {
			return input;
		}

		Set<String> tokensInUse = tokensInUse(unit);

		List<ImportDeclaration> unusedImports = removeUnusedImports(importDeclarations, tokensInUse);

		if (unusedImports.isEmpty()) {
			return input;
		} else {
			// This is necessary to get, after mutations, a source as similar as possible to the original one
			LexicalPreservingPrinter.setup(unit);

			// Remove all unused imports
			unusedImports.forEach(importDeclaration -> {
				try {
					importDeclaration.remove();
				} catch (RuntimeException e) {
					throw new RuntimeException("Issue removing an import statement: " + importDeclaration, e);
				}
			});

			// print the CompilationUnit after mutations
			return LexicalPreservingPrinter.print(unit);
		}
	}

	/*
	 * Extract all of the tokens from the main body of the file.
	 *
	 * This set of tokens represents all of the file's dependencies, and is used to figure out whether
	 * or not an import is unused.
	 */
	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L278
	private static Set<String> tokensInUse(CompilationUnit unit) {
		// Extract tokens from the java code:
		Stream<Node> packageDecl = unit.getPackageDeclaration().isPresent()
				? Stream.of(unit.getPackageDeclaration().get()).map(PackageDeclaration::getAnnotations)
						.flatMap(NodeList::stream)
				: Stream.empty();
		Stream<String> typesInCode = Stream.concat(packageDecl, unit.getTypes().stream())
				.map(Node::getTokenRange).filter(Optional::isPresent).map(Optional::get)
				.filter(r -> r != TokenRange.INVALID).flatMap(r -> {
					// get all JavaTokens as strings from each range
					return StreamSupport.stream(r.spliterator(), false);
				}).map(JavaToken::asString);

		// Extract referenced class names from parsed javadoc comments:
		Stream<String> typesInJavadocs = unit.getAllComments().stream()
				.filter(c -> c instanceof JavadocComment).map(JavadocComment.class::cast)
				.map(JavadocComment::parse).flatMap(JavaparserRemoveUnusedImportsFunc::parseJavadoc);

		return Stream.concat(typesInCode, typesInJavadocs)
				.filter(t -> t != null && !t.isEmpty() && Character.isJavaIdentifierStart(t.charAt(0)))
				.collect(Collectors.toSet());
	}

	/*
	 * Remove unused imports.
	 *
	 * This algorithm only looks at the file itself, and evaluates whether or not a given import is
	 * unused, by checking if the last segment of the import path (typically a class name or a static
	 * function name) appears in the file.
	 *
	 * This means that it is not possible to remove import statements with wildcards.
	 */
	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L350
	private static List<ImportDeclaration> removeUnusedImports(Collection<ImportDeclaration> imports, Set<String> tokensInUse) {
		// We clone the input Collection as it typically reflects dynamically the imports from the CompilationUnit
		// Hence, removal during iteration may lead to issues
		imports = new ArrayList<>(imports);

		return imports.stream().filter(i -> {
			String[] segments = i.getNameAsString().split("[.]");
			if (segments.length == 0) {
				throw new AssertionError("Parse tree includes invalid import statements");
			}

			String lastSegment = segments[segments.length - 1];
			if (lastSegment.equals("*")) {
				return false;
			}

			return !tokensInUse.contains(lastSegment);
		}).collect(Collectors.toList());
	}

	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L366
	static void removeSamePackageImports(Set<ImportDeclaration> imports,
			Optional<PackageDeclaration> packageDeclaration) {
		String packageName = packageDeclaration.map(p -> p.getName().toString()).orElse("");
		imports.removeIf(i -> {
			String imp = i.getNameAsString();
			if (packageName.isEmpty()) {
				return !imp.contains(".");
			}
			return imp.startsWith(packageName) && imp.lastIndexOf(".") <= packageName.length();
		});
	}

	// parse both main doc description and any block tags
	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L304
	private static Stream<String> parseJavadoc(Javadoc javadoc) {
		// parse main doc description
		Stream<String> stringsFromJavadocDescription = Stream.of(javadoc.getDescription()).flatMap(JavaparserRemoveUnusedImportsFunc::parseJavadocDescription);
		// grab tag names and parsed descriptions for block tags
		Stream<String> stringsFromBlockTags = javadoc.getBlockTags().stream().flatMap(tag -> {
			// only @throws and @exception have names who are importable; @param and others don't
			EnumSet<JavadocBlockTag.Type> blockTagTypesWithImportableNames = EnumSet.of(JavadocBlockTag.Type.THROWS, JavadocBlockTag.Type.EXCEPTION);
			Stream<String> importableTagNames = blockTagTypesWithImportableNames.contains(tag.getType())
					? Stream.of(tag.getName()).filter(Optional::isPresent).map(Optional::get)
					: Stream.empty();
			Stream<String> tagDescriptions = Stream.of(tag.getContent()).flatMap(JavaparserRemoveUnusedImportsFunc::parseJavadocDescription);
			return Stream.concat(importableTagNames, tagDescriptions);
		});
		return Stream.concat(stringsFromJavadocDescription, stringsFromBlockTags);
	}

	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L323
	private static Stream<String> parseJavadocDescription(JavadocDescription description) {
		return description.getElements().stream().map(element -> {
			if (element instanceof JavadocInlineTag) {
				// inline tags like {@link Foo}
				return ((JavadocInlineTag) element).getContent();
			} else if (element instanceof JavadocSnippet) {
				// snippets like @see Foo
				return element.toText();
			} else {
				// try to handle unknown elements as best we can
				return element.toText();
			}
		}).flatMap(s -> {
			// split text descriptions into word tokens
			return Stream.of(s.split("\\W+"));
		});
	}
}
