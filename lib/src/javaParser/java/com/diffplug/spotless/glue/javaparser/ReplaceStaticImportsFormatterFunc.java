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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.nodeTypes.SwitchNode;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.HasAccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;

import com.diffplug.spotless.FormatterFunc;

/**
 * Replaces static imports with regular imports and qualified references:
 * {@code import static org.junit.jupiter.api.Assertions.assertTrue;} and {@code assertTrue(x)} become
 * {@code import org.junit.jupiter.api.Assertions;} and {@code Assertions.assertTrue(x)}.
 *
 * <h3>Goal: never introduce a compile error or change behaviour</h3>
 *
 * <p>Each static import is either replaced completely or left exactly as it is. It is replaced only
 * when every occurrence of every name it can supply is known to be either
 * <ul>
 *   <li>a use of the import, which gets qualified with the imported type, or</li>
 *   <li>something else, such as a local variable, a member of an enclosing class, or the label of a
 *       {@code switch} on an enum, which stays as it is.</li>
 * </ul>
 * Anything else keeps the import: a name the classpath cannot resolve, a supertype it cannot see, a name
 * that several imports supply, a use inside a {@code spotless:off} region or a doc comment, and so on.
 *
 * <h3>How occurrences are classified</h3>
 *
 * <p>JavaParser's resolution of method calls is not reliable enough to decide this (overloads, generics,
 * lambdas, the order of imports), so the scoping rules of the JLS are applied directly, and JavaParser is
 * mostly asked about types: the members of an imported type, and the members an enclosing class declares
 * or inherits.
 * <ul>
 *   <li>A method, field or member type that an enclosing class declares or inherits hides a static import
 *       of the same name (JLS 6.4.1, 15.12.1), so such an occurrence is not a use of the import. Private
 *       members and static interface methods are not inherited (JLS 8.2, 8.4.8), and a class whose
 *       supertypes cannot all be resolved, or that uses Lombok, has members this step cannot see.</li>
 *   <li>A local variable, parameter or pattern variable hides a static import of the same name.</li>
 *   <li>Otherwise the name can only come from a static import. If exactly one imported type supplies it,
 *       the occurrence is a use of that import; if several do, or an import the classpath cannot resolve
 *       could, the imports are left alone.</li>
 *   <li>An unqualified enum constant used as a {@code case} label of a switch on an enum is resolved against
 *       the enum, not through the import (JLS 14.11.1), so it is left alone. In a switch on another type,
 *       which Java 21 allows, it is an ordinary name.</li>
 * </ul>
 *
 * <h3>Qualifying a use</h3>
 *
 * <p>A use of {@code member} from type {@code T} becomes {@code T.member}, written with the simple name of
 * {@code T} (or {@code Outer.Inner} for a nested type), adding {@code import pkg.Outer;} when needed. If that
 * simple name means something else at the use, or the new import could change what another name in the file
 * refers to, the fully qualified name is written instead. A static import of a member type becomes a regular
 * import of that type, and its uses stay as they are.
 *
 * <p>Writing just {@code Math} relies on there being no other type of that name in the file's package, or in a
 * package imported on demand. The step checks this against the classpath, and a package that is not on it may
 * hold any type, so such names get the fully qualified form. Types missing from a package that is on it, such as
 * generated sources, are assumed not to exist.
 *
 * <p>Edits are made to the text at the positions JavaParser reports, so formatting is preserved. New
 * imports are added after the existing ones, so this step is meant to run before {@code importOrder()}.
 */
public class ReplaceStaticImportsFormatterFunc implements FormatterFunc.Closeable {
	private static final Pattern STATIC_IMPORT = Pattern.compile("\\bimport\\s+static\\b");
	private static final Pattern MULTI_RELEASE = Pattern.compile("^META-INF/versions/\\d+/");
	/** A unicode escape of a line terminator, which javac reads as a line break, e.g. hiding code behind a {@code //}. */
	private static final Pattern ESCAPED_LINE_TERMINATOR = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*\\\\u+000[aAdD]");
	/** The regions that {@code toggleOffOn()} restores after formatting, for the default markers and the common Eclipse ones. */
	private static final List<Pattern> FENCES = List.of(
			Pattern.compile("spotless:off[\\s\\S]*?spotless:on"),
			Pattern.compile("@formatter:off[\\s\\S]*?@formatter:on"));
	/** Unqualified references in doc comments: {@code {@link name}}, {@code @see name}, {@code @throws name}... */
	private static final Pattern DOC_REFERENCE = Pattern.compile("(?:\\{@(?:link|linkplain|value)|@(?:see|throws|exception))\\s+([A-Za-z_$][\\w$]*)(?![\\w$.#])");
	/** Unqualified references in Markdown doc comments: {@code [name]} and {@code [name(int)]}. */
	private static final Pattern MARKDOWN_REFERENCE = Pattern.compile("\\[([A-Za-z_$][\\w$]*)(?:\\([^)\\]]*\\))?\\]");
	/** Methods that every class, enum, record and interface has as members (JLS 4.3.2, 9.2). */
	private static final Set<String> OBJECT_METHODS = Set.of("clone", "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait");

	private final Collection<File> typeSolverClasspath;
	private final Set<String> excludes;
	/** Reads unicode escapes like javac does, so that a name spelled with them is seen; positions stay those of the text. */
	private final JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.BLEEDING_EDGE).setPreprocessUnicodeEscapes(true));
	/** The packages of each jar on the classpath, read when first needed. */
	private final Map<File, Set<String>> jarPackages = new HashMap<>();
	private final Map<String, Boolean> knownPackages = new HashMap<>();
	/** How many classpath entries have a top-level class, by its path such as {@code com/acme/Base}. */
	private final Map<String, Integer> copiesOnClasspath = new HashMap<>();
	private static volatile Set<String> jdkPackages;
	/** Created when the first file with a static import is formatted, since building it reads every jar. */
	private TypeSolver typeSolver;
	private JavaSymbolSolver symbolSolver;
	private boolean typeSolverFailed;

	public ReplaceStaticImportsFormatterFunc(Collection<File> typeSolverClasspath, Collection<String> excludes) {
		this.typeSolverClasspath = List.copyOf(typeSolverClasspath);
		this.excludes = Set.copyOf(excludes);
	}

	/** Synchronized because JavaParser's parser and type solver caches are not thread-safe. */
	@Override
	public synchronized String apply(String rawUnix) {
		if (!STATIC_IMPORT.matcher(rawUnix).find() || ESCAPED_LINE_TERMINATOR.matcher(rawUnix).find()) {
			// the edits rely on the lines of the text being the lines that javac sees
			return rawUnix;
		}
		try {
			ParseResult<CompilationUnit> parsed = parser.parse(rawUnix);
			if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
				return rawUnix;
			}
			CompilationUnit cu = parsed.getResult().get();
			if (cu.getImports().stream().noneMatch(ImportDeclaration::isStatic) || cu.getModule().isPresent() || !initializeTypeSolver()) {
				return rawUnix;
			}
			symbolSolver.inject(cu);
			String result = new Rewrite(cu, rawUnix).apply();
			if (result.equals(rawUnix) || !parser.parse(result).isSuccessful()) {
				return rawUnix;
			}
			return result;
		} catch (RuntimeException | StackOverflowError e) {
			// JavaParser can fail in many ways on code it does not fully understand: when in doubt, leave the file alone
			return rawUnix;
		}
	}

	/** Creates the type solver on first use. False if that failed, e.g. for an unreadable jar: then every file stays as it is. */
	private boolean initializeTypeSolver() {
		if (symbolSolver == null && !typeSolverFailed) {
			try {
				// the files of the source directories need symbol resolution too, e.g. to find the type that contains a nested one
				ParserConfiguration sourceConfiguration = new ParserConfiguration().setLanguageLevel(LanguageLevel.BLEEDING_EDGE).setPreprocessUnicodeEscapes(true);
				TypeSolver solver = SymbolResolution.typeSolver(typeSolverClasspath, sourceConfiguration);
				JavaSymbolSolver resolver = new JavaSymbolSolver(solver);
				sourceConfiguration.setSymbolResolver(resolver);
				typeSolver = solver;
				symbolSolver = resolver;
			} catch (IOException | RuntimeException e) {
				typeSolverFailed = true;
			}
		}
		return symbolSolver != null;
	}

	/** Releases the type solver, which JavaParser would otherwise keep in a static cache for as long as its classes are loaded. */
	@Override
	public synchronized void close() {
		typeSolver = null;
		symbolSolver = null;
		SymbolResolution.release();
	}

	/**
	 * Whether the type solver can see the package, so that not finding a type in it means the type does not exist.
	 * A package it cannot see, e.g. generated code or another module's classes, may hold any type.
	 */
	private boolean isKnownPackage(String packageName) {
		return knownPackages.computeIfAbsent(packageName, this::findPackage);
	}

	private boolean findPackage(String packageName) {
		if (jdkPackages().contains(packageName)) {
			return true;
		}
		String path = packageName.replace('.', '/');
		for (File entry : typeSolverClasspath) {
			if (entry.isDirectory()) {
				// only sources count: the type solver cannot read the class files of an output directory
				File[] sources = new File(entry, path).listFiles((dir, name) -> name.endsWith(".java"));
				if (sources != null && sources.length > 0) {
					return true;
				}
			} else if (entry.isFile() && entry.getName().endsWith(".jar") && jarPackages.computeIfAbsent(entry, ReplaceStaticImportsFormatterFunc::packagesOf).contains(packageName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether more than one jar on the classpath has the top-level class of the type, e.g. two versions of a library.
	 * A source directory and the classes compiled from it hold the same class, so directories do not count.
	 */
	private boolean isOnClasspathTwice(ResolvedReferenceTypeDeclaration type) {
		String packageName = type.getPackageName();
		if (jdkPackages().contains(packageName)) {
			return false;
		}
		String topLevel = type.getClassName().split("\\.")[0];
		String path = packageName.isEmpty() ? topLevel : packageName.replace('.', '/') + "/" + topLevel;
		return copiesOnClasspath.computeIfAbsent(path, this::countCopies) > 1;
	}

	private int countCopies(String path) {
		int copies = 0;
		String packageName = path.contains("/") ? path.substring(0, path.lastIndexOf('/')).replace('/', '.') : "";
		for (File entry : typeSolverClasspath) {
			if (entry.isFile() && entry.getName().endsWith(".jar")
					&& jarPackages.computeIfAbsent(entry, ReplaceStaticImportsFormatterFunc::packagesOf).contains(packageName)) {
				try (ZipFile zip = new ZipFile(entry)) {
					if (zip.getEntry(path + ".class") != null) {
						copies++;
					}
				} catch (IOException e) {
					copies++; // unreadable: assume it may have the class
				}
			}
		}
		return copies;
	}

	private static Set<String> packagesOf(File jar) {
		Set<String> packages = new HashSet<>();
		try (ZipFile zip = new ZipFile(jar)) {
			zip.stream().map(ZipEntry::getName).filter(name -> name.endsWith(".class")).forEach(name -> {
				String path = MULTI_RELEASE.matcher(name).replaceFirst("");
				int slash = path.lastIndexOf('/');
				packages.add(slash < 0 ? "" : path.substring(0, slash).replace('/', '.'));
			});
		} catch (IOException e) {
			// an unreadable jar shows no packages, so nothing is assumed about them
		}
		return packages;
	}

	private static Set<String> jdkPackages() {
		Set<String> packages = jdkPackages;
		if (packages == null) {
			packages = new HashSet<>();
			for (Module module : ModuleLayer.boot().modules()) {
				packages.addAll(module.getPackages());
			}
			jdkPackages = packages;
		}
		return packages;
	}

	/** The kinds of member a static import can supply. */
	private enum Kind {
		METHOD, FIELD, TYPE
	}

	/** One resolved static import declaration, and what is known about it. */
	private static final class StaticImport {
		final ImportDeclaration declaration;
		final ResolvedReferenceTypeDeclaration type;
		final Map<Kind, Set<String>> names = new EnumMap<>(Kind.class);
		final Set<String> enumConstants = new HashSet<>();
		/** Canonical names of the member types declared in the imported type itself, by simple name. */
		final Map<String, String> declaredMemberTypes = new HashMap<>();
		/** The parameter counts of the imported type's instance methods, which compete with its static ones once a call is qualified. */
		final Map<String, List<Arity>> instanceMethods = new HashMap<>();
		/** The types that declare the static methods and fields this import supplies, by name. */
		final Map<String, Set<String>> declaringTypes = new HashMap<>();
		/** True when this import must stay, because it is excluded or some occurrence could not be classified with certainty. */
		boolean keep;

		StaticImport(ImportDeclaration declaration, ResolvedReferenceTypeDeclaration type) {
			this.declaration = declaration;
			this.type = type;
			for (Kind kind : Kind.values()) {
				names.put(kind, new HashSet<>());
			}
		}

		boolean supplies(Kind kind, String name) {
			return names.get(kind).contains(name);
		}

		boolean suppliesAnything(String name) {
			return supplies(Kind.METHOD, name) || supplies(Kind.FIELD, name) || supplies(Kind.TYPE, name);
		}

		void retainOnly(String member) {
			for (Set<String> set : names.values()) {
				set.retainAll(Set.of(member));
			}
			enumConstants.retainAll(Set.of(member));
		}
	}

	/** How many arguments a method takes. */
	private record Arity(int parameters, boolean varargs) {
		boolean accepts(int arguments) {
			return varargs ? arguments >= parameters - 1 : arguments == parameters;
		}
	}

	/** An occurrence that uses a static import, and so will be qualified. */
	private record Use(Node node, String name, StaticImport source) {}

	/** What the enclosing classes of an occurrence say about a name. */
	private enum Membership {
		/** An enclosing class certainly declares or inherits a member of that name. */
		MEMBER,
		/** No enclosing class declares or inherits a member of that name. */
		NOT_MEMBER,
		/** It cannot be decided, e.g. because a supertype cannot be resolved. */
		UNKNOWN
	}

	/** The member names of one kind that a class has. */
	private static final class Members {
		/** Names it certainly has: declared in it, or inherited from a supertype. */
		final Set<String> certain = new HashSet<>();
		/** Names it may have, such as package-private members of a supertype. */
		final Set<String> possible = new HashSet<>();
		/** False when some members cannot be seen, e.g. because a supertype cannot be resolved. */
		boolean complete = true;

		void add(String name, boolean isCertain) {
			possible.add(name);
			if (isCertain) {
				certain.add(name);
			}
		}
	}

	/** A class around an occurrence, and whether the occurrence is in its body, where its members are in scope. */
	private record EnclosingScope(Node node, boolean inBody) {}

	/** What a simple type name refers to at some point of the file. */
	private enum Visible {
		/** No type of that name is visible. */
		NOTHING,
		/** The expected type. */
		EXPECTED,
		/** A top-level type from the same package, an on-demand import or {@code java.lang}, which a single-type import would shadow. */
		SHADOWABLE,
		/** Another type, such as a member type in scope, which an import cannot shadow. */
		OTHER,
		/** It cannot be determined. */
		UNKNOWN
	}

	/** How to qualify a use: the text to write before it, and the import that text needs, or null. */
	private record Qualifier(String text, String importName) {}

	/** The edits that replace one static import's uses, and the imports they need. */
	private static final class Plan {
		final List<Edit> insertions = new ArrayList<>();
		final Set<String> imports = new TreeSet<>();
	}

	private record Edit(int start, int end, String replacement) {}

	/** The work for one file. */
	private final class Rewrite {
		private final CompilationUnit cu;
		private final String text;
		private final int[] lineOffsets;
		private final String packageName;
		private final List<StaticImport> imports = new ArrayList<>();
		/** Members named by single static imports that cannot be resolved: any occurrence of these names may use them. */
		private final Set<String> unresolvedNames = new HashSet<>();
		/** Simple name -> canonical name, for the non-static single-type imports. */
		private final Map<String, String> typeImports = new HashMap<>();
		private final Set<String> declaredTypeNames = new HashSet<>();
		private final Set<String> declaredTypeQualifiedNames = new HashSet<>();
		private final Set<String> typeParameterNames = new HashSet<>();
		private final Set<String> variableNames = new HashSet<>();
		/** The text ranges that {@code toggleOffOn()} would restore, as start and end offsets. */
		private final List<int[]> fences = new ArrayList<>();
		private final Map<Node, Map<Kind, Members>> membersCache = new IdentityHashMap<>();
		private final Map<String, Boolean> usedUnqualifiedCache = new HashMap<>();
		private final List<Use> uses = new ArrayList<>();
		/** Canonical names of the member types to import, for each static import that supplies them. */
		private final Map<StaticImport, Set<String>> memberTypeImports = new IdentityHashMap<>();
		/** Simple name -> canonical name, for the imports planned so far for qualified uses. */
		private final Map<String, String> plannedSimpleNames = new HashMap<>();

		Rewrite(CompilationUnit cu, String text) {
			this.cu = cu;
			this.text = text;
			this.lineOffsets = lineOffsets(text);
			this.packageName = cu.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
		}

		String apply() {
			collectFileFacts();
			for (ImportDeclaration declaration : cu.getImports()) {
				if (isFenced(declaration)) {
					return text; // toggleOffOn() would restore it, whatever this step does
				}
			}
			for (ImportDeclaration declaration : cu.getImports()) {
				if (!declaration.isStatic()) {
					continue;
				}
				Optional<StaticImport> resolved = resolveImport(declaration);
				if (resolved.isPresent()) {
					resolved.get().keep |= isExcluded(declaration);
					imports.add(resolved.get());
				} else if (declaration.isAsterisk()) {
					return text; // it could supply any name, so no name in the file can be attributed with certainty
				} else {
					unresolvedNames.add(declaration.getName().getIdentifier());
				}
			}
			if (imports.stream().allMatch(i -> i.keep)) {
				return text;
			}
			classifyOccurrences();
			keepImportsReferencedInComments();
			return edit();
		}

		private void collectFileFacts() {
			for (ImportDeclaration declaration : cu.getImports()) {
				if (!declaration.isStatic() && !declaration.isAsterisk() && !declaration.isModule()) {
					typeImports.put(declaration.getName().getIdentifier(), declaration.getNameAsString());
				}
			}
			for (TypeDeclaration<?> declaration : cu.findAll(TypeDeclaration.class)) {
				declaredTypeNames.add(declaration.getNameAsString());
				declaration.getFullyQualifiedName().ifPresent(declaredTypeQualifiedNames::add);
			}
			for (TypeParameter parameter : cu.findAll(TypeParameter.class)) {
				declaredTypeNames.add(parameter.getNameAsString());
				typeParameterNames.add(parameter.getNameAsString());
			}
			for (VariableDeclarator variable : cu.findAll(VariableDeclarator.class)) {
				variableNames.add(variable.getNameAsString());
			}
			for (Parameter parameter : cu.findAll(Parameter.class)) {
				variableNames.add(parameter.getNameAsString());
			}
			for (TypePatternExpr pattern : cu.findAll(TypePatternExpr.class)) {
				variableNames.add(pattern.getNameAsString());
			}
			for (EnumConstantDeclaration constant : cu.findAll(EnumConstantDeclaration.class)) {
				variableNames.add(constant.getNameAsString());
			}
			for (Pattern fence : FENCES) {
				Matcher matcher = fence.matcher(text);
				while (matcher.find()) {
					fences.add(new int[]{matcher.start(), matcher.end()});
				}
			}
		}

		/**
		 * {@code Type.*}, or just {@code Type}, keeps every static import from that type. {@code Type.member} keeps the
		 * static import of that member, and an on-demand import of the type if the file uses that member through it.
		 */
		private boolean isExcluded(ImportDeclaration declaration) {
			String name = declaration.getNameAsString();
			if (declaration.isAsterisk()) {
				return excludes.contains(name + ".*") || excludes.contains(name) || usesExcludedMember(name);
			}
			String type = declaration.getName().getQualifier().map(Name::asString).orElse("");
			return excludes.contains(name) || excludes.contains(type + ".*") || excludes.contains(type);
		}

		private boolean usesExcludedMember(String type) {
			for (String exclude : excludes) {
				String member = exclude.startsWith(type + ".") ? exclude.substring(type.length() + 1) : "";
				if (!member.isEmpty() && !member.contains(".") && !member.equals("*")
						&& cu.findAll(SimpleName.class).stream().anyMatch(name -> name.getIdentifier().equals(member))) {
					return true;
				}
			}
			return false;
		}

		private boolean isFenced(Node node) {
			if (fences.isEmpty()) {
				return false;
			}
			int begin = node.getBegin().map(this::offset).orElse(-1);
			for (int[] fence : fences) {
				if (begin < 0 || (fence[0] <= begin && begin < fence[1])) {
					return true;
				}
			}
			return false;
		}

		// ---------------------------------------------------------------- the imports

		/** Resolves the imported type and the names it supplies; empty if that cannot be done with certainty. */
		private Optional<StaticImport> resolveImport(ImportDeclaration declaration) {
			String typeName = declaration.isAsterisk()
					? declaration.getNameAsString()
					: declaration.getName().getQualifier().map(Name::asString).orElse("");
			if (typeName.isEmpty()) {
				return Optional.empty();
			}
			try {
				SymbolReference<ResolvedReferenceTypeDeclaration> reference = typeSolver.tryToSolveType(typeName);
				if (!reference.isSolved()) {
					return Optional.empty();
				}
				ResolvedReferenceTypeDeclaration type = reference.getCorrespondingDeclaration();
				if (!typeName.equals(type.getQualifiedName())) {
					return Optional.empty(); // not the canonical name, so something does not match what the compiler sees
				}
				requireCompleteAncestry(type, new HashSet<>());
				StaticImport staticImport = new StaticImport(declaration, type);
				collectMembers(staticImport);
				if (!declaration.isAsterisk()) {
					String member = declaration.getName().getIdentifier();
					staticImport.retainOnly(member);
					if (!staticImport.suppliesAnything(member)) {
						return Optional.empty(); // the type solver does not see this member
					}
				}
				return Optional.of(staticImport);
			} catch (RuntimeException e) {
				return Optional.empty();
			}
		}

		/** Collects the static members that a static import of the type supplies to this file (JLS 6.6, 7.5.3, 7.5.4, 8.4.8). */
		private void collectMembers(StaticImport staticImport) {
			ResolvedReferenceTypeDeclaration type = staticImport.type;
			Set<ResolvedMethodDeclaration> methods = new LinkedHashSet<>(type.getDeclaredMethods());
			type.getAllMethods().forEach(method -> methods.add(method.getDeclaration()));
			for (ResolvedMethodDeclaration method : methods) {
				ResolvedReferenceTypeDeclaration declaringType = method.declaringType();
				if (!method.isStatic()) {
					if (method.accessSpecifier() != AccessSpecifier.PRIVATE) {
						staticImport.instanceMethods.computeIfAbsent(method.getName(), name -> new ArrayList<>())
								.add(new Arity(method.getNumberOfParams(), method.hasVariadicParameter()));
					}
				} else if (isImportable(method.accessSpecifier(), declaringType)
						&& !(declaringType.isInterface() && !declaringType.getQualifiedName().equals(type.getQualifiedName()))) {
					// static methods of an interface are not inherited
					staticImport.names.get(Kind.METHOD).add(method.getName());
					staticImport.declaringTypes.computeIfAbsent(method.getName(), name -> new HashSet<>()).add(declaringType.getQualifiedName());
				}
			}
			for (ResolvedFieldDeclaration field : type.getAllFields()) {
				boolean implicitlyPublicStatic = isInterfaceOrAnnotation(field.declaringType());
				if ((field.isStatic() || implicitlyPublicStatic)
						&& (implicitlyPublicStatic || isImportable(field.accessSpecifier(), field.declaringType()))) {
					staticImport.names.get(Kind.FIELD).add(field.getName());
					staticImport.declaringTypes.computeIfAbsent(field.getName(), name -> new HashSet<>()).add(field.declaringType().getQualifiedName());
				}
			}
			if (type.isEnum()) {
				for (var constant : type.asEnum().getEnumConstants()) {
					staticImport.names.get(Kind.FIELD).add(constant.getName());
					staticImport.declaringTypes.computeIfAbsent(constant.getName(), name -> new HashSet<>()).add(type.getQualifiedName());
					staticImport.enumConstants.add(constant.getName());
				}
			}
			for (ResolvedReferenceTypeDeclaration owner : withAncestors(type)) {
				for (ResolvedReferenceTypeDeclaration memberType : owner.internalTypes()) {
					if (isInterfaceOrAnnotation(owner) || isImportable(accessOf(memberType), owner)) {
						staticImport.names.get(Kind.TYPE).add(memberType.getName());
						if (owner == type) {
							staticImport.declaredMemberTypes.put(memberType.getName(), memberType.getQualifiedName());
						}
					}
				}
			}
		}

		/** Whether a member with that access, declared in that type, can be imported into this file. */
		private boolean isImportable(AccessSpecifier access, ResolvedTypeDeclaration declaringType) {
			if (access == AccessSpecifier.PUBLIC) {
				return true;
			}
			// protected and package-private members are only accessible from the same package here, and private ones never
			return access != AccessSpecifier.PRIVATE && packageOf(declaringType).equals(packageName);
		}

		/**
		 * Fails if any ancestor of the type cannot be resolved, or if the type may get members from Lombok: JavaParser would
		 * silently miss them. Also fails for a type that is on the classpath twice, e.g. in two versions of a library, since
		 * the compiler may see the other one.
		 */
		private void requireCompleteAncestry(ResolvedReferenceTypeDeclaration type, Set<String> seen) {
			if (!seen.add(type.getQualifiedName())) {
				return;
			}
			if (type.toAst(TypeDeclaration.class).filter(ReplaceStaticImportsFormatterFunc::usesLombok).isPresent()) {
				throw new IllegalStateException("Lombok generates members that JavaParser cannot see");
			}
			if (isOnClasspathTwice(type)) {
				throw new IllegalStateException(type.getQualifiedName() + " is on the classpath twice");
			}
			for (ResolvedReferenceType ancestor : type.getAncestors(false)) {
				requireCompleteAncestry(ancestor.getTypeDeclaration().orElseThrow(), seen);
			}
		}

		// ---------------------------------------------------------------- the occurrences

		/** Classifies every occurrence of every name the static imports supply. */
		private void classifyOccurrences() {
			Set<String> suppliedNames = new HashSet<>(unresolvedNames);
			for (StaticImport staticImport : imports) {
				staticImport.names.values().forEach(suppliedNames::addAll);
			}
			boolean closedWorld = imports.stream().anyMatch(i -> i.declaration.isAsterisk() && !i.keep);
			for (SimpleName name : cu.findAll(SimpleName.class)) {
				String identifier = name.getIdentifier();
				if (suppliedNames.contains(identifier)) {
					if (unresolvedNames.contains(identifier) || isFenced(name)) {
						keepSuppliersOf(identifier, Kind.values());
					} else {
						classify(name);
					}
				} else if (closedWorld) {
					checkExplained(name);
				}
			}
			for (Name name : cu.findAll(Name.class)) {
				if (name.getQualifier().isPresent()) {
					continue;
				}
				String identifier = name.getIdentifier();
				if (suppliedNames.contains(identifier)) {
					if (unresolvedNames.contains(identifier) || isFenced(name)) {
						keepSuppliersOf(identifier, Kind.values());
					} else {
						classifyLeftmostName(name);
					}
				} else if (closedWorld && name.getParentNode().orElse(null) instanceof AnnotationExpr annotation && !resolves(annotation)) {
					keepOnDemandImports();
				}
			}
		}

		private void classify(SimpleName name) {
			Node parent = name.getParentNode().orElse(null);
			String identifier = name.getIdentifier();
			if (parent instanceof MethodCallExpr call && call.getName() == name) {
				if (call.getScope().isEmpty()) {
					classifyMethodCall(call, identifier);
				} // else a member access, such as `list.max()`
			} else if (parent instanceof NameExpr nameExpr) {
				classifyExpressionName(nameExpr, identifier);
			} else if (parent instanceof ClassOrInterfaceType type && type.getName() == name) {
				if (type.getScope().isPresent()) {
					return; // a member type, such as `Map.Entry`
				}
				if (isMethodReferenceScope(type) && suppliesField(identifier)) {
					// `NAMES::add` is parsed as a type, but it may well be a field
					classifyFieldUse(type, identifier);
				} else {
					classifyTypeName(type, identifier);
				}
			} else if (!isDeclarationOrMemberAccess(parent)) {
				// a position this step does not know about: be safe
				keepSuppliersOf(identifier, Kind.values());
			}
		}

		/** True for the parent of a {@code SimpleName} that declares something, or that names a member of something else. */
		private boolean isDeclarationOrMemberAccess(Node parent) {
			return parent instanceof FieldAccessExpr
					|| parent instanceof MethodCallExpr
					|| parent instanceof ClassOrInterfaceType
					|| parent instanceof MethodDeclaration
					|| parent instanceof ConstructorDeclaration
					|| parent instanceof CompactConstructorDeclaration
					|| parent instanceof AnnotationMemberDeclaration
					|| parent instanceof VariableDeclarator
					|| parent instanceof Parameter
					|| parent instanceof ReceiverParameter
					|| parent instanceof TypeDeclaration
					|| parent instanceof EnumConstantDeclaration
					|| parent instanceof TypeParameter
					|| parent instanceof TypePatternExpr
					|| parent instanceof MemberValuePair
					|| parent instanceof LabeledStmt
					|| parent instanceof BreakStmt
					|| parent instanceof ContinueStmt;
		}

		/** The leftmost segment of a {@link Name}, as used by annotations, imports, packages, modules and {@code Outer.this}. */
		private void classifyLeftmostName(Name name) {
			Node root = name;
			Node parent = root.getParentNode().orElse(null);
			while (parent instanceof Name qualified && qualified.getQualifier().isPresent() && qualified.getQualifier().get() == root) {
				root = parent;
				parent = root.getParentNode().orElse(null);
			}
			if (parent instanceof ImportDeclaration || parent instanceof PackageDeclaration || parent instanceof ModuleDeclaration
					|| (parent != null && parent.findAncestor(ModuleDeclaration.class).isPresent())) {
				return;
			}
			if (parent instanceof AnnotationExpr || parent instanceof SuperExpr || parent instanceof ThisExpr) {
				classifyTypeName(name, name.getIdentifier()); // `@Outer.Inner`, `Listener.super.m()`, `Outer.this`
			} else {
				keepSuppliersOf(name.getIdentifier(), Kind.values());
			}
		}

		/**
		 * The closed-world check for on-demand imports: the type solver may not see every member an on-demand
		 * import supplies (version skew, Lombok), so they are only removed if every other simple name in the file
		 * is explained by something else.
		 */
		private void checkExplained(SimpleName name) {
			Node parent = name.getParentNode().orElse(null);
			boolean explained = true;
			if (parent instanceof MethodCallExpr call && call.getName() == name && call.getScope().isEmpty()) {
				explained = membership(call, Kind.METHOD, name.getIdentifier()) == Membership.MEMBER;
			} else if (parent instanceof NameExpr nameExpr) {
				explained = resolves(nameExpr);
			} else if (parent instanceof ClassOrInterfaceType type && type.getName() == name && type.getScope().isEmpty()) {
				explained = declaredTypeNames.contains(name.getIdentifier()) || resolves(type);
			}
			if (!explained) {
				keepOnDemandImports();
			}
		}

		private boolean resolves(NameExpr nameExpr) {
			try {
				nameExpr.resolve();
				return true;
			} catch (RuntimeException e) {
				// not a variable: a type used as a qualifier, or the first segment of a package name
				if (isQualifier(nameExpr) && visibleType(context(nameExpr), nameExpr.getNameAsString(), null) != Visible.NOTHING) {
					return true;
				}
				return nameExpr.getParentNode().orElse(null) instanceof FieldAccessExpr && Character.isLowerCase(nameExpr.getNameAsString().charAt(0));
			}
		}

		private boolean resolves(ClassOrInterfaceType type) {
			try {
				type.resolve();
				return true;
			} catch (RuntimeException e) {
				return false;
			}
		}

		private boolean resolves(AnnotationExpr annotation) {
			try {
				annotation.resolve();
				return true;
			} catch (RuntimeException e) {
				return false;
			}
		}

		private void keepOnDemandImports() {
			for (StaticImport staticImport : imports) {
				if (staticImport.declaration.isAsterisk()) {
					staticImport.keep = true;
				}
			}
		}

		/** {@code name(...)} without a scope (JLS 15.12.1). */
		private void classifyMethodCall(MethodCallExpr call, String name) {
			switch (membership(call, Kind.METHOD, name)) {
			case MEMBER:
				return; // a method of an enclosing class, not the import
			case UNKNOWN:
				keepSuppliersOf(name, Kind.METHOD);
				return;
			case NOT_MEMBER:
				break;
			}
			Optional<StaticImport> source = onlySupplier(name, Kind.METHOD);
			if (source.isEmpty()) {
				return;
			}
			int arguments = call.getArguments().size();
			boolean instanceOverload = source.get().instanceMethods.getOrDefault(name, List.of()).stream().anyMatch(arity -> arity.accepts(arguments));
			if (instanceOverload) {
				// once qualified as `T.name(...)`, an instance method of T could be the one picked, which does not compile
				keepSuppliersOf(name, Kind.METHOD);
				return;
			}
			uses.add(new Use(call, name, source.get()));
		}

		/** An unqualified name in an expression: a variable, a field, or a type used as a qualifier. */
		private void classifyExpressionName(NameExpr nameExpr, String name) {
			if (nameExpr.getParentNode().orElse(null) instanceof SwitchEntry entry && entry.getLabels().contains(nameExpr)) {
				switch (switchesOnEnum(entry)) {
				case MEMBER:
					return; // resolved against the enum type of the switch, not through the import (JLS 14.11.1)
				case UNKNOWN:
					keepSuppliersOf(name, Kind.values());
					return;
				case NOT_MEMBER:
					break; // a switch on another type, where the label is an ordinary name (Java 21)
				}
			}
			if (suppliesField(name)) {
				classifyFieldUse(nameExpr, name); // a variable obscures a type of the same name (JLS 6.4.2)
			} else if (imports.stream().anyMatch(i -> i.supplies(Kind.TYPE, name))) {
				if (isQualifier(nameExpr)) {
					classifyTypeName(nameExpr, name);
				} else {
					keepSuppliersOf(name, Kind.TYPE);
				}
			}
			// otherwise the name is only supplied as a method, which a name in an expression cannot refer to
		}

		/** {@link Membership#MEMBER} if the switch of this entry is on an enum type, NOT_MEMBER if it is certainly on another type. */
		private Membership switchesOnEnum(SwitchEntry entry) {
			try {
				Expression selectorExpression = ((SwitchNode) entry.getParentNode().orElseThrow()).getSelector();
				for (NameExpr name : selectorExpression.findAll(NameExpr.class)) {
					if (isOutOfScopeLoopVariable(name)) {
						return Membership.UNKNOWN; // JavaParser would give the selector the type of a variable that is not in scope
					}
				}
				ResolvedType selector = selectorExpression.calculateResolvedType();
				if (selector.isPrimitive()) {
					return Membership.NOT_MEMBER;
				}
				if (selector.isReferenceType()) {
					Optional<ResolvedReferenceTypeDeclaration> declaration = selector.asReferenceType().getTypeDeclaration();
					if (declaration.isPresent()) {
						return declaration.get().isEnum() ? Membership.MEMBER : Membership.NOT_MEMBER;
					}
				}
				return Membership.UNKNOWN; // e.g. a type variable that JavaParser could not infer
			} catch (RuntimeException e) {
				return Membership.UNKNOWN;
			}
		}

		/** JavaParser keeps the variable of a for loop in scope after the loop, which javac does not. */
		private boolean isOutOfScopeLoopVariable(NameExpr nameExpr) {
			try {
				return isOutOfScopeLoopVariable(nameExpr.resolve(), nameExpr);
			} catch (RuntimeException e) {
				return false;
			}
		}

		private boolean isOutOfScopeLoopVariable(ResolvedValueDeclaration value, Node occurrence) {
			Node declaration = value.toAst().orElse(null);
			if (declaration instanceof VariableDeclarator) {
				declaration = declaration.getParentNode().orElse(null);
			}
			Node variable = declaration;
			Node loop = variable == null ? null : variable.getParentNode().orElse(null);
			boolean loopVariable = (loop instanceof ForStmt forStmt && forStmt.getInitialization().stream().anyMatch(init -> init == variable))
					|| (loop instanceof ForEachStmt forEach && forEach.getVariable() == variable);
			return loopVariable && !occurrence.isDescendantOf(loop);
		}

		/** A use of a static field: an unqualified name in an expression, or the scope of a method reference. */
		private void classifyFieldUse(Node node, String name) {
			switch (localOrField(node, name)) {
			case MEMBER:
				return; // a local variable, a parameter or a field of an enclosing class
			case UNKNOWN:
				keepSuppliersOf(name, Kind.FIELD, Kind.TYPE);
				return;
			case NOT_MEMBER:
				onlySupplier(name, Kind.FIELD).ifPresent(source -> uses.add(new Use(node, name, source)));
				return;
			}
		}

		/** For a name in an expression, a local variable or a field of an enclosing class counts as {@link Membership#MEMBER}. */
		private Membership localOrField(Node node, String name) {
			try {
				Optional<? extends ResolvedValueDeclaration> value;
				if (node instanceof NameExpr nameExpr) {
					value = Optional.of(nameExpr.resolve());
				} else {
					SymbolReference<? extends ResolvedValueDeclaration> reference = context(node).solveSymbol(name);
					value = reference.isSolved() ? Optional.of(reference.getCorrespondingDeclaration()) : Optional.empty();
				}
				if (value.isPresent() && !value.get().isField() && !value.get().isEnumConstant()) {
					return isOutOfScopeLoopVariable(value.get(), node) ? Membership.UNKNOWN : Membership.MEMBER;
				}
			} catch (RuntimeException e) {
				// JavaParser could not resolve it, so look for a local of that name ourselves
				if (hasLocalNamed(node, name)) {
					return Membership.UNKNOWN;
				}
			}
			// JavaParser misses some pattern variables, e.g. `name` in `x instanceof T name ? name : null`
			boolean patternVariable = node.findAncestor(BodyDeclaration.class)
					.map(body -> body.findAll(TypePatternExpr.class).stream().anyMatch(pattern -> pattern.getNameAsString().equals(name)))
					.orElse(false);
			return patternVariable ? Membership.UNKNOWN : membership(node, Kind.FIELD, name);
		}

		/** True if a variable of that name is declared in the innermost enclosing class, other than as a field. */
		private boolean hasLocalNamed(Node node, String name) {
			Node scope = enclosingScopes(node).stream().findFirst().map(EnclosingScope::node).orElse(cu);
			for (VariableDeclarator variable : scope.findAll(VariableDeclarator.class)) {
				if (variable.getNameAsString().equals(name) && !(variable.getParentNode().orElse(null) instanceof FieldDeclaration)) {
					return true;
				}
			}
			for (Parameter parameter : scope.findAll(Parameter.class)) {
				if (parameter.getNameAsString().equals(name)) {
					return true;
				}
			}
			for (TypePatternExpr pattern : scope.findAll(TypePatternExpr.class)) {
				if (pattern.getNameAsString().equals(name)) {
					return true;
				}
			}
			return false;
		}

		/** An unqualified type name: a class type, the leftmost part of an annotation name, or a qualifier like {@code Entry.comparingByKey()}. */
		private void classifyTypeName(Node node, String name) {
			List<StaticImport> suppliers = imports.stream().filter(i -> i.supplies(Kind.TYPE, name)).toList();
			if (suppliers.stream().anyMatch(i -> !i.declaration.isAsterisk())) {
				// a single-static-import of a type shadows those of static-imports-on-demand (JLS 6.4.1)
				suppliers = suppliers.stream().filter(i -> !i.declaration.isAsterisk()).toList();
			}
			if (suppliers.isEmpty() || typeImports.containsKey(name)) {
				return; // not a member type of an import, or a single-type import of that name shadows them (JLS 6.4.1)
			}
			if (declaredTypeNames.contains(name)) {
				keepSuppliersOf(name, Kind.TYPE);
				return;
			}
			switch (membership(node, Kind.TYPE, name)) {
			case MEMBER:
				return;
			case UNKNOWN:
				keepSuppliersOf(name, Kind.TYPE);
				return;
			case NOT_MEMBER:
				break;
			}
			Set<String> canonicalNames = new HashSet<>();
			for (StaticImport supplier : suppliers) {
				// a member type inherited from a supertype can only be imported through the supertype (JLS 7.5.1)
				canonicalNames.add(supplier.declaredMemberTypes.getOrDefault(name, ""));
			}
			if (canonicalNames.size() != 1 || canonicalNames.contains("")) {
				keepSuppliersOf(name, Kind.TYPE);
				return;
			}
			if (suppliers.stream().allMatch(i -> i.declaration.isAsterisk())) {
				if (isSamePackageType(name)) {
					return; // a type of the same package shadows a static-import-on-demand (JLS 6.4.1)
				}
				// and so may one that the type solver cannot see, such as generated code, so the import stays
				keepSuppliersOf(name, Kind.TYPE);
				return;
			}
			String canonicalName = canonicalNames.iterator().next();
			for (StaticImport supplier : suppliers) {
				memberTypeImports.computeIfAbsent(supplier, s -> new LinkedHashSet<>()).add(canonicalName);
			}
		}

		private boolean isSamePackageType(String name) {
			try {
				return typeSolver.tryToSolveType(packageName.isEmpty() ? name : packageName + "." + name).isSolved();
			} catch (RuntimeException e) {
				return true;
			}
		}

		private boolean suppliesField(String name) {
			return imports.stream().anyMatch(i -> i.supplies(Kind.FIELD, name));
		}

		/**
		 * The one static import that supplies this name, for an occurrence that can only come from a static import.
		 * If imports of several types supply it, JavaParser cannot be trusted to pick the one the compiler picks,
		 * unless they all reach the very same declarations, as {@code Mockito.*} and {@code ArgumentMatchers.any} do:
		 * then the import of the declaring type is the one.
		 */
		private Optional<StaticImport> onlySupplier(String name, Kind kind) {
			List<StaticImport> suppliers = imports.stream().filter(i -> i.supplies(kind, name)).toList();
			List<StaticImport> single = suppliers.stream().filter(i -> !i.declaration.isAsterisk()).toList();
			if (kind == Kind.FIELD && !single.isEmpty()) {
				// a single-static-import of a field shadows those of static-imports-on-demand (JLS 6.4.1); for methods,
				// it only shadows the override-equivalent ones, so the other overloads still compete
				suppliers = single;
			}
			Set<String> types = new HashSet<>();
			suppliers.forEach(i -> types.add(i.type.getQualifiedName()));
			if (types.size() == 1) {
				// prefer an excluded import, which keeps its uses as they are, then a single static import
				List<StaticImport> candidates = suppliers;
				return candidates.stream().filter(i -> i.keep).findFirst()
						.or(() -> candidates.stream().filter(i -> !i.declaration.isAsterisk()).findFirst())
						.or(() -> Optional.of(candidates.get(0)));
			}
			Set<String> declaringTypes = new HashSet<>();
			suppliers.forEach(i -> declaringTypes.addAll(i.declaringTypes.getOrDefault(name, Set.of("?"))));
			if (declaringTypes.size() == 1) {
				String declaringType = declaringTypes.iterator().next();
				for (StaticImport supplier : suppliers) {
					if (supplier.type.getQualifiedName().equals(declaringType)) {
						return Optional.of(supplier);
					}
				}
			}
			keepSuppliersOf(name, kind);
			return Optional.empty();
		}

		private void keepSuppliersOf(String name, Kind... kinds) {
			for (StaticImport staticImport : imports) {
				for (Kind kind : kinds) {
					if (staticImport.supplies(kind, name)) {
						staticImport.keep = true;
					}
				}
			}
		}

		/** Static imports named in doc comments stay, so that {@code javadoc} can still resolve the references. */
		private void keepImportsReferencedInComments() {
			for (Comment comment : cu.getAllComments()) {
				String content = comment.getContent();
				List<Pattern> patterns = comment.isJavadocComment() || comment.isMarkdownComment() || content.startsWith("/")
						? List.of(DOC_REFERENCE, MARKDOWN_REFERENCE)
						: List.of(DOC_REFERENCE);
				for (Pattern pattern : patterns) {
					Matcher matcher = pattern.matcher(content);
					while (matcher.find()) {
						keepSuppliersOf(matcher.group(1), Kind.values());
					}
				}
			}
		}

		// ---------------------------------------------------------------- the enclosing classes

		/** Whether an enclosing class of {@code node} declares or inherits a member of that kind and name. */
		private Membership membership(Node node, Kind kind, String name) {
			boolean complete = true;
			for (EnclosingScope scope : enclosingScopes(node)) {
				Members members = members(scope.node()).get(kind);
				if (!scope.inBody()) {
					// a class's members are not in scope in its header: annotations, type parameters, extends, implements (JLS 6.3)
					if (scope.node() instanceof RecordDeclaration && (members.possible.contains(name) || !members.complete)) {
						return Membership.UNKNOWN; // a record header declares its components, so be careful there
					}
					continue;
				}
				if (members.certain.contains(name)) {
					return Membership.MEMBER;
				}
				if (members.possible.contains(name)) {
					return Membership.UNKNOWN;
				}
				complete &= members.complete;
			}
			return complete ? Membership.NOT_MEMBER : Membership.UNKNOWN;
		}

		/** The classes around a node, innermost first: type declarations, anonymous classes and enum constant bodies. */
		private List<EnclosingScope> enclosingScopes(Node node) {
			List<EnclosingScope> scopes = new ArrayList<>();
			Node child = node;
			Node parent = node.getParentNode().orElse(null);
			while (parent != null) {
				if (parent instanceof TypeDeclaration<?> declaration) {
					boolean inBody = containsSame(declaration.getMembers(), child)
							|| (declaration instanceof EnumDeclaration enumDeclaration && containsSame(enumDeclaration.getEntries(), child));
					scopes.add(new EnclosingScope(parent, inBody));
				} else if (parent instanceof ObjectCreationExpr creation && creation.getAnonymousClassBody().isPresent()) {
					if (containsSame(creation.getAnonymousClassBody().get(), child)) {
						scopes.add(new EnclosingScope(parent, true));
					}
				} else if (parent instanceof EnumConstantDeclaration constant && containsSame(constant.getClassBody(), child)) {
					scopes.add(new EnclosingScope(parent, true));
				}
				child = parent;
				parent = parent.getParentNode().orElse(null);
			}
			return scopes;
		}

		private Map<Kind, Members> members(Node scope) {
			return membersCache.computeIfAbsent(scope, this::collectScopeMembers);
		}

		private Map<Kind, Members> collectScopeMembers(Node scope) {
			Map<Kind, Members> members = new EnumMap<>(Kind.class);
			for (Kind kind : Kind.values()) {
				members.put(kind, new Members());
			}
			NodeList<BodyDeclaration<?>> body = new NodeList<>();
			try {
				List<ResolvedReferenceTypeDeclaration> supertypes = new ArrayList<>();
				if (scope instanceof TypeDeclaration<?> declaration) {
					body = declaration.getMembers();
					addImplicitMembers(declaration, members);
					if (usesLombok(declaration)) {
						throw new IllegalStateException("Lombok generates members that JavaParser cannot see");
					}
					supertypes.addAll(directSupertypes(declaration));
				} else if (scope instanceof ObjectCreationExpr creation) {
					body = creation.getAnonymousClassBody().orElseGet(NodeList::new);
					supertypes.add(creation.getType().resolve().asReferenceType().getTypeDeclaration().orElseThrow());
					OBJECT_METHODS.forEach(method -> members.get(Kind.METHOD).add(method, true));
				} else {
					// the members of the enum itself are found through the enclosing EnumDeclaration
					body = ((EnumConstantDeclaration) scope).getClassBody();
				}
				for (ResolvedReferenceTypeDeclaration supertype : supertypes) {
					requireCompleteAncestry(supertype, new HashSet<>());
					addInheritedMembers(supertype, members);
				}
			} catch (RuntimeException e) {
				members.values().forEach(m -> m.complete = false);
			}
			for (BodyDeclaration<?> declaration : body) {
				if (declaration instanceof MethodDeclaration method) {
					members.get(Kind.METHOD).add(method.getNameAsString(), true);
				} else if (declaration instanceof FieldDeclaration field) {
					field.getVariables().forEach(variable -> members.get(Kind.FIELD).add(variable.getNameAsString(), true));
				} else if (declaration instanceof TypeDeclaration<?> type) {
					members.get(Kind.TYPE).add(type.getNameAsString(), true);
				} else if (declaration instanceof AnnotationMemberDeclaration member) {
					members.get(Kind.METHOD).add(member.getNameAsString(), true);
				}
			}
			return members;
		}

		/** Members that a type has without declaring them in its body: enum constants, record components, {@code values()}... */
		private void addImplicitMembers(TypeDeclaration<?> declaration, Map<Kind, Members> members) {
			if (declaration instanceof EnumDeclaration enumDeclaration) {
				enumDeclaration.getEntries().forEach(entry -> members.get(Kind.FIELD).add(entry.getNameAsString(), true));
				members.get(Kind.METHOD).add("values", true);
				members.get(Kind.METHOD).add("valueOf", true);
			} else if (declaration instanceof RecordDeclaration record) {
				for (Parameter component : record.getParameters()) {
					members.get(Kind.FIELD).add(component.getNameAsString(), true);
					members.get(Kind.METHOD).add(component.getNameAsString(), true);
				}
			}
			boolean isInterface = declaration instanceof AnnotationDeclaration
					|| (declaration instanceof ClassOrInterfaceDeclaration classOrInterface && classOrInterface.isInterface());
			for (String method : OBJECT_METHODS) {
				// an interface only has the public methods of Object (JLS 9.2), not clone or finalize
				if (!isInterface || !(method.equals("clone") || method.equals("finalize"))) {
					members.get(Kind.METHOD).add(method, true);
				}
			}
		}

		/** The resolved direct supertypes of a type declaration, including the implicit ones. */
		private List<ResolvedReferenceTypeDeclaration> directSupertypes(TypeDeclaration<?> declaration) {
			List<ClassOrInterfaceType> types = new ArrayList<>();
			List<String> implicit = new ArrayList<>();
			if (declaration instanceof ClassOrInterfaceDeclaration classOrInterface) {
				types.addAll(classOrInterface.getExtendedTypes());
				types.addAll(classOrInterface.getImplementedTypes());
			} else if (declaration instanceof EnumDeclaration enumDeclaration) {
				types.addAll(enumDeclaration.getImplementedTypes());
				implicit.add("java.lang.Enum");
			} else if (declaration instanceof RecordDeclaration record) {
				types.addAll(record.getImplementedTypes());
				implicit.add("java.lang.Record");
			} else if (declaration instanceof AnnotationDeclaration) {
				implicit.add("java.lang.annotation.Annotation");
			}
			List<ResolvedReferenceTypeDeclaration> supertypes = new ArrayList<>();
			for (ClassOrInterfaceType type : types) {
				supertypes.add(type.resolve().asReferenceType().getTypeDeclaration().orElseThrow());
			}
			for (String name : implicit) {
				supertypes.add(typeSolver.solveType(name));
			}
			return supertypes;
		}

		/**
		 * Adds the members that a class inherits from a supertype (JLS 8.2, 8.4.8). Private members and static
		 * interface methods are not inherited; package-private ones only within their package, which depends on
		 * every class in between, so they are only possible members.
		 */
		private void addInheritedMembers(ResolvedReferenceTypeDeclaration supertype, Map<Kind, Members> members) {
			for (var method : supertype.getAllMethods()) {
				ResolvedMethodDeclaration declaration = method.getDeclaration();
				if (declaration.isStatic() && declaration.declaringType().isInterface()) {
					continue;
				}
				addInherited(members.get(Kind.METHOD), declaration.getName(), declaration.accessSpecifier(), declaration.declaringType());
			}
			// a field or member type that two ancestors declare may be hidden on the way, e.g. by a private field of a
			// class in between (JLS 8.3, 8.5), so it is only a possible member
			List<ResolvedFieldDeclaration> fields = supertype.getAllFields();
			Map<String, Integer> fieldCount = new HashMap<>();
			fields.forEach(field -> fieldCount.merge(field.getName(), 1, Integer::sum));
			for (ResolvedFieldDeclaration field : fields) {
				AccessSpecifier access = isInterfaceOrAnnotation(field.declaringType()) ? AccessSpecifier.PUBLIC : field.accessSpecifier();
				if (fieldCount.get(field.getName()) > 1) {
					members.get(Kind.FIELD).add(field.getName(), false);
				} else {
					addInherited(members.get(Kind.FIELD), field.getName(), access, field.declaringType());
				}
			}
			Map<String, Integer> typeCount = new HashMap<>();
			List<ResolvedReferenceTypeDeclaration> owners = withAncestors(supertype);
			owners.forEach(owner -> owner.internalTypes().forEach(memberType -> typeCount.merge(memberType.getName(), 1, Integer::sum)));
			for (ResolvedReferenceTypeDeclaration owner : owners) {
				for (ResolvedReferenceTypeDeclaration memberType : owner.internalTypes()) {
					AccessSpecifier access = isInterfaceOrAnnotation(owner) ? AccessSpecifier.PUBLIC : accessOf(memberType);
					if (typeCount.get(memberType.getName()) > 1) {
						members.get(Kind.TYPE).add(memberType.getName(), false);
					} else {
						addInherited(members.get(Kind.TYPE), memberType.getName(), access, owner);
					}
				}
			}
		}

		private void addInherited(Members members, String name, AccessSpecifier access, ResolvedTypeDeclaration declaringType) {
			String declaringPackage = packageOf(declaringType);
			// the JDK that runs the build may not be the one the project compiles against, and their members differ
			boolean fromJdk = jdkPackages().contains(declaringPackage) && !"java.lang.Object".equals(declaringType.getQualifiedName());
			if (access == AccessSpecifier.PUBLIC || access == AccessSpecifier.PROTECTED) {
				members.add(name, !fromJdk);
			} else if (access == AccessSpecifier.NONE && declaringPackage.equals(packageName)) {
				members.add(name, false);
			} // private members, and package-private ones of another package, are not inherited
		}

		// ---------------------------------------------------------------- the edits

		private String edit() {
			Map<StaticImport, Plan> plans = planQualifiers();
			List<StaticImport> removed = imports.stream().filter(i -> !i.keep).toList();
			if (removed.isEmpty()) {
				return text;
			}
			List<Edit> edits = new ArrayList<>();
			Set<String> newImports = new TreeSet<>();
			for (StaticImport staticImport : removed) {
				Plan plan = plans.get(staticImport);
				if (plan != null) {
					edits.addAll(plan.insertions);
					newImports.addAll(plan.imports);
				}
				for (String canonicalName : memberTypeImports.getOrDefault(staticImport, Set.of())) {
					if (!canonicalName.equals(typeImports.get(simpleName(canonicalName)))) {
						newImports.add(canonicalName);
					}
				}
			}
			addImportEdits(removed, newImports, edits);
			if (touchesFence(edits)) {
				return text; // toggleOffOn() would undo part of the edits, e.g. a new import inside a spotless:off region
			}
			return applyEdits(edits);
		}

		/** Works out how to qualify every use; a static import with a use that cannot be qualified is kept. */
		private Map<StaticImport, Plan> planQualifiers() {
			while (true) {
				plannedSimpleNames.clear();
				Map<StaticImport, Plan> plans = new IdentityHashMap<>();
				Set<Integer> qualifiedOffsets = new HashSet<>();
				StaticImport failed = null;
				for (Use use : uses) {
					StaticImport source = use.source();
					if (source.keep) {
						continue;
					}
					int offset = use.node().getBegin().map(this::offset).orElse(-1);
					if (!qualifiedOffsets.add(offset)) {
						continue; // the same occurrence, seen twice
					}
					Optional<Qualifier> qualifier = offset >= 0 && text.startsWith(use.name(), offset) ? qualifier(use) : Optional.empty();
					if (qualifier.isEmpty()) {
						failed = source;
						break;
					}
					Plan plan = plans.computeIfAbsent(source, s -> new Plan());
					plan.insertions.add(new Edit(offset, offset, qualifier.get().text() + "."));
					if (qualifier.get().importName() != null) {
						plan.imports.add(qualifier.get().importName());
					}
				}
				if (failed == null) {
					return plans;
				}
				failed.keep = true;
			}
		}

		/** Qualifies a use with the simple name of the imported type where that is safe, otherwise with its fully qualified name. */
		private Optional<Qualifier> qualifier(Use use) {
			ResolvedReferenceTypeDeclaration type = use.source().type;
			if (!isReachableThroughContainers(type)) {
				return Optional.empty();
			}
			Node node = use.node();
			Context context = context(node);
			// a nested type of this file may already be visible by its own name, e.g. inside its outer class
			if (type.containerType().isPresent() && declaredTypeQualifiedNames.contains(type.getQualifiedName())
					&& !typeParameterNames.contains(type.getName())
					&& noVariableNamed(node, context, type.getName())
					&& isOwnMemberTypeInScope(node, type)
					&& visibleType(context, type.getName(), type.getQualifiedName()) == Visible.EXPECTED) {
				return Optional.of(new Qualifier(type.getName(), null));
			}
			String className = type.getClassName();
			String simple = firstSegment(className);
			String topLevel = type.getPackageName().isEmpty() ? simple : type.getPackageName() + "." + simple;
			// an import of a java.lang type would not last: removeUnusedImports() deletes those as redundant
			boolean importable = !"java.lang".equals(type.getPackageName());
			if (noVariableNamed(node, context, simple) && noMemberTypeNamed(node, simple, topLevel)) {
				Visible visible = visibleType(context, simple, topLevel);
				// a type parameter of an enclosing class shadows the type, and JavaParser does not see that
				if (visible == Visible.EXPECTED && !typeParameterNames.contains(simple) && isUnambiguous(simple, topLevel)) {
					return Optional.of(new Qualifier(className, null));
				}
				if (importable && (visible == Visible.NOTHING || visible == Visible.SHADOWABLE || visible == Visible.EXPECTED)) {
					String planned = plannedSimpleNames.get(simple);
					if (topLevel.equals(planned)) {
						return Optional.of(new Qualifier(className, topLevel));
					}
					if (planned == null && canImport(simple)) {
						plannedSimpleNames.put(simple, topLevel);
						return Optional.of(new Qualifier(className, topLevel));
					}
				}
			}
			String fullyQualified = type.getQualifiedName();
			String first = firstSegment(fullyQualified);
			if (type.getPackageName().isEmpty() || !noVariableNamed(node, context, first) || declaredTypeNames.contains(first)
					|| membership(node, Kind.TYPE, first) != Membership.NOT_MEMBER || visibleType(context, first, null) != Visible.NOTHING) {
				return Optional.empty(); // a variable or type of that name would obscure the package (JLS 6.4.2)
			}
			return Optional.of(new Qualifier(fullyQualified, null));
		}

		/** For a nested type, {@code Outer.Inner} would mean a field of {@code Outer} named {@code Inner} if there is one (JLS 6.5.2). */
		private boolean isReachableThroughContainers(ResolvedReferenceTypeDeclaration type) {
			try {
				ResolvedReferenceTypeDeclaration inner = type;
				Optional<ResolvedReferenceTypeDeclaration> container = inner.containerType();
				while (container.isPresent()) {
					String innerName = inner.getName();
					if (container.get().getAllFields().stream().anyMatch(field -> field.getName().equals(innerName))) {
						return false;
					}
					inner = container.get();
					container = inner.containerType();
				}
				return true;
			} catch (RuntimeException e) {
				return false;
			}
		}

		/** True if no variable of that name is in scope, which would obscure a type or package of the same name (JLS 6.4.2). */
		private boolean noVariableNamed(Node node, Context context, String name) {
			return !variableNames.contains(name)
					&& !unresolvedNames.contains(name)
					&& !suppliesField(name)
					&& membership(node, Kind.FIELD, name) == Membership.NOT_MEMBER
					&& !isVisibleAsVariable(context, name);
		}

		/** True if no member type of that name is in scope at the node. */
		private boolean noMemberTypeNamed(Node node, String simple, String topLevel) {
			return membership(node, Kind.TYPE, simple) == Membership.NOT_MEMBER;
		}

		/**
		 * True if the innermost enclosing class with a member type of that name declares the nested type itself.
		 * JavaParser does not see member types inherited from superclasses, such as {@code Thread.State}.
		 */
		private boolean isOwnMemberTypeInScope(Node node, ResolvedReferenceTypeDeclaration type) {
			String name = type.getName();
			for (EnclosingScope scope : enclosingScopes(node)) {
				Members members = members(scope.node()).get(Kind.TYPE);
				if (!scope.inBody() && !(scope.node() instanceof RecordDeclaration)) {
					continue; // a class's members are not in scope in its header
				}
				if (!members.complete) {
					return false;
				}
				if (members.possible.contains(name)) {
					return scope.node() instanceof TypeDeclaration<?> declaration
							&& declaration.getFullyQualifiedName().map(outer -> (outer + "." + name).equals(type.getQualifiedName())).orElse(false);
				}
			}
			return false;
		}

		/**
		 * A type visible through {@code java.lang} or an on-demand import can only be used by its simple name if no
		 * other on-demand import supplies a type of that name, which would make it ambiguous, and no type of the same
		 * package has that name, which would shadow it (JLS 6.4.1, 7.5.2). Types the type solver cannot see count too.
		 */
		private boolean isUnambiguous(String simple, String topLevel) {
			if (typeImports.containsKey(simple) && !topLevel.equals(typeImports.get(simple))) {
				return false; // an import names another type, even if the type solver cannot resolve it
			}
			if (topLevel.equals(typeImports.get(simple)) || declaredTypeQualifiedNames.contains(topLevel)
					|| packageOfQualified(topLevel).equals(packageName)) {
				return true;
			}
			if (!isKnownPackage(packageName)) {
				return false;
			}
			Set<String> providers = new HashSet<>();
			addIfType(providers, "java.lang." + simple);
			for (ImportDeclaration declaration : cu.getImports()) {
				if (declaration.isModule()) {
					return false;
				}
				if (declaration.isAsterisk() && !declaration.isStatic()) {
					String name = declaration.getNameAsString();
					if (!isKnownPackage(name) && !typeSolver.tryToSolveType(name).isSolved()) {
						return false;
					}
					addIfType(providers, name + "." + simple);
				}
			}
			for (StaticImport staticImport : imports) {
				if (staticImport.declaration.isAsterisk() && staticImport.keep && staticImport.supplies(Kind.TYPE, simple)) {
					providers.add(staticImport.declaredMemberTypes.getOrDefault(simple, "?"));
				}
			}
			return providers.size() == 1 && providers.contains(topLevel);
		}

		private void addIfType(Set<String> providers, String name) {
			try {
				SymbolReference<ResolvedReferenceTypeDeclaration> reference = typeSolver.tryToSolveType(name);
				if (reference.isSolved()) {
					providers.add(reference.getCorrespondingDeclaration().getQualifiedName());
				}
			} catch (RuntimeException e) {
				providers.add("?" + name);
			}
		}

		/** Whether {@code import x.simple;} can be added without changing what any other name in the file refers to. */
		private boolean canImport(String simple) {
			if (typeImports.containsKey(simple) || declaredTypeNames.contains(simple) || unresolvedNames.contains(simple)) {
				return false;
			}
			for (StaticImport staticImport : imports) {
				if (!staticImport.declaration.isAsterisk() && staticImport.declaration.getName().getIdentifier().equals(simple)) {
					return false; // a single-static-import of a type with that name would clash (JLS 7.5.3)
				}
			}
			for (Set<String> canonicalNames : memberTypeImports.values()) {
				for (String canonicalName : canonicalNames) {
					if (simpleName(canonicalName).equals(simple)) {
						return false;
					}
				}
			}
			return !usedUnqualified(simple);
		}

		/** Whether the simple name is used anywhere in the file as a type or an expression name, where a new import could capture it. */
		private boolean usedUnqualified(String simple) {
			return usedUnqualifiedCache.computeIfAbsent(simple, s -> {
				for (SimpleName name : cu.findAll(SimpleName.class)) {
					if (name.getIdentifier().equals(s)) {
						Node parent = name.getParentNode().orElse(null);
						if (parent instanceof NameExpr || (parent instanceof ClassOrInterfaceType type && type.getScope().isEmpty())) {
							return true;
						}
					}
				}
				for (Name name : cu.findAll(Name.class)) {
					if (name.getQualifier().isEmpty() && name.getIdentifier().equals(s)
							&& name.findAncestor(ImportDeclaration.class).isEmpty()
							&& name.findAncestor(PackageDeclaration.class).isEmpty()) {
						return true;
					}
				}
				return false;
			});
		}

		private Context context(Node node) {
			return JavaParserFactory.getContext(node, typeSolver);
		}

		private Visible visibleType(Context context, String simple, String expected) {
			try {
				SymbolReference<ResolvedTypeDeclaration> reference = context.solveType(simple);
				if (!reference.isSolved()) {
					return Visible.NOTHING;
				}
				ResolvedTypeDeclaration declaration = reference.getCorrespondingDeclaration();
				if (declaration.getQualifiedName().equals(expected)) {
					return Visible.EXPECTED;
				}
				boolean topLevel = declaration instanceof ResolvedReferenceTypeDeclaration type && type.containerType().isEmpty();
				return topLevel ? Visible.SHADOWABLE : Visible.OTHER;
			} catch (RuntimeException e) {
				return Visible.UNKNOWN;
			}
		}

		private boolean isVisibleAsVariable(Context context, String name) {
			try {
				return context.solveSymbol(name).isSolved();
			} catch (RuntimeException e) {
				return true;
			}
		}

		private void addImportEdits(List<StaticImport> removed, Set<String> newImports, List<Edit> edits) {
			Set<ImportDeclaration> removedDeclarations = Collections.newSetFromMap(new IdentityHashMap<>());
			removed.forEach(r -> removedDeclarations.add(r.declaration));
			ImportDeclaration lastRemaining = null;
			for (ImportDeclaration declaration : cu.getImports()) {
				if (!removedDeclarations.contains(declaration)) {
					lastRemaining = declaration;
				}
			}
			StringBuilder importLines = new StringBuilder();
			for (String newImport : newImports) {
				importLines.append("import ").append(newImport).append(";\n");
			}
			List<int[]> blocks = removalBlocks(removed);
			if (!newImports.isEmpty()) {
				if (lastRemaining == null) {
					// the new imports take the place of the first removed block
					int[] first = blocks.remove(0);
					edits.add(new Edit(first[0], first[1], importLines.toString()));
				} else {
					int end = offset(lastRemaining.getEnd().orElseThrow()) + 1;
					int newline = text.indexOf('\n', end);
					String rest = text.substring(end, newline < 0 ? text.length() : newline).strip();
					if (rest.isEmpty() || rest.startsWith("//")) {
						int afterLast = lineEnd(end);
						edits.add(new Edit(afterLast, afterLast, importLines.toString()));
					} else {
						// the line goes on, e.g. with the class or a comment: the new imports go on lines of their own
						edits.add(new Edit(end, end, "\n" + importLines.toString().stripTrailing()));
					}
				}
			}
			for (int[] block : blocks) {
				edits.add(new Edit(block[0], withFollowingBlankLine(block), ""));
			}
		}

		/** The end of a deleted block, extended over the next line if the block sits between two blank lines, so that only one remains. */
		private int withFollowingBlankLine(int[] block) {
			boolean wholeLines = block[0] > 0 && text.charAt(block[0] - 1) == '\n' && text.charAt(block[1] - 1) == '\n';
			boolean blankBefore = block[0] >= 2 && text.charAt(block[0] - 2) == '\n';
			boolean blankAfter = block[1] < text.length() && text.charAt(block[1]) == '\n';
			return wholeLines && blankBefore && blankAfter ? block[1] + 1 : block[1];
		}

		/** The text ranges of the removed imports: whole lines when an import is alone on its line, with adjacent lines merged. */
		private List<int[]> removalBlocks(List<StaticImport> removed) {
			List<int[]> ranges = new ArrayList<>();
			for (StaticImport staticImport : removed) {
				int begin = offset(staticImport.declaration.getBegin().orElseThrow());
				int end = offset(staticImport.declaration.getEnd().orElseThrow()) + 1;
				int lineStart = text.lastIndexOf('\n', begin - 1) + 1;
				int newline = text.indexOf('\n', end);
				int contentEnd = newline < 0 ? text.length() : newline;
				String rest = text.substring(end, contentEnd).strip();
				// an import alone on its line goes with its line, including a trailing comment about it, such as `// NOPMD`
				if (text.substring(lineStart, begin).isBlank() && (rest.isEmpty() || rest.startsWith("//"))) {
					ranges.add(new int[]{lineStart, newline < 0 ? text.length() : newline + 1});
				} else {
					// shares its line with other code: remove it with the blanks that follow it
					int after = end;
					while (after < contentEnd && (text.charAt(after) == ' ' || text.charAt(after) == '\t')) {
						after++;
					}
					ranges.add(new int[]{begin, after});
				}
			}
			ranges.sort(Comparator.comparingInt(range -> range[0]));
			List<int[]> blocks = new ArrayList<>();
			for (int[] range : ranges) {
				int[] last = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
				if (last != null && last[1] == range[0]) {
					last[1] = range[1];
				} else {
					blocks.add(new int[]{range[0], range[1]});
				}
			}
			return blocks;
		}

		private boolean touchesFence(List<Edit> edits) {
			for (Edit edit : edits) {
				for (int[] fence : fences) {
					boolean touches = edit.start() == edit.end()
							? fence[0] < edit.start() && edit.start() < fence[1]
							: edit.start() < fence[1] && fence[0] < edit.end();
					if (touches) {
						return true;
					}
				}
			}
			return false;
		}

		/** The offset just after the end of the line that contains {@code offset}. */
		private int lineEnd(int offset) {
			int newline = text.indexOf('\n', offset);
			return newline < 0 ? text.length() : newline + 1;
		}

		private String applyEdits(List<Edit> edits) {
			edits.sort(Comparator.comparingInt(Edit::start).thenComparingInt(Edit::end));
			List<Edit> merged = new ArrayList<>();
			for (Edit edit : edits) {
				Edit last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
				if (last != null && edit.start() < last.end()) {
					throw new IllegalStateException("overlapping edits");
				}
				if (last != null && edit.start() == last.start() && last.start() == last.end()) {
					// an insertion, then a replacement that starts at the same place
					merged.set(merged.size() - 1, new Edit(edit.start(), edit.end(), last.replacement() + edit.replacement()));
				} else {
					merged.add(edit);
				}
			}
			StringBuilder result = new StringBuilder(text);
			for (int i = merged.size() - 1; i >= 0; i--) {
				Edit edit = merged.get(i);
				result.replace(edit.start(), edit.end(), edit.replacement());
			}
			return result.toString();
		}

		private int offset(Position position) {
			if (position.line < 1 || position.line >= lineOffsets.length) {
				return -1;
			}
			return lineOffsets[position.line] + position.column - 1;
		}
	}

	/** True if the type declaration, or one of its members, carries an annotation that may come from Lombok. */
	private static boolean usesLombok(TypeDeclaration<?> declaration) {
		CompilationUnit cu = declaration.findCompilationUnit().orElse(null);
		Set<String> lombokNames = new HashSet<>();
		boolean lombokOnDemand = false;
		if (cu != null) {
			for (ImportDeclaration declarationImport : cu.getImports()) {
				String name = declarationImport.getNameAsString();
				if (name.equals("lombok") || name.startsWith("lombok.")) {
					if (declarationImport.isAsterisk()) {
						lombokOnDemand = true;
					} else {
						lombokNames.add(declarationImport.getName().getIdentifier());
					}
				}
			}
		}
		for (AnnotationExpr annotation : declaration.findAll(AnnotationExpr.class)) {
			String name = annotation.getNameAsString();
			if (name.startsWith("lombok.") || lombokNames.contains(name) || (lombokOnDemand && !name.contains("."))) {
				return true;
			}
		}
		return false;
	}

	/** {@code NodeList.contains} compares nodes structurally, which is slow and could match an identical member elsewhere. */
	private static boolean containsSame(List<? extends Node> nodes, Node node) {
		for (Node element : nodes) {
			if (element == node) {
				return true;
			}
		}
		return false;
	}

	private static List<ResolvedReferenceTypeDeclaration> withAncestors(ResolvedReferenceTypeDeclaration type) {
		List<ResolvedReferenceTypeDeclaration> types = new ArrayList<>();
		types.add(type);
		for (ResolvedReferenceType ancestor : type.getAllAncestors()) {
			types.add(ancestor.getTypeDeclaration().orElseThrow());
		}
		return types;
	}

	private static boolean isInterfaceOrAnnotation(ResolvedTypeDeclaration type) {
		return type.isInterface() || type.isAnnotation();
	}

	private static AccessSpecifier accessOf(ResolvedReferenceTypeDeclaration type) {
		return type instanceof HasAccessSpecifier withAccess ? withAccess.accessSpecifier() : AccessSpecifier.NONE;
	}

	private static String packageOf(ResolvedTypeDeclaration type) {
		return type instanceof ResolvedReferenceTypeDeclaration reference ? reference.getPackageName() : "";
	}

	private static String packageOfQualified(String qualifiedName) {
		int dot = qualifiedName.lastIndexOf('.');
		return dot < 0 ? "" : qualifiedName.substring(0, dot);
	}

	private static boolean isQualifier(NameExpr nameExpr) {
		Node parent = nameExpr.getParentNode().orElse(null);
		if (parent instanceof MethodCallExpr call) {
			return call.getScope().filter(scope -> scope == nameExpr).isPresent();
		}
		if (parent instanceof FieldAccessExpr access) {
			return access.getScope() == nameExpr;
		}
		return parent instanceof MethodReferenceExpr reference && reference.getScope() == nameExpr;
	}

	/** True for the {@code X} of {@code X::method}, which JavaParser parses as a type even when it is a variable. */
	private static boolean isMethodReferenceScope(ClassOrInterfaceType type) {
		return type.getParentNode().orElse(null) instanceof TypeExpr typeExpr
				&& typeExpr.getParentNode().orElse(null) instanceof MethodReferenceExpr reference
				&& reference.getScope() == typeExpr;
	}

	private static String firstSegment(String dottedName) {
		int dot = dottedName.indexOf('.');
		return dot < 0 ? dottedName : dottedName.substring(0, dot);
	}

	private static String simpleName(String canonicalName) {
		return canonicalName.substring(canonicalName.lastIndexOf('.') + 1);
	}

	/** {@code lineOffsets[line]} is the offset of the first character of that 1-based line. */
	private static int[] lineOffsets(String text) {
		List<Integer> offsets = new ArrayList<>();
		offsets.add(0);
		offsets.add(0);
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				offsets.add(i + 1);
			}
		}
		return offsets.stream().mapToInt(Integer::intValue).toArray();
	}
}
