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
package com.diffplug.spotless.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

/**
 * Tests for {@code replaceStaticImports}. The step must never produce code that does not compile or
 * behaves differently, so most tests are about the cases where a static import has to stay, or where
 * a name must not be qualified. {@link StepHarness#test} checks that the result is idempotent, and
 * {@link #assertCompiles} compiles inputs and outputs with the JDK's own compiler.
 */
class ReplaceStaticImportsStepTest extends ResourceHarness {
	private static final Pattern PACKAGE_DECLARATION = Pattern.compile("(?m)^package\\s+([\\w.]+)\\s*;");
	private static final Pattern TYPE_DECLARATION = Pattern.compile("(?m)^\\s*(public\\s+)?(?:(?:abstract|final|sealed|non-sealed|static)\\s+)*(?:class|interface|enum|record|@interface)\\s+(\\w+)");
	private int compilations;

	/** A step whose classpath is the {@code src} directory of the test folder, plus the JDK. */
	private FormatterStep step(String... excludes) {
		return ReplaceStaticImportsStep.create(Set.of(newFile("src")), List.of(excludes), TestProvisioner.mavenCentral());
	}

	/** Writes a source file that the type solver, and the compiler, can see. */
	private void source(String path, String content) throws IOException {
		setFile("src/" + path).toContent(content);
	}

	/**
	 * In a real project, the file being formatted is in a source directory on the classpath, which tells the step
	 * which types its package has. The tests format strings, so they also write them there.
	 */
	private void inASourceDirectory(String source) throws IOException {
		Matcher packageDeclaration = PACKAGE_DECLARATION.matcher(source);
		String directory = packageDeclaration.find() ? packageDeclaration.group(1).replace('.', '/') + "/" : "";
		setFile("src/" + directory + typeName(source) + ".java").toContent(source);
	}

	/** Checks the step's output, that it is idempotent, and that both the input and the output compile. */
	private void assertReplaced(String before, String after) throws IOException {
		inASourceDirectory(before);
		StepHarness.forStep(step()).test(before, after);
		assertCompiles(before);
		assertCompiles(after);
	}

	private void assertReplacedResource(String before, String after) throws IOException {
		assertReplaced(getTestResource(before), getTestResource(after));
	}

	/** Checks that the step leaves a file that compiles alone. */
	private void assertUnchanged(String source) throws IOException {
		inASourceDirectory(source);
		StepHarness.forStep(step()).testUnaffected(source);
		assertCompiles(source);
	}

	/** For files that cannot compile here, because they refer to types that are deliberately missing. */
	private void assertUnchangedWithoutCompiling(String source) throws IOException {
		inASourceDirectory(source);
		StepHarness.forStep(step()).testUnaffected(source);
	}

	/** The name of the public type of the source, or else of its first type, which names its file. */
	private static String typeName(String source) {
		String typeName = "A";
		boolean found = false;
		Matcher declaration = TYPE_DECLARATION.matcher(source);
		while (declaration.find()) {
			if (!found || declaration.group(1) != null) {
				typeName = declaration.group(2);
				found = true;
				if (declaration.group(1) != null) {
					break;
				}
			}
		}
		return typeName;
	}

	/** A jar holding the given sources, compiled by the JDK's compiler. */
	private File jar(String name, String path, String source) throws IOException {
		File sources = newFile("jar-sources/" + name);
		File sourceFile = new File(sources, path);
		Files.createDirectories(sourceFile.getParentFile().toPath());
		Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));
		File classes = newFile("jar-classes/" + name);
		Files.createDirectories(classes.toPath());
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager files = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
			assertThat(compiler.getTask(null, files, null, List.of("-d", classes.getPath()), null, files.getJavaFileObjects(sourceFile)).call()).isTrue();
		}
		File jar = newFile("libs/" + name + ".jar");
		Files.createDirectories(jar.getParentFile().toPath());
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar.toPath()))) {
			try (Stream<Path> classFiles = Files.walk(classes.toPath())) {
				for (Path classFile : (Iterable<Path>) classFiles.filter(Files::isRegularFile)::iterator) {
					zip.putNextEntry(new ZipEntry(classes.toPath().relativize(classFile).toString().replace(File.separatorChar, '/')));
					zip.write(Files.readAllBytes(classFile));
					zip.closeEntry();
				}
			}
		}
		return jar;
	}

	/** Compiles the source with the JDK's compiler against the {@code src} folder, to prove that it is valid Java. */
	private void assertCompiles(String source) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertThat(compiler).as("these tests need a JDK").isNotNull();
		File dir = newFile("compile/" + compilations++);
		File file = new File(dir, typeName(source) + ".java");
		File classes = new File(dir, "classes");
		Files.createDirectories(classes.toPath());
		Files.createDirectories(newFile("src").toPath());
		Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
			// javac-only holds types that the compiler sees but the type solver does not, such as generated code
			String sourcepath = newFile("src").getPath() + File.pathSeparator + newFile("javac-only").getPath();
			List<String> options = List.of("-d", classes.getPath(), "-sourcepath", sourcepath, "-proc:none", "-Xlint:none");
			boolean compiled = compiler.getTask(null, files, diagnostics, options, null, files.getJavaFileObjects(file)).call();
			assertThat(compiled).as("does not compile: %s%n%s", diagnostics.getDiagnostics(), source).isTrue();
		}
	}

	@Test
	void enumConstantFromTheIssue() throws IOException {
		source("org/enums/Value.java", """
				package org.enums;

				public enum Value {
				    SOME_ENUM_VALUE, OTHER_VALUE
				}
				""");
		assertReplaced("""
				package com.example;

				import static org.enums.Value.SOME_ENUM_VALUE;

				public class Test {
				    public void method() {
				        var example = SOME_ENUM_VALUE;
				    }
				}
				""", """
				package com.example;

				import org.enums.Value;

				public class Test {
				    public void method() {
				        var example = Value.SOME_ENUM_VALUE;
				    }
				}
				""");
	}

	@Test
	void staticMethodGetsQualifiedAndItsTypeImported() throws IOException {
		assertReplaced("""
				package p;

				import java.util.List;

				import static java.util.Objects.requireNonNull;

				class A {
				    List<String> f(List<String> list) {
				        return requireNonNull(list).isEmpty() ? null : requireNonNull(list);
				    }
				}
				""", """
				package p;

				import java.util.List;
				import java.util.Objects;

				class A {
				    List<String> f(List<String> list) {
				        return Objects.requireNonNull(list).isEmpty() ? null : Objects.requireNonNull(list);
				    }
				}
				""");
	}

	@Test
	void onDemandImportFromJavaLangNeedsNoNewImport() throws IOException {
		// in a resource file, because the build forbids wildcard imports even inside text blocks
		assertReplacedResource("java/replacestaticimports/OnDemandJavaLangUnformatted.test", "java/replacestaticimports/OnDemandJavaLangFormatted.test");
	}

	@Test
	void enumCaseLabelsStayUnqualified() throws IOException {
		// a qualified enum constant is not a valid case label before Java 21
		assertReplaced("""
				package p;

				import static java.util.concurrent.TimeUnit.SECONDS;

				import java.util.concurrent.TimeUnit;

				class A {
				\tlong f(TimeUnit unit) {
				\t\tswitch (unit) {
				\t\tcase SECONDS:
				\t\t\treturn SECONDS.toMillis(1);
				\t\tdefault:
				\t\t\treturn 0;
				\t\t}
				\t}

				\tString g(TimeUnit unit) {
				\t\treturn switch (unit) {
				\t\tcase SECONDS -> "s";
				\t\tdefault -> "?";
				\t\t};
				\t}
				}
				""", """
				package p;

				import java.util.concurrent.TimeUnit;

				class A {
				\tlong f(TimeUnit unit) {
				\t\tswitch (unit) {
				\t\tcase SECONDS:
				\t\t\treturn TimeUnit.SECONDS.toMillis(1);
				\t\tdefault:
				\t\t\treturn 0;
				\t\t}
				\t}

				\tString g(TimeUnit unit) {
				\t\treturn switch (unit) {
				\t\tcase SECONDS -> "s";
				\t\tdefault -> "?";
				\t\t};
				\t}
				}
				""");
	}

	@Test
	void constantCaseLabelGetsQualified() throws IOException {
		source("q/Codes.java", """
				package q;

				public final class Codes {
				    public static final int OK = 0;
				}
				""");
		assertReplaced("""
				package p;

				import static q.Codes.OK;

				class A {
				    String f(int code) {
				        switch (code) {
				        case OK:
				            return "ok";
				        default:
				            return "?";
				        }
				    }
				}
				""", """
				package p;

				import q.Codes;

				class A {
				    String f(int code) {
				        switch (code) {
				        case Codes.OK:
				            return "ok";
				        default:
				            return "?";
				        }
				    }
				}
				""");
	}

	@Test
	void localVariableHidesTheImport() throws IOException {
		assertReplaced("""
				package p;

				import static java.lang.Math.PI;

				class A {
				    double f() {
				        double PI = 3;
				        return PI;
				    }

				    double g() {
				        return PI;
				    }
				}
				""", """
				package p;

				class A {
				    double f() {
				        double PI = 3;
				        return PI;
				    }

				    double g() {
				        return Math.PI;
				    }
				}
				""");
	}

	@Test
	void variableNamedLikeAnImportedMethodIsNotAUse() throws IOException {
		assertReplaced("""
				package p;

				import static java.lang.Math.max;

				class A {
				    int f(int a, int b) {
				        int max = max(a, b);
				        return max;
				    }
				}
				""", """
				package p;

				class A {
				    int f(int a, int b) {
				        int max = Math.max(a, b);
				        return max;
				    }
				}
				""");
	}

	@Test
	void methodOfTheClassHidesTheImport() throws IOException {
		// JLS 15.12.1: a method of an enclosing class hides a static import of the same name, so the import is unused
		assertReplaced("""
				package p;

				import static java.util.Objects.isNull;

				class A {
				    boolean isNull(Object o) {
				        return o == null;
				    }

				    boolean f(Object o) {
				        return isNull(o);
				    }
				}
				""", """
				package p;

				class A {
				    boolean isNull(Object o) {
				        return o == null;
				    }

				    boolean f(Object o) {
				        return isNull(o);
				    }
				}
				""");
	}

	@Test
	void inheritedMethodHidesTheImport() throws IOException {
		source("q/Base.java", """
				package q;

				public class Base {
				    protected static void check(boolean condition) {}
				}
				""");
		source("q/Checks.java", """
				package q;

				public final class Checks {
				    public static void check(boolean condition) {}
				}
				""");
		assertReplaced("""
				package p;

				import static q.Checks.check;

				import q.Base;

				class A extends Base {
				    void f() {
				        check(true);
				    }
				}
				""", """
				package p;

				import q.Base;

				class A extends Base {
				    void f() {
				        check(true);
				    }
				}
				""");
	}

	@Test
	void unresolvableSupertypeKeepsTheImport() throws IOException {
		// the missing supertype could declare a requireNonNull method that hides the import
		assertUnchangedWithoutCompiling("""
				package p;

				import static java.util.Objects.requireNonNull;

				class A extends Missing {
				    Object f(Object o) {
				        return requireNonNull(o);
				    }
				}
				""");
	}

	@Test
	void unresolvableImportIsLeftAlone() throws IOException {
		assertUnchangedWithoutCompiling("""
				package p;

				import static org.junit.Assert.assertTrue;

				class A {
				    void f() {
				        assertTrue(true);
				    }
				}
				""");
	}

	@Test
	void overloadsFromTwoImportedTypesAreLeftAlone() throws IOException {
		// valueOf("1") is Integer.valueOf(String); picking the overload is left to the compiler
		assertUnchanged("""
				package p;

				import static java.lang.Integer.valueOf;
				import static java.lang.String.valueOf;

				class A {
				    Object f() {
				        return valueOf("1");
				    }
				}
				""");
	}

	@Test
	void secondTypeWithTheSameSimpleNameIsFullyQualified() throws IOException {
		assertReplaced("""
				package p;

				import static java.sql.Date.valueOf;
				import static java.util.Date.from;
				import java.time.Instant;

				class A {
				    long f(Instant instant) {
				        return valueOf("2020-01-01").getTime() + from(instant).getTime();
				    }
				}
				""", """
				package p;

				import java.time.Instant;
				import java.sql.Date;

				class A {
				    long f(Instant instant) {
				        return Date.valueOf("2020-01-01").getTime() + java.util.Date.from(instant).getTime();
				    }
				}
				""");
	}

	@Test
	void typeDeclaredInTheFileForcesTheFullyQualifiedName() throws IOException {
		assertReplaced("""
				package p;

				import static java.util.Objects.requireNonNull;

				class A {
				    Object f(Object o) {
				        return requireNonNull(o);
				    }
				}

				class Objects {}
				""", """
				package p;

				class A {
				    Object f(Object o) {
				        return java.util.Objects.requireNonNull(o);
				    }
				}

				class Objects {}
				""");
	}

	@Test
	void unusedTypeOfTheSamePackageIsShadowedByTheNewImport() throws IOException {
		source("p/Objects.java", """
				package p;

				class Objects {}
				""");
		assertReplaced("""
				package p;

				import static java.util.Objects.requireNonNull;

				class A {
				    Object f(Object o) {
				        return requireNonNull(o);
				    }
				}
				""", """
				package p;

				import java.util.Objects;

				class A {
				    Object f(Object o) {
				        return Objects.requireNonNull(o);
				    }
				}
				""");
	}

	@Test
	void usedTypeOfTheSamePackageForcesTheFullyQualifiedName() throws IOException {
		source("p/Objects.java", """
				package p;

				class Objects {}
				""");
		assertReplaced("""
				package p;

				import static java.util.Objects.requireNonNull;

				class A {
				    Object f(Object o) {
				        return requireNonNull(new Objects());
				    }
				}
				""", """
				package p;

				class A {
				    Object f(Object o) {
				        return java.util.Objects.requireNonNull(new Objects());
				    }
				}
				""");
	}

	@Test
	void staticImportOfANestedTypeBecomesARegularImport() throws IOException {
		assertReplaced("""
				package p;

				import static java.util.Map.Entry;

				import java.util.Map;

				class A {
				    Entry<String, String> first(Map<String, String> map) {
				        return map.entrySet().iterator().next();
				    }
				}
				""", """
				package p;

				import java.util.Map;
				import java.util.Map.Entry;

				class A {
				    Entry<String, String> first(Map<String, String> map) {
				        return map.entrySet().iterator().next();
				    }
				}
				""");
	}

	@Test
	void memberOfANestedTypeIsQualifiedThroughItsOuterType() throws IOException {
		assertReplaced("""
				package p;

				import static java.util.Map.Entry.comparingByKey;

				import java.util.Comparator;
				import java.util.Map;

				class A {
				    Comparator<Map.Entry<String, Integer>> byKey() {
				        return comparingByKey();
				    }
				}
				""", """
				package p;

				import java.util.Comparator;
				import java.util.Map;

				class A {
				    Comparator<Map.Entry<String, Integer>> byKey() {
				        return Map.Entry.comparingByKey();
				    }
				}
				""");
	}

	@Test
	void interfaceConstant() throws IOException {
		source("q/Limits.java", """
				package q;

				public interface Limits {
				    int MAX = 5;
				}
				""");
		assertReplaced("""
				package p;

				import static q.Limits.MAX;

				class A {
				    int f() {
				        return MAX;
				    }
				}
				""", """
				package p;

				import q.Limits;

				class A {
				    int f() {
				        return Limits.MAX;
				    }
				}
				""");
	}

	@Test
	void constantInAnAnnotation() throws IOException {
		source("q/Names.java", """
				package q;

				public final class Names {
				    public static final String UNCHECKED = "unchecked";
				}
				""");
		assertReplaced("""
				package p;

				import static q.Names.UNCHECKED;

				class A {
				    @SuppressWarnings(UNCHECKED)
				    void f() {}
				}
				""", """
				package p;

				import q.Names;

				class A {
				    @SuppressWarnings(Names.UNCHECKED)
				    void f() {}
				}
				""");
	}

	@Test
	void usesInLambdasAndAnonymousClasses() throws IOException {
		assertReplaced("""
				package p;

				import static java.util.Objects.requireNonNull;

				import java.util.function.Function;

				class A {
				    Function<Object, Object> f = o -> requireNonNull(o);
				    Runnable r = new Runnable() {
				        @Override
				        public void run() {
				            requireNonNull(f);
				        }
				    };
				}
				""", """
				package p;

				import java.util.function.Function;
				import java.util.Objects;

				class A {
				    Function<Object, Object> f = o -> Objects.requireNonNull(o);
				    Runnable r = new Runnable() {
				        @Override
				        public void run() {
				            Objects.requireNonNull(f);
				        }
				    };
				}
				""");
	}

	@Test
	void singleAndOnDemandImportOfTheSameType() throws IOException {
		assertReplacedResource("java/replacestaticimports/SingleAndOnDemandUnformatted.test", "java/replacestaticimports/SingleAndOnDemandFormatted.test");
	}

	@Test
	void unusedStaticImportIsRemoved() throws IOException {
		assertReplaced("""
				package p;

				import static java.util.Objects.requireNonNull;

				class A {}
				""", """
				package p;

				class A {}
				""");
	}

	@Test
	void filesWithoutStaticImportsOrThatDoNotParseAreUnchanged() throws IOException {
		assertUnchanged("""
				package p;

				import java.util.Objects;

				class A {
				    Object f(Object o) {
				        return Objects.requireNonNull(o);
				    }
				}
				""");
		assertUnchangedWithoutCompiling("""
				package p;

				import static java.util.Objects.requireNonNull;

				class A {
				    Object f(Object o) {
				        return requireNonNull(o)
				""");
	}

	@Test
	void nullClasspathIsRejected() {
		assertThrows(NullPointerException.class, () -> ReplaceStaticImportsStep.create(null, TestProvisioner.mavenCentral()));
	}

	@Test
	void enumLabelOfASwitchOnAnotherTypeIsAUse() throws IOException {
		// Java 21 allows enum constants as labels of a switch on any type; there, the label is an ordinary name
		String before = """
				package p;

				import static java.util.concurrent.TimeUnit.SECONDS;

				class A {
				    String f(Object o) {
				        return switch (o) {
				        case SECONDS -> "s";
				        default -> "?";
				        };
				    }
				}
				""";
		String after = """
				package p;

				import java.util.concurrent.TimeUnit;

				class A {
				    String f(Object o) {
				        return switch (o) {
				        case TimeUnit.SECONDS -> "s";
				        default -> "?";
				        };
				    }
				}
				""";
		inASourceDirectory(before);
		StepHarness.forStep(step()).test(before, after);
		if (Runtime.version().feature() >= 21) {
			assertCompiles(before);
			assertCompiles(after);
		}
	}

	@Test
	void staticInterfaceMethodsAreNotInherited() throws IOException {
		// JLS 8.4.8: Comparator.comparing is not a member of a class implementing Comparator, so it comes from the import
		assertReplaced("""
				package p;

				import static java.util.Comparator.comparing;

				import java.util.Comparator;

				class ByLength implements Comparator<String> {
				    public int compare(String a, String b) {
				        return Integer.compare(a.length(), b.length());
				    }

				    Comparator<String> natural() {
				        return comparing((String s) -> s);
				    }
				}
				""", """
				package p;

				import java.util.Comparator;

				class ByLength implements Comparator<String> {
				    public int compare(String a, String b) {
				        return Integer.compare(a.length(), b.length());
				    }

				    Comparator<String> natural() {
				        return Comparator.comparing((String s) -> s);
				    }
				}
				""");
	}

	@Test
	void privateMethodOfTheSuperclassDoesNotHideTheImport() throws IOException {
		source("q/PrivateBase.java", """
				package q;

				public class PrivateBase {
				    private static void check(boolean condition) {}
				}
				""");
		source("q/Checks.java", """
				package q;

				public final class Checks {
				    public static void check(boolean condition) {}
				}
				""");
		assertReplaced("""
				package p;

				import static q.Checks.check;

				import q.PrivateBase;

				class A extends PrivateBase {
				    void f() {
				        check(true);
				    }
				}
				""", """
				package p;

				import q.PrivateBase;
				import q.Checks;

				class A extends PrivateBase {
				    void f() {
				        Checks.check(true);
				    }
				}
				""");
	}

	@Test
	void inheritedMemberTypeWithTheSameNameForcesTheFullyQualifiedName() throws IOException {
		source("q/BaseWithObjects.java", """
				package q;

				public class BaseWithObjects {
				    public static class Objects {}
				}
				""");
		assertReplaced("""
				package p;

				import static java.util.Objects.requireNonNull;

				import q.BaseWithObjects;

				class A extends BaseWithObjects {
				    Object f(Object o) {
				        return requireNonNull(o);
				    }
				}
				""", """
				package p;

				import q.BaseWithObjects;

				class A extends BaseWithObjects {
				    Object f(Object o) {
				        return java.util.Objects.requireNonNull(o);
				    }
				}
				""");
	}

	@Test
	void unresolvableAncestorOfASupertypeKeepsTheImport() throws IOException {
		// JavaParser silently skips supertypes it cannot resolve, which could declare a requireNonNull method
		source("q/Middle.java", """
				package q;

				public class Middle extends Missing {}
				""");
		assertUnchangedWithoutCompiling("""
				package p;

				import static java.util.Objects.requireNonNull;

				import q.Middle;

				class A extends Middle {
				    Object f(Object o) {
				        return requireNonNull(o);
				    }
				}
				""");
	}

	@Test
	void instanceOverloadOfTheImportedTypeKeepsTheImport() throws IOException {
		// Mixed.foo("x") would pick the instance method foo(String), which does not compile in a static context
		source("q/Mixed.java", """
				package q;

				public class Mixed {
				    public static String foo(Object o) {
				        return "static";
				    }

				    public String foo(String s) {
				        return "instance";
				    }
				}
				""");
		assertUnchanged("""
				package p;

				import static q.Mixed.foo;

				class A {
				    String f() {
				        return foo("x");
				    }
				}
				""");
	}

	@Test
	void simpleNameThatTwoOnDemandImportsSupplyGetsItsOwnImport() throws IOException {
		// List is both java.util.List and java.awt.List: a single-type import makes List.of unambiguous
		assertReplacedResource("java/replacestaticimports/AmbiguousOnDemandUnformatted.test", "java/replacestaticimports/AmbiguousOnDemandFormatted.test");
	}

	@Test
	void fieldUsedAsTheScopeOfAMethodReference() throws IOException {
		source("q/Holder.java", """
				package q;

				import java.util.ArrayList;
				import java.util.List;

				public final class Holder {
				    public static final List<String> NAMES = new ArrayList<>();
				}
				""");
		assertReplaced("""
				package p;

				import static q.Holder.NAMES;

				import java.util.function.Consumer;

				class A {
				    Consumer<String> adder = NAMES::add;
				}
				""", """
				package p;

				import java.util.function.Consumer;
				import q.Holder;

				class A {
				    Consumer<String> adder = Holder.NAMES::add;
				}
				""");
	}

	@Test
	void referenceInADocCommentKeepsTheImport() throws IOException {
		assertUnchanged("""
				package p;

				import static java.util.concurrent.TimeUnit.SECONDS;

				/** Gives up after a few {@link SECONDS}. */
				class A {}
				""");
	}

	@Test
	void useInsideSpotlessOffKeepsTheImport() throws IOException {
		// toggleOffOn() would restore the unqualified use, while the import would stay deleted
		assertUnchanged("""
				package p;

				import static java.util.Objects.requireNonNull;

				class A {
				    // spotless:off
				    Object f(Object o) { return requireNonNull(o); }
				    // spotless:on
				}
				""");
	}

	@Test
	void unresolvableSingleImportOfTheSameNameKeepsTheOthers() throws IOException {
		assertUnchangedWithoutCompiling(getTestResource("java/replacestaticimports/UnresolvableSingleImportOfTheSameName.test"));
	}

	@Test
	void unresolvableOnDemandImportLeavesTheFileAlone() throws IOException {
		// it could supply any name in the file
		assertUnchangedWithoutCompiling(getTestResource("java/replacestaticimports/UnresolvableOnDemandImport.test"));
	}

	@Test
	void nestedTypeOfThisFileIsQualifiedWithItsOwnName() throws IOException {
		String before = """
				package p;

				import static p.A.Color.RED;

				class A {
				    enum Color {
				        RED, GREEN
				    }

				    Color color = RED;
				}
				""";
		// the type solver sees the file through the source directory, as it does in a real project
		source("p/A.java", before);
		assertReplaced(before, """
				package p;

				class A {
				    enum Color {
				        RED, GREEN
				    }

				    Color color = Color.RED;
				}
				""");
	}

	@Test
	void memberTypeInheritedByTheImportedTypeKeepsTheImport() throws IOException {
		// `import q.SubHolder.Kind;` would not compile: an import needs the canonical name, q.SuperHolder.Kind
		source("q/SuperHolder.java", """
				package q;

				public class SuperHolder {
				    public static class Kind {}
				}
				""");
		source("q/SubHolder.java", """
				package q;

				public class SubHolder extends SuperHolder {}
				""");
		assertUnchanged("""
				package p;

				import static q.SubHolder.Kind;

				class A {
				    Kind kind;
				}
				""");
	}

	@Test
	void fieldNamedLikeANestedTypeKeepsTheImport() throws IOException {
		// JLS 6.5.2: in Outer.Inner.m(), Outer.Inner would be the field, not the nested class
		source("q/Outer.java", """
				package q;

				public class Outer {
				    public static final int Inner = 0;

				    public static class Inner {
				        public static String m() {
				            return "m";
				        }
				    }
				}
				""");
		assertUnchanged("""
				package p;

				import static q.Outer.Inner.m;

				class A {
				    String s = m();
				}
				""");
	}

	@Test
	void classWithLombokAnnotationsKeepsTheImport() throws IOException {
		// Lombok adds members that JavaParser cannot see, and that could hide the import
		assertUnchangedWithoutCompiling("""
				package p;

				import static java.util.Objects.requireNonNull;

				import lombok.Data;

				@Data
				class A {
				    Object f(Object o) {
				        return requireNonNull(o);
				    }
				}
				""");
	}

	@Test
	void excludedImportsAreKept() throws IOException {
		String before = """
				package p;

				import static java.lang.Math.max;
				import static java.util.Objects.requireNonNull;

				class A {
				    int f(Object o) {
				        requireNonNull(o);
				        return max(1, 2);
				    }
				}
				""";
		String after = """
				package p;

				import static java.util.Objects.requireNonNull;

				class A {
				    int f(Object o) {
				        requireNonNull(o);
				        return Math.max(1, 2);
				    }
				}
				""";
		inASourceDirectory(before);
		StepHarness.forStep(step("java.util.Objects.*")).test(before, after);
		StepHarness.forStep(step("java.util.Objects.requireNonNull")).test(before, after);
		StepHarness.forStep(step("java.util.Objects")).test(before, after);
		StepHarness.forStep(step("java.util.Objects.*", "java.lang.Math.*")).testUnaffected(before);
		assertCompiles(after);
	}

	@Test
	void equality() throws IOException {
		File jar = newFile("libs/a.jar");
		Files.createDirectories(jar.getParentFile().toPath());
		Files.write(jar.toPath(), new byte[]{1, 2, 3});
		new SerializableEqualityTester() {
			Set<File> classpath = Set.of();
			List<String> excludes = List.of();

			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();
				excludes = List.of("java.util.Objects.*");
				api.areDifferentThan();
				excludes = List.of("java.util.Objects.*", "java.lang.Math.max");
				api.areDifferentThan();
				classpath = Set.of(jar);
				api.areDifferentThan();
			}

			@Override
			protected Serializable create() {
				return ReplaceStaticImportsStep.create(classpath, excludes, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}

	@Test
	void equalityDependsOnTheContentOfTheJarsNotOnWhereTheyAre() throws IOException {
		File here = newFile("here/lib.jar");
		File there = newFile("there/lib.jar");
		File other = newFile("other/lib.jar");
		for (File jar : List.of(here, there, other)) {
			Files.createDirectories(jar.getParentFile().toPath());
			Files.write(jar.toPath(), new byte[]{jar == other ? (byte) 2 : (byte) 1});
		}
		assertEquals(ReplaceStaticImportsStep.create(Set.of(here), List.of("b.B.*", "a.A.*"), TestProvisioner.mavenCentral()),
				ReplaceStaticImportsStep.create(Set.of(there), List.of("a.A.*", "b.B.*"), TestProvisioner.mavenCentral()));
		assertNotEquals(ReplaceStaticImportsStep.create(Set.of(here), TestProvisioner.mavenCentral()),
				ReplaceStaticImportsStep.create(Set.of(other), TestProvisioner.mavenCentral()));
	}

	@Test
	void sameDeclarationReachedThroughTwoImportsIsQualifiedWithItsDeclaringType() throws IOException {
		// like Mockito, which inherits any() from ArgumentMatchers: both imports reach the same method
		source("q/Matchers.java", """
				package q;

				public class Matchers {
				    public static <T> T any() {
				        return null;
				    }
				}
				""");
		source("q/Mocks.java", """
				package q;

				public class Mocks extends Matchers {
				    public static <T> T when(T call) {
				        return call;
				    }
				}
				""");
		assertReplacedResource("java/replacestaticimports/SameDeclarationThroughTwoImportsUnformatted.test", "java/replacestaticimports/SameDeclarationThroughTwoImportsFormatted.test");
	}

	@Test
	void trailingCommentGoesWithTheImport() throws IOException {
		assertReplaced("""
				package p;

				import static java.util.Objects.requireNonNull; // NOPMD

				class A {
				    Object f(Object o) {
				        return requireNonNull(o);
				    }
				}
				""", """
				package p;

				import java.util.Objects;

				class A {
				    Object f(Object o) {
				        return Objects.requireNonNull(o);
				    }
				}
				""");
	}

	@Test
	void javaLangTypeThatAnInvisibleOnDemandImportCouldShadowIsFullyQualified() throws IOException {
		// javac sees x.Math, the type solver does not: `Math.max` would be ambiguous, and an import of
		// java.lang.Math would not last, because removeUnusedImports() deletes java.lang imports
		setFile("javac-only/x/Math.java").toContent("package x;\n\npublic class Math {}\n");
		assertReplacedResource("java/replacestaticimports/JavaLangWithInvisibleOnDemandCompetitorUnformatted.test", "java/replacestaticimports/JavaLangWithInvisibleOnDemandCompetitorFormatted.test");
	}

	@Test
	void classHeaderIsOutsideTheScopeOfTheClassMembers() throws IOException {
		// JLS 6.3: NAME in the annotation of Impl is the import, while NAME in its body is the field Impl inherits
		source("p/Base.java", """
				package p;

				public class Base {
				    public static final String NAME = "base";
				}
				""");
		source("p/Other.java", """
				package p;

				public final class Other {
				    public static final String NAME = "unchecked";
				}
				""");
		assertReplaced("""
				package q;

				import static p.Other.NAME;

				import p.Base;

				@SuppressWarnings(NAME)
				class Impl extends Base {
				    String name = NAME;
				}
				""", """
				package q;

				import p.Base;
				import p.Other;

				@SuppressWarnings(Other.NAME)
				class Impl extends Base {
				    String name = NAME;
				}
				""");
	}

	@Test
	void singleImportOfAFieldShadowsAnOnDemandOne() throws IOException {
		source("p/A.java", """
				package p;

				public class A {
				    public static final String X = "A.X";

				    public static String a() {
				        return "a";
				    }
				}
				""");
		source("p/B.java", """
				package p;

				public class B {
				    public static final String X = "B.X";
				}
				""");
		assertReplacedResource("java/replacestaticimports/SingleImportOfAFieldShadowsOnDemandUnformatted.test", "java/replacestaticimports/SingleImportOfAFieldShadowsOnDemandFormatted.test");
	}

	@Test
	void typeNamedBeforeSuperIsAType() throws IOException {
		source("p/Registry.java", """
				package p;

				public final class Registry {
				    public interface Listener {
				        default int id() {
				            return 1;
				        }
				    }
				}
				""");
		assertReplaced("""
				package q;

				import static p.Registry.Listener;

				class Impl implements Listener {
				    public int id() {
				        return Listener.super.id() + 1;
				    }
				}
				""", """
				package q;

				import p.Registry.Listener;

				class Impl implements Listener {
				    public int id() {
				        return Listener.super.id() + 1;
				    }
				}
				""");
	}

	@Test
	void excludedMemberKeepsTheOnDemandImportThatSuppliesIt() throws IOException {
		String source = getTestResource("java/replacestaticimports/ExcludedMemberUsedThroughOnDemandImport.test");
		inASourceDirectory(source);
		StepHarness.forStep(step("java.util.concurrent.TimeUnit.SECONDS")).testUnaffected(source);
	}

	@Test
	void nameSpelledWithAUnicodeEscapeKeepsTheImport() throws IOException {
		// javac reads max as max, so the import is still in use
		assertUnchanged("""
				package p;

				import static java.lang.Math.max;

				class A {
				    int a = \\u006dax(1, 2);
				}
				""");
	}

	@Test
	void newImportStartsALineWhenTheLastImportSharesItsLine() throws IOException {
		assertReplaced("""
				package p; import java.util.List; import static java.util.concurrent.TimeUnit.SECONDS; class A { Object f(List<String> l) { return SECONDS; } }
				""", """
				package p; import java.util.List;
				import java.util.concurrent.TimeUnit; class A { Object f(List<String> l) { return TimeUnit.SECONDS; } }
				""");
	}

	@Test
	void editInsideASpotlessOffRegionLeavesTheFileAlone() throws IOException {
		// the new import would go inside the region, and toggleOffOn() would throw it away
		assertUnchanged("""
				package p;

				import static java.util.concurrent.TimeUnit.SECONDS;
				import java.util.List; // spotless:off
				// spotless:on

				class A {
				    Object f(List<String> l) {
				        return SECONDS;
				    }
				}
				""");
	}

	@Test
	void moduleInfoIsLeftAlone() throws IOException {
		StepHarness.forStep(step()).testUnaffected("""
				import static java.util.Map.Entry;

				module m {
				    uses Entry;
				}
				""");
	}

	@Test
	void patternVariableInAConditionalHidesTheImport() throws IOException {
		// JavaParser does not see `out` in the then-operand, but javac does
		assertUnchanged("""
				package p;

				import static java.lang.System.out;

				import java.io.PrintStream;

				class A {
				    PrintStream target(Object sink) {
				        return sink instanceof PrintStream out ? out : null;
				    }
				}
				""");
	}

	@Test
	void memberTypeInheritedFromASuperclassHidesTheNestedTypeOfThisFile() throws IOException {
		// inside Worker, State is Thread.State, so the constant is qualified through Job
		assertReplaced("""
				package p;

				import static p.Job.State.DONE;

				public class Job {
				    enum State {
				        WAITING, DONE
				    }

				    volatile State state;

				    class Worker extends Thread {
				        public void run() {
				            state = DONE;
				        }
				    }
				}
				""", """
				package p;

				public class Job {
				    enum State {
				        WAITING, DONE
				    }

				    volatile State state;

				    class Worker extends Thread {
				        public void run() {
				            state = Job.State.DONE;
				        }
				    }
				}
				""");
	}

	@Test
	void unresolvableImportOfTheSameSimpleNameForcesTheFullyQualifiedName() throws IOException {
		// javac sees com.acme.forum.Thread, the type solver does not
		setFile("javac-only/com/acme/forum/Thread.java").toContent("package com.acme.forum;\n\npublic class Thread {}\n");
		assertReplaced("""
				package p;

				import static java.lang.Thread.sleep;

				import com.acme.forum.Thread;

				class A {
				    void f(Thread thread) throws InterruptedException {
				        sleep(10);
				    }
				}
				""", """
				package p;

				import com.acme.forum.Thread;

				class A {
				    void f(Thread thread) throws InterruptedException {
				        java.lang.Thread.sleep(10);
				    }
				}
				""");
	}

	@Test
	void loopVariableIsNotInScopeAfterTheLoop() throws IOException {
		// the last PI is the import, though JavaParser resolves it to the loop variable
		assertUnchanged("""
				package p;

				import static java.lang.Math.PI;

				class A {
				    double f(double[] xs) {
				        double s = 0;
				        for (double PI : xs) {
				            s += PI;
				        }
				        return s + PI;
				    }
				}
				""");
	}

	@Test
	void cloneIsNotAMemberOfAnInterface() throws IOException {
		source("q/Copies.java", """
				package q;

				public final class Copies {
				    public static <T> T clone(T value) {
				        return value;
				    }
				}
				""");
		assertReplaced("""
				package p;

				import static q.Copies.clone;

				public interface A {
				    default String copy(String s) {
				        return clone(s);
				    }
				}
				""", """
				package p;

				import q.Copies;

				public interface A {
				    default String copy(String s) {
				        return Copies.clone(s);
				    }
				}
				""");
	}

	@Test
	void typeParameterShadowsTheSimpleName() throws IOException {
		assertReplaced("""
				package p;

				import static java.lang.Math.max;

				public class A<Math> {
				    int f(int a, int b) {
				        return max(a, b);
				    }
				}
				""", """
				package p;

				public class A<Math> {
				    int f(int a, int b) {
				        return java.lang.Math.max(a, b);
				    }
				}
				""");
	}

	@Test
	void fieldHiddenByAPrivateFieldOnTheWayKeepsTheImport() throws IOException {
		// Mid's private NAME hides Base.NAME, so A inherits neither and NAME is the import
		source("q/Base.java", "package q;\n\npublic class Base {\n    public static final String NAME = \"base\";\n}\n");
		source("q/Mid.java", "package q;\n\npublic class Mid extends Base {\n    private static final String NAME = \"mid\";\n}\n");
		source("q/Names.java", "package q;\n\npublic final class Names {\n    public static final String NAME = \"names\";\n}\n");
		assertUnchanged("""
				package p;

				import static q.Names.NAME;

				import q.Mid;

				class A extends Mid {
				    String name() {
				        return NAME;
				    }
				}
				""");
	}

	@Test
	void stackOverflowInJavaParserLeavesTheFileAlone() throws IOException {
		assertUnchanged("""
				package p;

				import static p.A.Color;
				import static p.A.Color.RED;

				public class A {
				    enum Color {
				        RED, GREEN
				    }
				}

				class B {
				    Object f() {
				        return RED;
				    }
				}
				""");
	}

	@Test
	void enumSwitchWhoseSelectorJavaParserCannotTypeKeepsTheImport() throws IOException {
		// a qualified label would only compile from Java 21 on
		assertUnchanged("""
				package p;

				import static java.util.concurrent.TimeUnit.SECONDS;

				import java.util.Collections;
				import java.util.List;
				import java.util.concurrent.TimeUnit;

				class A {
				    int f(List<TimeUnit> units) {
				        switch (Collections.max(units)) {
				        case SECONDS:
				            return 1;
				        default:
				            return 0;
				        }
				    }
				}
				""");
	}

	@Test
	void classInTwoJarsKeepsTheImport() throws IOException {
		// only version 2 of lib.Base has describe(Object), which hides the import: the compiler may see either version
		File one = jar("lib-1", "lib/Base.java", "package lib;\n\npublic class Base {}\n");
		File two = jar("lib-2", "lib/Base.java", "package lib;\n\npublic class Base {\n    public String describe(Object o) {\n        return \"base\";\n    }\n}\n");
		source("q/Util.java", """
				package q;

				public final class Util {
				    public static String describe(Object o) {
				        return "util";
				    }
				}
				""");
		String source = """
				package p;

				import static q.Util.describe;

				import lib.Base;

				class A extends Base {
				    String run() {
				        return describe(this);
				    }
				}
				""";
		inASourceDirectory(source);
		StepHarness.forStep(ReplaceStaticImportsStep.create(Set.of(newFile("src"), one, two), TestProvisioner.mavenCentral())).testUnaffected(source);
	}
}
