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
package com.diffplug.spotless.npm;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.diffplug.spotless.ResourceHarness;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NodeServerLayoutTest extends ResourceHarness {

	@Test
	void itCalculatesSameNodeModulesDirForSameContent() throws IOException {
		File testDir = newFolder("build");
		String packageJsonContent = prettierPackageJson(emptyMap());
		String serveJsContent = "fun main() { console.log('Hello, world!'); }";
		NodeServerLayout layout1 = new NodeServerLayout(testDir, packageJsonContent, serveJsContent);
		NodeServerLayout layout2 = new NodeServerLayout(testDir, packageJsonContent, serveJsContent);

		assertThat(layout1.nodeModulesDir()).isEqualTo(layout2.nodeModulesDir());
	}

	@Test
	void itCalculatesDifferentNodeModulesDirForDifferentPackageJson() throws IOException {
		File testDir = newFolder("build");
		String packageJsonContent1 = prettierPackageJson(Map.of("prettier-plugin-xy", "^2.0.0"));
		String packageJsonContent2 = prettierPackageJson(Map.of("prettier-plugin-xy", "^2.1.0"));
		String serveJsContent = "fun main() { console.log('Hello, world!'); }";

		NodeServerLayout layout1 = new NodeServerLayout(testDir, packageJsonContent1, serveJsContent);
		NodeServerLayout layout2 = new NodeServerLayout(testDir, packageJsonContent2, serveJsContent);

		assertThat(layout1.nodeModulesDir()).isNotEqualTo(layout2.nodeModulesDir());
	}

	@Test
	void itCalculatesDifferentNodeModulesDirForDifferentServeJs() throws IOException {
		File testDir = newFolder("build");
		String packageJsonContent = prettierPackageJson(emptyMap());
		String serveJsContent1 = "fun main() { console.log('Hello, world!'); }";
		String serveJsContent2 = "fun main() { console.log('Goodbye, world!'); }";

		NodeServerLayout layout1 = new NodeServerLayout(testDir, packageJsonContent, serveJsContent1);
		NodeServerLayout layout2 = new NodeServerLayout(testDir, packageJsonContent, serveJsContent2);

		assertThat(layout1.nodeModulesDir()).isNotEqualTo(layout2.nodeModulesDir());
	}

	static String prettierPackageJson(Map<String, String> dependencies) {
		String templateContent = NpmResourceHelper.readUtf8StringFromClasspath(NodeServerLayoutTest.class, "/com/diffplug/spotless/npm/prettier-package.json");
		String dependenciesList = dependencies.entrySet().stream()
				.map(entry -> "\"%s\": \"%s\"".formatted(entry.getKey(), entry.getValue()))
				.reduce((a, b) -> a + ",\n  " + b)
				.orElse("");

		return templateContent.replace("${devDependencies}", dependenciesList);
	}
}
