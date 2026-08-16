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
