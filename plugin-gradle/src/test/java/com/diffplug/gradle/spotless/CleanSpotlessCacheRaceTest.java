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
package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for https://github.com/diffplug/spotless/issues/3067.
 */
class CleanSpotlessCacheRaceTest extends GradleIntegrationHarness {
	private static final Pattern LOADER_ID_PATTERN = Pattern.compile("SPOTLESS_LOADER_ID=(\\d+)");

	private File createTwoClassJar(String relativePath) throws IOException {
		File srcDir = newFile("jar-src");
		File classesDir = newFile("jar-classes");
		assertThat(srcDir.mkdirs() || srcDir.isDirectory()).isTrue();
		assertThat(classesDir.mkdirs() || classesDir.isDirectory()).isTrue();

		File pkgDir = new File(srcDir, "example");
		assertThat(pkgDir.mkdirs() || pkgDir.isDirectory()).isTrue();
		File firstJava = new File(pkgDir, "FirstClass.java");
		File secondJava = new File(pkgDir, "SecondClass.java");
		Files.writeString(firstJava.toPath(), "package example; public class FirstClass {}\n", StandardCharsets.UTF_8);
		Files.writeString(secondJava.toPath(), "package example; public class SecondClass {}\n", StandardCharsets.UTF_8);

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertThat(compiler).as("JDK compiler must be available").isNotNull();
		int rc = compiler.run(null, null, null, "-d", classesDir.getAbsolutePath(),
				firstJava.getAbsolutePath(), secondJava.getAbsolutePath());
		assertThat(rc).isZero();

		File jarFile = newFile(relativePath);
		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
			for (String classRelPath : List.of("example/FirstClass.class", "example/SecondClass.class")) {
				File classFile = new File(classesDir, classRelPath);
				jos.putNextEntry(new JarEntry(classRelPath));
				jos.write(Files.readAllBytes(classFile.toPath()));
				jos.closeEntry();
			}
		}
		return jarFile;
	}

	@Test
	void parallelCleanDoesNotCloseInUseSpotlessCacheClassLoader() throws IOException {
		File jarFile = createTwoClassJar("test-formatter.jar");
		String jarPath = jarFile.getAbsolutePath().replace("\\", "/");
		String aStartedPath = newFile("a-started.flag").getAbsolutePath().replace("\\", "/");
		String bCleanDonePath = newFile("b-clean-done.flag").getAbsolutePath().replace("\\", "/");

		setFile("settings.gradle").toLines(
				"rootProject.name = 'clean-race'",
				"include 'a', 'b'");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless' apply false",
				"}");

		setFile("a/input.txt").toContent("hello\n");
		setFile("a/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"",
				"class JarLoadingState implements Serializable {",
				"    private static final long serialVersionUID = 1L",
				"    final com.diffplug.spotless.JarState jarState",
				"    final String aStartedPath",
				"    final String bCleanDonePath",
				"    JarLoadingState(com.diffplug.spotless.JarState jarState, String aStartedPath, String bCleanDonePath) {",
				"        this.jarState = jarState",
				"        this.aStartedPath = aStartedPath",
				"        this.bCleanDonePath = bCleanDonePath",
				"    }",
				"}",
				"",
				"class JarLoadingFunc implements com.diffplug.spotless.FormatterFunc, Serializable {",
				"    private static final long serialVersionUID = 1L",
				"    final com.diffplug.spotless.JarState jarState",
				"    final String aStartedPath",
				"    final String bCleanDonePath",
				"    JarLoadingFunc(com.diffplug.spotless.JarState jarState, String aStartedPath, String bCleanDonePath) {",
				"        this.jarState = jarState",
				"        this.aStartedPath = aStartedPath",
				"        this.bCleanDonePath = bCleanDonePath",
				"    }",
				"    @Override",
				"    String apply(String input) throws Exception {",
				"        ClassLoader cl = jarState.getClassLoader()",
				"        cl.loadClass('example.FirstClass')",
				"        File started = new File(aStartedPath)",
				"        started.text = 'started'",
				"        File bCleanDone = new File(bCleanDonePath)",
				"        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15)",
				"        while (!bCleanDone.exists()) {",
				"            if (System.nanoTime() > deadline) {",
				"                throw new IllegalStateException('Timed out waiting for :b:clean to finish')",
				"            }",
				"            Thread.sleep(25)",
				"        }",
				"        Class<?> second = cl.loadClass('example.SecondClass')",
				"        if (input.contains(second.getName())) {",
				"            return input",
				"        }",
				"        return input + second.getName() + '\\n'",
				"    }",
				"}",
				"",
				"class JarLoadingToFormatter implements com.diffplug.spotless.SerializedFunction<JarLoadingState, com.diffplug.spotless.FormatterFunc> {",
				"    private static final long serialVersionUID = 1L",
				"    @Override",
				"    com.diffplug.spotless.FormatterFunc apply(JarLoadingState state) {",
				"        return new JarLoadingFunc(state.jarState, state.aStartedPath, state.bCleanDonePath)",
				"    }",
				"}",
				"",
				"def jarState = com.diffplug.spotless.JarState.preserveOrder([new File('" + jarPath + "')])",
				"def step = com.diffplug.spotless.FormatterStep.create(",
				"    'jarLoading',",
				"    new JarLoadingState(jarState, '" + aStartedPath + "', '" + bCleanDonePath + "'),",
				"    new JarLoadingToFormatter()",
				")",
				"",
				"spotless {",
				"    format('misc') {",
				"        target 'input.txt'",
				"        addStep(step)",
				"    }",
				"}");

		setFile("b/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"",
				"tasks.named('clean').configure {",
				"    doFirst {",
				"        File started = new File('" + aStartedPath + "')",
				"        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15)",
				"        while (!started.exists()) {",
				"            if (System.nanoTime() > deadline) {",
				"                throw new IllegalStateException('Timed out waiting for :a:spotlessMisc to start')",
				"            }",
				"            Thread.sleep(25)",
				"        }",
				"    }",
				"    doLast {",
				"        new File('" + bCleanDonePath + "').text = 'done'",
				"    }",
				"}");

		gradleRunner()
				.withArguments(":b:clean", ":a:spotlessApply", ":a:spotlessCheck", "--parallel", "--stacktrace")
				.build();

		assertFile("a/input.txt").hasContent("hello\nexample.SecondClass\n");
	}

	@Test
	void cleanStillClearsSpotlessCacheAtBuildEndIncludingWithConfigurationCache() throws IOException {
		File jarFile = createTwoClassJar("cache-check.jar");
		String jarPath = jarFile.getAbsolutePath().replace("\\", "/");

		setFile("settings.gradle").toLines(
				"rootProject.name = 'clean-cache-clear'",
				"include 'a', 'b'");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless' apply false",
				"}");

		setFile("a/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"",
				"tasks.register('checkLoader') {",
				"    doLast {",
				"        def cl = com.diffplug.spotless.JarState.preserveOrder([new File('" + jarPath + "')]).getClassLoader()",
				"        println('SPOTLESS_LOADER_ID=' + System.identityHashCode(cl))",
				"    }",
				"}");

		setFile("b/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}");

		try {
			// Build 1: populate SpotlessCache
			BuildResult run1 = gradleRunner().withArguments(":a:checkLoader", "--no-configuration-cache").build();
			String loader1 = extractLoaderId(run1.getOutput());

			// Build 2 (without clean): SpotlessCache keeps the same classloader
			BuildResult run2 = gradleRunner().withArguments(":a:checkLoader", "--no-configuration-cache").build();
			String loader2 = extractLoaderId(run2.getOutput());
			assertThat(loader2).isEqualTo(loader1);

			// Build 3: clean clears SpotlessCache at end of build (configuration cache miss)
			gradleRunner().withArguments("clean", "--configuration-cache").build();

			// Build 4: SpotlessCache creates a new classloader
			BuildResult run4 = gradleRunner().withArguments(":a:checkLoader", "--no-configuration-cache").build();
			String loader4 = extractLoaderId(run4.getOutput());
			assertThat(loader4).isNotEqualTo(loader1);

			// Build 5: second clean (configuration cache HIT!) must STILL clear SpotlessCache
			BuildResult cleanHit = gradleRunner().withArguments("clean", "--configuration-cache").build();
			assertThat(cleanHit.getOutput()).contains("Reusing configuration cache.");

			// Build 6: SpotlessCache creates another new classloader
			BuildResult run6 = gradleRunner().withArguments(":a:checkLoader", "--no-configuration-cache").build();
			String loader6 = extractLoaderId(run6.getOutput());
			assertThat(loader6).isNotEqualTo(loader4);
		} finally {
			// Release open JarFile handles in the TestKit daemon before JUnit cleans up @TempDir on Windows
			gradleRunner().withArguments("clean", "--no-configuration-cache").build();
		}
	}

	@Test
	void cleanAndSpotlessApplyWorkWithIsolatedProjects() throws IOException {
		setFile("gradle.properties").toContent("org.gradle.unsafe.isolated-projects=true\n");
		setFile("settings.gradle").toLines(
				"rootProject.name = 'clean-isolated'",
				"include 'a', 'b'");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless' apply false",
				"}");
		setFile("a/input.txt").toContent("  hello  \n");
		setFile("a/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    format('misc') {",
				"        target 'input.txt'",
				"        trimTrailingWhitespace()",
				"    }",
				"}");
		setFile("b/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}");

		gradleRunner().withArguments("clean", "spotlessApply").build();
		assertFile("a/input.txt").hasContent("  hello\n");
		// Second run exercises isolated-projects configuration cache reuse
		gradleRunner().withArguments("clean", "spotlessCheck").build();
	}

	private static String extractLoaderId(String output) {
		Matcher matcher = LOADER_ID_PATTERN.matcher(output);
		assertThat(matcher.find()).as("Expected SPOTLESS_LOADER_ID in output:\n" + output).isTrue();
		return matcher.group(1);
	}
}
