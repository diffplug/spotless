/*
 * Copyright 2026 DiffPlug
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javassist.ClassPool;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/** The JavaParser setup shared by the steps that resolve names against the project's classpath. */
final class SymbolResolution {
	static {
		// If ClassPool is allowed to cache class files, it does not free the file-lock
		ClassPool.cacheOpenedJarFile = false;
	}

	private SymbolResolution() {}

	/** A parser that accepts every language level; JavaParser defaults to Java 11 and rejects newer syntax such as records. */
	static JavaParser parser() {
		return new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.BLEEDING_EDGE));
	}

	/** A type solver for the JDK and the given jars and source directories. Entries that do not exist are ignored. */
	static CombinedTypeSolver typeSolver(Collection<File> classpath) throws IOException {
		CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
		combinedTypeSolver.add(new ReflectionTypeSolver(ReflectionTypeSolver.JCL_ONLY));
		for (File element : classpath) {
			if (element.isFile() && element.getName().endsWith(".jar")) {
				combinedTypeSolver.add(new JarTypeSolver(element));
			} else if (element.isDirectory()) {
				combinedTypeSolver.add(new JavaParserTypeSolver(element));
			}
		}
		return combinedTypeSolver;
	}

	/**
	 * Like {@link #typeSolver(Collection)}, but parses the files of the source directories with the given configuration,
	 * which can have a symbol resolver, and only when a file there mentions the name being looked up.
	 */
	static CombinedTypeSolver typeSolver(Collection<File> classpath, ParserConfiguration sourceConfiguration) throws IOException {
		CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
		combinedTypeSolver.add(new ReflectionTypeSolver(ReflectionTypeSolver.JCL_ONLY));
		for (File element : classpath) {
			if (element.isFile() && element.getName().endsWith(".jar")) {
				combinedTypeSolver.add(new JarTypeSolver(element));
			} else if (element.isDirectory()) {
				combinedTypeSolver.add(new MentionFilter(element, sourceConfiguration));
			}
		}
		return combinedTypeSolver;
	}

	/**
	 * JavaParser caches a facade for every type solver in a static map, which keeps the type solver, and with it
	 * every jar it has read, alive for as long as JavaParser's classes are loaded. Clears it.
	 */
	static void release() {
		synchronized (JavaParserFacade.class) {
			// JavaParserFacade.get is static synchronized, clearInstances is not
			JavaParserFacade.clearInstances();
		}
	}

	/**
	 * A source directory that only asks JavaParser about a type when one of the files that could declare it mentions
	 * its name. When a package has no file named after the type, JavaParser parses every file of the package to look for
	 * a non-public top-level type of that name, and most of the names this step looks up are in no package at all.
	 */
	private static final class MentionFilter implements TypeSolver {
		private static final Pattern IDENTIFIER = Pattern.compile("[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*");
		private static final String ANY_NAME = "\\u";

		private final File root;
		private final ParserConfiguration configuration;
		private final Map<File, Set<String>> namesByDirectory = new HashMap<>();
		private TypeSolver parent;
		private JavaParserTypeSolver delegate;
		private RuntimeException failure;

		MentionFilter(File root, ParserConfiguration configuration) {
			this.root = root;
			this.configuration = configuration;
		}

		@Override
		public TypeSolver getParent() {
			return parent;
		}

		@Override
		public void setParent(TypeSolver parent) {
			this.parent = parent;
		}

		@Override
		public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
			return mayDeclare(name) ? delegate().tryToSolveType(name) : SymbolReference.unsolved();
		}

		@Override
		public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveTypeInModule(String qualifiedModuleName, String simpleTypeName) {
			return mayDeclare(simpleTypeName) ? delegate().tryToSolveTypeInModule(qualifiedModuleName, simpleTypeName) : SymbolReference.unsolved();
		}

		/** Created on first use, since it walks the whole directory, and remembered when that fails, e.g. for an unreadable folder. */
		private JavaParserTypeSolver delegate() {
			if (failure != null) {
				throw failure;
			}
			if (delegate == null) {
				try {
					JavaParserTypeSolver created = new JavaParserTypeSolver(root, configuration);
					created.setParent(parent);
					delegate = created;
				} catch (RuntimeException e) {
					failure = e;
					throw e;
				}
			}
			return delegate;
		}

		/** A type can only be declared in a file that mentions its name, in a directory of one of the packages the name could have. */
		private boolean mayDeclare(String name) {
			String[] segments = name.split("\\.");
			String simpleName = segments[segments.length - 1];
			for (int packageLength = 0; packageLength < segments.length; packageLength++) {
				File directory = new File(root, String.join("/", Arrays.copyOfRange(segments, 0, packageLength)));
				Set<String> names = namesIn(directory);
				if (names.contains(simpleName) || names.contains(ANY_NAME)) {
					return true;
				}
			}
			return false;
		}

		private Set<String> namesIn(File directory) {
			return namesByDirectory.computeIfAbsent(directory, dir -> {
				Set<String> names = new HashSet<>();
				File[] sources = dir.listFiles((parentDir, fileName) -> fileName.endsWith(".java"));
				if (sources == null) {
					return names;
				}
				for (File source : sources) {
					String content;
					try {
						// decoded like JavaParser decodes it, without failing on bytes that are not UTF-8
						content = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					if (content.contains(ANY_NAME)) {
						// a unicode escape can spell any name, so this file may declare anything
						names.add(ANY_NAME);
					}
					Matcher identifier = IDENTIFIER.matcher(content);
					while (identifier.find()) {
						names.add(identifier.group());
					}
				}
				return names;
			});
		}
	}
}
