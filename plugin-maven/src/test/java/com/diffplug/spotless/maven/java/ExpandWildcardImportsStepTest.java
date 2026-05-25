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
package com.diffplug.spotless.maven.java;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.diffplug.common.io.Resources;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ExpandWildcardImportsStepTest extends MavenIntegrationHarness {

	@Test
	void expandWildcardImports() throws Exception {
		writePomWithJavaSteps("<expandWildcardImports/>");

		// Create supporting classes in source roots so JavaParserTypeSolver can resolve them
		setFile("src/main/java/foo/bar/AnotherClassInSamePackage.java")
			.toResource("java/expandwildcardimports/AnotherClassInSamePackage.test");
		setFile("src/main/java/foo/bar/baz/AnotherImportedClass.java")
			.toResource("java/expandwildcardimports/AnotherImportedClass.test");
		// Source for the annotation used in the test (resolves via source root, not JAR)
		setFile("src/main/java/org/example/SomeAnnotation.java")
			.toContent("""
						package org.example;

						public @interface SomeAnnotation {}
						""");

		String path = "src/main/java/foo/bar/JavaClassWithWildcards.java";
		setFile(path).toResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(path).sameAsResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");
	}

	/**
	 * Baseline: tests that types from directly declared JAR dependencies
	 * are correctly resolved for wildcard import expansion.
	 */
	@Test
	void expandWildcardImports_withDirectJarDependency() throws Exception {
		setupLocalMavenRepo();

		String projectDeps = """
				<dependency>
				  <groupId>com.example</groupId>
				  <artifactId>test-annotation-lib</artifactId>
				  <version>1.0.0</version>
				</dependency>
				""";

		writePomWithProjectDeps("<java><expandWildcardImports/></java>", projectDeps);

		setFile("src/main/java/foo/bar/AnotherClassInSamePackage.java")
			.toResource("java/expandwildcardimports/AnotherClassInSamePackage.test");
		setFile("src/main/java/foo/bar/baz/AnotherImportedClass.java")
			.toResource("java/expandwildcardimports/AnotherImportedClass.test");

		String path = "src/main/java/foo/bar/JavaClassWithWildcards.java";
		setFile(path).toResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(path).sameAsResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");
	}

	/**
	 * Tests that types from JARs transitively brought in by a POM-type dependency
	 * are correctly resolved for wildcard import expansion.
	 */
	@Test
	void expandWildcardImports_withPomTypeDependency() throws Exception {
		setupLocalMavenRepo();

		String projectDeps = """
				<dependency>
				  <groupId>com.example</groupId>
				  <artifactId>test-dependencies</artifactId>
				  <version>1.0.0</version>
				  <type>pom</type>
				</dependency>
				""";

		writePomWithProjectDeps("<java><expandWildcardImports/></java>", projectDeps);

		// Create supporting classes in source roots
		setFile("src/main/java/foo/bar/AnotherClassInSamePackage.java")
			.toResource("java/expandwildcardimports/AnotherClassInSamePackage.test");
		setFile("src/main/java/foo/bar/baz/AnotherImportedClass.java")
			.toResource("java/expandwildcardimports/AnotherImportedClass.test");
		// SomeAnnotation comes from the JAR (transitively via POM dependency), not from source

		String path = "src/main/java/foo/bar/JavaClassWithWildcards.java";
		setFile(path).toResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(path).sameAsResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");
	}

	/**
	 * Tests that wildcard imports are expanded for types from JARs transitively
	 * brought in by a POM-type dependency (e.g. an aggregator POM that lists
	 * multiple library dependencies). Uses types from two separate transitive
	 * JARs: one providing an annotation, another providing model classes.
	 */
	@Test
	void expandWildcardImports_withPomDependencyTransitiveJars() throws Exception {
		setupLocalMavenRepo();

		String projectDeps = """
				<dependency>
				  <groupId>com.example</groupId>
				  <artifactId>test-dependencies</artifactId>
				  <version>1.0.0</version>
				  <type>pom</type>
				</dependency>
				""";

		writePomWithProjectDeps("<java><expandWildcardImports/></java>", projectDeps);

		// No source-root classes for org.example.model — those types come
		// exclusively from the transitive JARs resolved via the POM dependency.
		String path = "src/main/java/foo/bar/PomDepClass.java";
		setFile(path).toResource("java/expandwildcardimports/PomDepWildcardImportsUnformatted.test");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(path).sameAsResource("java/expandwildcardimports/PomDepWildcardImportsFormatted.test");
	}

	/**
	 * Tests that both direct JAR dependencies and POM-type dependencies work together.
	 * The SomeAnnotation type is available both directly (via test-annotation-lib JAR)
	 * and transitively (via test-dependencies POM).
	 */
	@Test
	void expandWildcardImports_withMixedPomAndJarDependencies() throws Exception {
		setupLocalMavenRepo();

		String projectDeps = """
				<dependency>
				  <groupId>com.example</groupId>
				  <artifactId>test-dependencies</artifactId>
				  <version>1.0.0</version>
				  <type>pom</type>
				</dependency>
				<dependency>
				  <groupId>com.example</groupId>
				  <artifactId>test-annotation-lib</artifactId>
				  <version>1.0.0</version>
				</dependency>
				""";

		writePomWithProjectDeps("<java><expandWildcardImports/></java>", projectDeps);

		setFile("src/main/java/foo/bar/AnotherClassInSamePackage.java")
			.toResource("java/expandwildcardimports/AnotherClassInSamePackage.test");
		setFile("src/main/java/foo/bar/baz/AnotherImportedClass.java")
			.toResource("java/expandwildcardimports/AnotherImportedClass.test");

		String path = "src/main/java/foo/bar/JavaClassWithWildcards.java";
		setFile(path).toResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");

		mavenRunner().withArguments("spotless:apply").runNoError();

		assertFile(path).sameAsResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");
	}

	private void setupLocalMavenRepo() throws Exception {
		// Write a minimal POM so that mvnw can start for install:install-file
		setFile("pom.xml").toContent("""
				<project xmlns="http://maven.apache.org/POM/4.0.0">
				  <modelVersion>4.0.0</modelVersion>
				  <groupId>tmp</groupId>
				  <artifactId>tmp</artifactId>
				  <version>1.0</version>
				</project>
				""");

		// Copy JARs using binary I/O (setFile().toResource() uses text I/O which corrupts binary files)
		copyBinaryResource("java/expandwildcardimports/example-lib.jar", "test-annotation-lib.jar");
		copyBinaryResource("java/expandwildcardimports/example-model.jar", "test-model-lib.jar");

		String aggregatorPomContent = """
				<?xml version="1.0" encoding="UTF-8"?>
				<project xmlns="http://maven.apache.org/POM/4.0.0"
				         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
				  <modelVersion>4.0.0</modelVersion>
				  <groupId>com.example</groupId>
				  <artifactId>test-dependencies</artifactId>
				  <version>1.0.0</version>
				  <packaging>pom</packaging>
				  <dependencies>
				    <dependency>
				      <groupId>com.example</groupId>
				      <artifactId>test-annotation-lib</artifactId>
				      <version>1.0.0</version>
				    </dependency>
				    <dependency>
				      <groupId>com.example</groupId>
				      <artifactId>test-model-lib</artifactId>
				      <version>1.0.0</version>
				    </dependency>
				  </dependencies>
				</project>
				""";
		setFile("test-dependencies-pom.xml").toContent(aggregatorPomContent);

		// Install artifacts into the local Maven repo so transitive resolution works
		mavenRunner().withArguments(
			"install:install-file",
			"-Dfile=test-annotation-lib.jar",
			"-DgroupId=com.example",
			"-DartifactId=test-annotation-lib",
			"-Dversion=1.0.0",
			"-Dpackaging=jar").runNoError();

		mavenRunner().withArguments(
			"install:install-file",
			"-Dfile=test-model-lib.jar",
			"-DgroupId=com.example",
			"-DartifactId=test-model-lib",
			"-Dversion=1.0.0",
			"-Dpackaging=jar").runNoError();

		mavenRunner().withArguments(
			"install:install-file",
			"-Dfile=test-dependencies-pom.xml",
			"-DgroupId=com.example",
			"-DartifactId=test-dependencies",
			"-Dversion=1.0.0",
			"-Dpackaging=pom").runNoError();
	}

	private void writePomWithProjectDeps(String spotlessConfig, String projectDependencies) throws Exception {
		String spotlessVersion = System.getProperty("spotlessMavenPluginVersion");
		String pomContent = """
				<project xmlns="http://maven.apache.org/POM/4.0.0"
				         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
				  <modelVersion>4.0.0</modelVersion>
				  <groupId>com.diffplug.spotless</groupId>
				  <artifactId>spotless-maven-plugin-tests</artifactId>
				  <version>1.0.0-SNAPSHOT</version>
				  <prerequisites>
				    <maven>3.1.0</maven>
				  </prerequisites>
				  <properties>
				    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
				    <maven.compiler.source>1.8</maven.compiler.source>
				    <maven.compiler.target>1.8</maven.compiler.target>
				  </properties>
				  <dependencies>
				    %s
				  </dependencies>
				  <build>
				    <plugins>
				      <plugin>
				        <groupId>com.diffplug.spotless</groupId>
				        <artifactId>spotless-maven-plugin</artifactId>
				        <version>%s</version>
				        <configuration>
				          %s
				        </configuration>
				      </plugin>
				    </plugins>
				  </build>
				</project>
				""".formatted(projectDependencies, spotlessVersion, spotlessConfig);
		setFile("pom.xml").toContent(pomContent);
	}

	private void copyBinaryResource(String resourcePath, String targetName) throws Exception {
		Path target = newFile(targetName).toPath();
		Files.createDirectories(target.getParent());
		Files.write(target, Resources.toByteArray(ResourceHarness.class.getResource("/" + resourcePath)));
	}
}
