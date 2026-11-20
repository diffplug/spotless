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
package com.diffplug.spotless.maven.java;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ReplaceStaticImportsStepTest extends MavenIntegrationHarness {

	@Test
	void replaceStaticImports() throws Exception {
		writePomWithJavaSteps("<replaceStaticImports><excludes><exclude>java.lang.Math.*</exclude></excludes></replaceStaticImports>");
		// only resolvable through the project's source roots
		setFile("src/main/java/q/Codes.java").toContent("""
				package q;

				public final class Codes {
				    public static final int OK = 0;
				}
				""");
		String path = "src/main/java/p/A.java";
		setFile(path).toContent("""
				package p;

				import static java.lang.Math.max;
				import static java.util.Objects.requireNonNull;
				import static q.Codes.OK;

				class A {
				    int f(Object o) {
				        requireNonNull(o);
				        return max(OK, 1);
				    }
				}
				""");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(path).hasContent("""
				package p;

				import static java.lang.Math.max;
				import java.util.Objects;
				import q.Codes;

				class A {
				    int f(Object o) {
				        Objects.requireNonNull(o);
				        return max(Codes.OK, 1);
				    }
				}
				""");
	}
}
