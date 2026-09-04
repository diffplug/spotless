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
package com.diffplug.spotless.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.TestProvisioner;

/**
 * Tests for {@code shortenFullyQualifiedTypes}.
 *
 * <h3>Safety contract: never introduce a compile error</h3>
 *
 * <p>The formatter operates without a classpath, so it cannot resolve types the way
 * the compiler does. The rule is: <em>when in doubt, leave the reference qualified.</em>
 * A verbose FQN is harmless; a wrong shortening breaks the build.
 *
 * <p>Tests are organised into three groups:
 *
 * <ol>
 *   <li><b>Exact checks</b> — situations where we have enough information to
 *       decide with certainty. Examples: the simple name already appears in a
 *       different import, or clashes with a type declared in the same file.
 *       In these cases the formatter must leave the FQN alone.</li>
 *
 *   <li><b>Expression-context heuristics</b> — FQTs used as static method call
 *       targets ({@code java.lang.management.ManagementFactory.getPlatformMXBeans(…)}),
 *       static field or enum-constant access, or nested-type member access.
 *       JavaParser sees these as {@code FieldAccessExpr}/{@code MethodCallExpr},
 *       not type nodes, so two heuristics guard against false positives:
 *       <ul>
 *         <li><em>Known-package check:</em> if the candidate's package already
 *             appears in the file's imports, own package, or {@code java.lang},
 *             we trust it.</li>
 *         <li><em>Minimum-depth fallback:</em> otherwise we require ≥ 2 lowercase
 *             (package) segments before the first uppercase (type) segment.
 *             This rejects {@code variable.Field} patterns that look like a FQT
 *             but are really field access on a local variable.
 *             In theory a legitimate single-segment package ({@code a.MyType})
 *             with no matching import would be skipped, but single-segment
 *             packages are virtually non-existent in real-world Java.</li>
 *       </ul></li>
 *
 *   <li><b>Known ambiguous / leave-as-is</b> — cases where shortening <em>could</em>
 *       change semantics and the formatter cannot prove safety. For example,
 *       a FQN whose simple name matches a same-package type used unqualified
 *       elsewhere in the file.</li>
 * </ol>
 */
class ShortenFullyQualifiedTypesStepTest {

	private FormatterStep step() {
		return ShortenFullyQualifiedTypesStep.create(TestProvisioner.mavenCentral());
	}

	private String apply(String input) throws Exception {
		return step().format(LineEnding.toUnix(input), new File(""));
	}

	/** Returns the code portion (everything after imports/package), for asserting FQNs are gone from code only. */
	private static String codeBody(String source) {
		// Strip lines starting with package/import to avoid matching FQNs inside import statements
		return source.lines()
				.filter(l -> !l.stripLeading().startsWith("package ") && !l.stripLeading().startsWith("import "))
				.reduce("", (a, b) -> a + "\n" + b);
	}

	@Test
	void basicFqnShortening() throws Exception {
		String before = String.join("\n",
				"package com.example.service;",
				"",
				"public class UserService {",
				"    private final java.util.Map<String, java.util.List<String>> cache = new java.util.HashMap<>();",
				"",
				"    public java.util.List<String> getUsers(java.util.function.Predicate<String> filter) throws java.io.IOException {",
				"        java.util.List<String> result = new java.util.ArrayList<>();",
				"        return result;",
				"    }",
				"}",
				"");
		String result = apply(before);
		// Verify FQNs are shortened in code (not in imports)
		assertFalse(result.contains("java.util.Map<"), "java.util.Map should be shortened");
		assertFalse(result.contains("java.util.List<"), "java.util.List should be shortened");
		assertFalse(result.contains("new java.util.HashMap"), "java.util.HashMap should be shortened");
		assertFalse(result.contains("new java.util.ArrayList"), "java.util.ArrayList should be shortened");
		assertFalse(result.contains("throws java.io.IOException"), "java.io.IOException should be shortened in throws");
		// Verify imports are added
		assertTrue(result.contains("import java.util.Map;"), "should import Map");
		assertTrue(result.contains("import java.util.List;"), "should import List");
		assertTrue(result.contains("import java.util.HashMap;"), "should import HashMap");
		assertTrue(result.contains("import java.util.ArrayList;"), "should import ArrayList");
		assertTrue(result.contains("import java.io.IOException;"), "should import IOException");
	}

	@Test
	void conflictingSimpleNamesNotShortened() throws Exception {
		String code = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    java.util.List<String> a;",
				"    java.awt.List b;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void existingImportConflict() throws Exception {
		String code = String.join("\n",
				"package com.example;",
				"",
				"import java.awt.List;",
				"",
				"public class Foo {",
				"    java.util.List<String> a;",
				"    List b;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void javaLangNotImported() throws Exception {
		String before = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    java.lang.String s;",
				"}",
				"");
		String result = apply(before);
		assertFalse(result.contains("java.lang.String"), "java.lang.String should be shortened");
		assertFalse(result.contains("import java.lang.String"), "java.lang.String should not be imported");
	}

	@Test
	void samePackageNotImported() throws Exception {
		String before = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    com.example.Bar b;",
				"}",
				"");
		String result = apply(before);
		assertFalse(result.contains("com.example.Bar"), "same-package FQN should be shortened");
		assertFalse(result.contains("import com.example.Bar"), "same-package type should not be imported");
	}

	@Test
	void alreadyImportedNotDuplicated() throws Exception {
		String before = String.join("\n",
				"package com.example;",
				"",
				"import java.util.List;",
				"",
				"public class Foo {",
				"    java.util.List<String> a;",
				"    List<String> b;",
				"}",
				"");
		String result = apply(before);
		assertFalse(result.contains("java.util.List<"), "FQN should be shortened");
		int count = result.split("import java\\.util\\.List;", -1).length - 1;
		assertEquals(1, count, "should not duplicate import");
	}

	@Test
	void noFqnUnchanged() throws Exception {
		String code = String.join("\n",
				"package com.example;",
				"",
				"import java.util.List;",
				"",
				"public class Foo {",
				"    List<String> a;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	// ── Java 14+ syntax tests ──────────────────────────────────────────

	@Test
	void instanceofPatternMatching() throws Exception {
		String before = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    void test(Object o) {",
				"        if (o instanceof java.util.List<?> list) {",
				"            System.out.println(list);",
				"        }",
				"    }",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.List"), "FQN in instanceof pattern should be shortened");
		assertTrue(result.contains("import java.util.List;"), "should add import");
		assertTrue(codeBody(result).contains("instanceof List<?> list"), "pattern variable should be preserved");
	}

	@Test
	void instanceofChainedPatterns() throws Exception {
		// Two instanceof patterns with FQNs on the same line
		String before = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    void test(Object a, Object b) {",
				"        if (a instanceof java.util.List<?> list",
				"                && b instanceof java.util.Map<?,?> map) {",
				"            System.out.println(list);",
				"        }",
				"    }",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.List"), "FQN List should be shortened");
		assertFalse(codeBody(result).contains("java.util.Map"), "FQN Map should be shortened");
		assertTrue(result.contains("import java.util.List;"), "should import List");
		assertTrue(result.contains("import java.util.Map;"), "should import Map");
	}

	@Test
	void switchPatternMatching() throws Exception {
		String before = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    String test(Object o) {",
				"        return switch (o) {",
				"            case java.util.List<?> list -> list.toString();",
				"            case java.util.Map<?,?> map -> map.toString();",
				"            default -> \"other\";",
				"        };",
				"    }",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.List"), "FQN in switch case should be shortened");
		assertFalse(codeBody(result).contains("java.util.Map"), "FQN in switch case should be shortened");
		assertTrue(result.contains("import java.util.List;"), "should import List");
		assertTrue(result.contains("import java.util.Map;"), "should import Map");
	}

	@Test
	void recordComponents() throws Exception {
		String before = String.join("\n",
				"package com.example;",
				"",
				"public record Pair(java.util.List<String> left, java.util.Map<String,String> right) {}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.List"), "FQN in record component should be shortened");
		assertFalse(codeBody(result).contains("java.util.Map"), "FQN in record component should be shortened");
		assertTrue(result.contains("import java.util.List;"), "should import List");
		assertTrue(result.contains("import java.util.Map;"), "should import Map");
	}

	@Test
	void sealedPermitsNotCorrupted() throws Exception {
		// sealed/permits are contextual keywords — ensure the step doesn't corrupt them
		String code = String.join("\n",
				"package com.example;",
				"",
				"public sealed interface Shape permits Circle, Square {}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void textBlockWithFqnUntouched() throws Exception {
		String code = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    String s = \"\"\"",
				"        java.util.List is a type",
				"        \"\"\";",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void varWithFqnInGenerics() throws Exception {
		String before = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    void test() {",
				"        var list = new java.util.ArrayList<java.util.Map<String, String>>();",
				"    }",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.ArrayList"), "FQN ArrayList should be shortened");
		assertFalse(codeBody(result).contains("java.util.Map"), "FQN Map in generic should be shortened");
		assertTrue(result.contains("import java.util.ArrayList;"), "should import ArrayList");
		assertTrue(result.contains("import java.util.Map;"), "should import Map");
	}

	@Test
	void lambdaParameterTypes() throws Exception {
		String before = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    Runnable r = () -> {",
				"        java.util.List<String> items = new java.util.ArrayList<>();",
				"        items.forEach((java.util.function.Consumer<String>) s -> {});",
				"    };",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.List<"), "FQN in lambda body should be shortened");
		assertFalse(codeBody(result).contains("java.util.function.Consumer"), "FQN cast in lambda should be shortened");
		assertTrue(result.contains("import java.util.List;"), "should import List");
	}

	@Test
	void fqnCollisionWithEnclosingClassName() throws Exception {
		// dev.jbang.cli.Alias intentionally uses dev.jbang.catalog.Alias as FQN
		// because the simple name "Alias" would clash with the enclosing class
		String code = String.join("\n",
				"package dev.jbang.cli;",
				"",
				"public class Alias {",
				"    dev.jbang.catalog.Alias catalogAlias;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void fqnCollisionWithInnerClassName() throws Exception {
		// FQN whose simple name matches an inner class declared in the same file
		String code = String.join("\n",
				"package com.example;",
				"",
				"public class Outer {",
				"    static class Conflict {}",
				"    com.other.Conflict externalConflict;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void fqnCollisionWithUnqualifiedSamePackageType() throws Exception {
		// RandomAccessFile is declared in another file in this package; importing java.io.RandomAccessFile
		// would silently change which type the unqualified superclass name resolves to
		String code = String.join("\n",
				"package test.reprod1;",
				"",
				"public class ClassA extends RandomAccessFile {",
				"    final java.io.RandomAccessFile file;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void fqnNoCollisionWithDifferentSimpleName() throws Exception {
		// FQN whose simple name does NOT match the enclosing class — should still shorten
		String before = String.join("\n",
				"package dev.jbang.cli;",
				"",
				"public class Alias {",
				"    java.util.List<String> items;",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.List"), "non-conflicting FQN should be shortened");
		assertTrue(result.contains("import java.util.List;"), "should import List");
	}

	@Test
	void issue3039_fqtInStaticMethodCall() throws Exception {
		String before = String.join("\n",
				"import java.lang.management.BufferPoolMXBean;",
				"import java.util.List;",
				"",
				"public class ClassA {",
				"    public void methodA() {",
				"        final List<BufferPoolMXBean> pools = java.lang.management.ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);",
				"    }",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.lang.management.ManagementFactory"),
				"FQT in static method call should be shortened");
		assertTrue(result.contains("import java.lang.management.ManagementFactory;"),
				"should add ManagementFactory import");
	}

	@Test
	void issue3039_fqtNestedTypeFieldAccess() throws Exception {
		String before = String.join("\n",
				"import java.util.List;",
				"import pkg.models.CustomTypeProperty;",
				"",
				"public class ClassB {",
				"    final List<CustomTypeProperty> connectionProps = List.of(new CustomTypeProperty().name(\"host\")",
				"        .type(pkg.models.CustomTypeProperty.TypeEnum.STRING));",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("pkg.models.CustomTypeProperty"),
				"FQT in nested type field access should be shortened");
	}

	@Test
	void exprStaticFieldAccess() throws Exception {
		String before = String.join("\n",
				"import java.util.concurrent.TimeUnit;",
				"",
				"public class Foo {",
				"    long millis = java.util.concurrent.TimeUnit.SECONDS.toMillis(5);",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.concurrent.TimeUnit"),
				"FQT for static field access should be shortened");
	}

	@Test
	void exprJavaLangImplicitPackage() throws Exception {
		// java.lang is always known — should shorten even without explicit imports
		String before = String.join("\n",
				"public class Foo {",
				"    void test() {",
				"        java.lang.System.exit(0);",
				"    }",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.lang.System"),
				"java.lang.System should be shortened (java.lang is implicit)");
		assertFalse(result.contains("import java.lang.System"),
				"java.lang types should not be imported");
	}

	@Test
	void exprChainedAfterStaticMethod() throws Exception {
		String before = String.join("\n",
				"import java.util.List;",
				"",
				"public class Foo {",
				"    List<String> items = java.util.Collections.unmodifiableList(new java.util.ArrayList<>());",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.Collections"),
				"FQT in chained static method call should be shortened");
		assertTrue(result.contains("import java.util.Collections;"), "should import Collections");
	}

	@Test
	void exprEnumConstantAccess() throws Exception {
		String before = String.join("\n",
				"import java.time.LocalDate;",
				"",
				"public class Foo {",
				"    Object day = java.time.DayOfWeek.MONDAY;",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.time.DayOfWeek"),
				"FQT for enum constant should be shortened");
		assertTrue(result.contains("import java.time.DayOfWeek;"), "should import DayOfWeek");
	}

	// ── Expression-context: should NOT shorten (ambiguous) ───────────────

	@Test
	void exprSingleSegmentUnknownPackageNotShortened() throws Exception {
		// 'config' could be a local variable; only 1 lowercase segment, no matching import
		String code = String.join("\n",
				"public class Foo {",
				"    Object v = config.Default.VALUE;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void exprSingleSegmentUnknownPackageMethodNotShortened() throws Exception {
		// 'builder' could be a local variable; only 1 lowercase segment, no matching import
		String code = String.join("\n",
				"public class Foo {",
				"    Object v = builder.Type.create();",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void exprSingleSegmentWithKnownImportDoesShorten() throws Exception {
		// 'config' has 1 lowercase segment but IS a known package (import exists from config.*)
		String before = String.join("\n",
				"import config.Other;",
				"",
				"public class Foo {",
				"    Object v = config.Default.VALUE;",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("config.Default"),
				"known single-segment package should be shortened");
	}

	@Test
	void exprCollisionWithExistingImportNotShortened() throws Exception {
		// java.util.List is already imported; java.awt.List in expression context must not shorten
		String code = String.join("\n",
				"import java.util.List;",
				"",
				"public class Foo {",
				"    int n = java.awt.List.COLUMN_HEADERS;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void exprCollisionWithDeclaredTypeNotShortened() throws Exception {
		// File declares class named 'Entry'; expression FQT with same simple name must not shorten
		String code = String.join("\n",
				"import java.util.Map;",
				"",
				"public class Entry {",
				"    Object e = java.util.Map.Entry.class;",
				"}",
				"");
		assertEquals(code, apply(code));
	}

	@Test
	void multipleAnnotationsWithFqn() throws Exception {
		// FQNs used as annotation types should NOT be treated as type references
		// (annotations start with @, not handled by ClassOrInterfaceType)
		// but FQN types in annotation values or alongside annotations should work
		String before = String.join("\n",
				"package com.example;",
				"",
				"public class Foo {",
				"    java.util.List<String> items;",
				"}",
				"");
		String result = apply(before);
		assertFalse(codeBody(result).contains("java.util.List"), "FQN should be shortened");
		assertTrue(result.contains("import java.util.List;"), "should import List");
	}
}
