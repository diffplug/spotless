/*
 * Copyright 2025 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class RemoveWildcardImportsStepTest extends MavenIntegrationHarness {

	@Test
	void testRemoveWildcardImports() throws Exception {
		writePomWithJavaSteps("<removeWildcardImports/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertThat(assertThrows(AssertionError.class, () -> assertFile(path).sameAsResource("java/removewildcardimports/JavaCodeWildcardsFormatted.test")).getMessage())
				.contains("Extra content at line 1:")
				.contains("Extra content at line 3:")
				.contains("import io.quarkus.maven.dependency.*;")
				.contains("import static io.quarkus.vertx.web.Route.HttpMethod.*;")
				.contains("import static org.springframework.web.reactive.function.BodyInserters.*;")
				.contains("java.util.*")
				.contains("java.util.Collections.*")
				.doesNotContain("import mylib.Helper;")
				.doesNotContain("public class Test {}");
	}

	@Test
	void testRemoveWildcardImportsWithNoResult() throws Exception {
		writePomWithJavaSteps("<removeWildcardImports/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/removewildcardimports/JavaCodeNoWildcardsUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/removewildcardimports/JavaCodeNoWildcardsFormatted.test");
	}
}
