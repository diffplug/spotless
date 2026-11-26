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
package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.spotless.java.GoogleJavaFormatStep;

/**
 * Integration tests for the spotlessPredeclare feature, which allows dependencies
 * to be predeclared in the root project and reused across all subprojects to avoid
 * OutOfMemoryErrors when multiple projects resolve the same dependencies concurrently.
 */
class SpotlessPredeclareIntegrationTest extends GradleIntegrationHarness {
	private static final String GRADLE_VERSION = "9.5.1";

	@Nested
	class MavenDependencies {
		@Test
		void predeclareSucceedsWithMavenDependencies() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    java { googleJavaFormat() }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub'");
			setFile("sub/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        googleJavaFormat()
					    }
					}
					""");
			setFile("sub/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("sub/test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
		}

		@Test
		void predeclareFailsWhenDependencyNotPredeclared() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    // Empty - no dependencies predeclared
					}
					""");
			setFile("settings.gradle").toContent("include 'sub'");
			setFile("sub/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        googleJavaFormat()
					    }
					}
					""");
			setFile("sub/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
			assertThat(result.getOutput())
					.contains("Add a step with [com.google.googlejavaformat:google-java-format:" + GoogleJavaFormatStep.defaultVersion() + "]")
					.contains("into the `spotlessPredeclare` block in the root project");
		}

		@Test
		void predeclareWorksWithMultipleVersions() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    java { googleJavaFormat() }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub1', 'sub2'");
			setFile("sub1/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        googleJavaFormat()
					    }
					}
					""");
			setFile("sub1/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
			setFile("sub2/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        googleJavaFormat()
					    }
					}
					""");
			setFile("sub2/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("sub1/test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
			assertFile("sub2/test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
		}
	}

	@Nested
	class P2Dependencies {
		@Test
		void predeclareSucceedsWithEclipseFormatter() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    java { eclipse() }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub'");
			setFile("sub/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        eclipse()
					    }
					}
					""");
			setFile("sub/test.java").toResource("java/eclipse/JavaCodeUnformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			// Verify the file was formatted (has proper indentation now)
			String formatted = read("sub/test.java");
			assertThat(formatted).contains("main(String[] args)");
			assertThat(formatted).as("Should be indented").doesNotStartWith("public static void main");
		}

		@Test
		void predeclareFailsWhenEclipseFormatterNotPredeclared() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    // Empty - no P2 dependencies predeclared
					}
					""");
			setFile("settings.gradle").toContent("include 'sub'");
			setFile("sub/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        eclipse()
					    }
					}
					""");
			setFile("sub/test.java").toResource("java/eclipse/JavaCodeUnformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
			// Eclipse JDT resolves from an embedded lockfile rather than a P2 query, so an
			// un-predeclared subproject fails on the Maven coordinates instead of on P2.
			// greclipse still has no lockfile, so it keeps the P2 message - see
			// GroovyDependencies.predeclareFailsWhenGroovyGrEclipseNotPredeclared.
			assertThat(result.getOutput())
					.contains("into the `spotlessPredeclare` block in the root project")
					.contains("org.eclipse.jdt:org.eclipse.jdt.core");
		}

		@Test
		void predeclareWorksWithEclipseConfigFile() throws IOException {
			setFile("eclipse-formatter.xml").toResource("java/eclipse/formatter.xml");
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    java { eclipse().configFile('eclipse-formatter.xml') }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub'");
			setFile("sub/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        eclipse().configFile(rootProject.file('eclipse-formatter.xml'))
					    }
					}
					""");
			setFile("sub/test.java").toResource("java/eclipse/JavaCodeUnformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
		}

		@Test
		void predeclareWorksWithMultipleEclipseProjects() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    java { eclipse() }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub1', 'sub2', 'sub3'");
			for (int i = 1; i <= 3; i++) {
				setFile("sub" + i + "/build.gradle").toContent("""
						plugins {
						    id 'com.diffplug.spotless'
						}
						repositories { mavenCentral() }
						spotless {
						    java {
						        target file('test.java')
						        eclipse()
						    }
						}
						""");
				setFile("sub" + i + "/test.java").toResource("java/eclipse/JavaCodeUnformatted.test");
			}

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			// Verify all files were formatted
			for (int i = 1; i <= 3; i++) {
				String formatted = read("sub" + i + "/test.java");
				assertThat(formatted).contains("main(String[] args)");
				assertThat(formatted).as("Should be indented").doesNotStartWith("public static void main");
			}
		}
	}

	@Nested
	class GroovyDependencies {
		@ParameterizedTest(name = "configurationCache={0}")
		@ValueSource(booleans = {false, true})
		void predeclareSucceedsWithGroovyGrEclipseFormatter(boolean configurationCache) throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    groovy { greclipse() }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub'");
			setFile("sub/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					    id 'groovy'
					}
					repositories { mavenCentral() }
					spotless {
					    groovy {
					        target file('test.groovy')
					        greclipse()
					    }
					}
					""");
			setFile("sub/test.groovy").toResource("groovy/greclipse/format/unformatted.test");
			String unformatted = read("sub/test.groovy");

			GradleRunner runner = runner(configurationCache, ":sub:spotlessGroovyApply");
			BuildResult result = runner.build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			String formatted = read("sub/test.groovy");
			assertThat(formatted).isNotEqualTo(unformatted).contains("class Foo", "def callBar()");

			if (configurationCache) {
				assertThat(result.getOutput()).contains("Configuration cache entry stored");
				setFile("sub/test.groovy").toContent(unformatted);
				BuildResult reused = runner.build();
				assertThat(reused.getOutput()).contains("Configuration cache entry reused");
				assertThat(read("sub/test.groovy")).isEqualTo(formatted);
			}
		}

		@ParameterizedTest(name = "configurationCache={0}")
		@ValueSource(booleans = {false, true})
		void predeclareFailsWhenGroovyGrEclipseNotPredeclared(boolean configurationCache) throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    // Empty - no Groovy P2 dependencies predeclared
					}
					""");
			setFile("settings.gradle").toContent("include 'sub'");
			setFile("sub/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					    id 'groovy'
					}
					repositories { mavenCentral() }
					spotless {
					    groovy {
					        target file('test.groovy')
					        greclipse()
					    }
					}
					""");
			setFile("sub/test.groovy").toResource("groovy/greclipse/format/unformatted.test");

			BuildResult result = runner(configurationCache, "spotlessApply").buildAndFail();
			assertThat(result.getOutput())
					.contains("P2 dependencies not predeclared")
					.contains("Add Eclipse formatter configuration to the `spotlessPredeclare` block in the root project");
		}

		@ParameterizedTest(name = "configurationCache={0}")
		@ValueSource(booleans = {false, true})
		void predeclareWorksWithMultipleGroovyProjects(boolean configurationCache) throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    groovy { greclipse() }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub1', 'sub2'");
			String unformatted = getTestResource("groovy/greclipse/format/unformatted.test");
			for (int i = 1; i <= 2; i++) {
				setFile("sub" + i + "/build.gradle").toContent("""
						plugins {
						    id 'com.diffplug.spotless'
						    id 'groovy'
						}
						repositories { mavenCentral() }
						spotless {
						    groovy {
						        target file('test.groovy')
						        greclipse()
						    }
						}
						""");
				setFile("sub" + i + "/test.groovy").toContent(unformatted);
			}

			BuildResult result = runner(configurationCache, "spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			if (configurationCache) {
				assertThat(result.getOutput()).contains("Configuration cache entry stored");
			}
			// Verify all files were formatted
			for (int i = 1; i <= 2; i++) {
				String formatted = read("sub" + i + "/test.groovy");
				assertThat(formatted).isNotEqualTo(unformatted).contains("class Foo", "def callBar()");
			}
		}

		/** Writes gradle.properties when configuration caching is enabled, then creates the runner. */
		private GradleRunner runner(boolean configurationCache, String task) throws IOException {
			if (configurationCache) {
				setFile("gradle.properties").toContent("org.gradle.configuration-cache.parallel=true");
			}
			String cacheOption = configurationCache ? "--configuration-cache" : "--no-configuration-cache";
			return gradleRunner().withGradleVersion(GRADLE_VERSION).withArguments(task, cacheOption);
		}
	}

	@Nested
	class MixedDependencies {
		@Test
		void predeclareWorksWithJavaAndGroovyFormatters() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    java { eclipse() }
					    groovy { greclipse() }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub1', 'sub2'");
			setFile("sub1/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        eclipse()
					    }
					}
					""");
			setFile("sub1/test.java").toResource("java/eclipse/JavaCodeUnformatted.test");
			setFile("sub2/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					    id 'groovy'
					}
					repositories { mavenCentral() }
					spotless {
					    groovy {
					        target file('test.groovy')
					        greclipse()
					    }
					}
					""");
			setFile("sub2/test.groovy").toResource("groovy/greclipse/format/unformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			// Verify Java file was formatted
			String javaFormatted = read("sub1/test.java");
			assertThat(javaFormatted).contains("main(String[] args)");
			assertThat(javaFormatted).doesNotStartWith("public static void main");
			// Verify Groovy file was formatted
			String groovyFormatted = read("sub2/test.groovy");
			assertThat(groovyFormatted).contains("class Foo");
			assertThat(groovyFormatted).contains("def callBar()");
		}

		@Test
		void predeclareWorksWithBothMavenAndP2Dependencies() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					    java {
					        googleJavaFormat()
					        eclipse()
					    }
					}
					""");
			setFile("settings.gradle").toContent("include 'sub1', 'sub2'");
			setFile("sub1/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        googleJavaFormat()
					    }
					}
					""");
			setFile("sub1/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
			setFile("sub2/build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless {
					    java {
					        target file('test.java')
					        eclipse()
					    }
					}
					""");
			setFile("sub2/test.java").toResource("java/eclipse/JavaCodeUnformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("sub1/test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
			// Verify Eclipse formatted file has proper indentation
			String eclipseFormatted = read("sub2/test.java");
			assertThat(eclipseFormatted).contains("main(String[] args)");
			assertThat(eclipseFormatted).as("Should be indented").doesNotStartWith("public static void main");
		}
	}

	@Nested
	class Gradle9Compatibility {
		@Test
		void issue2599_Gradle951_CanUsePredeclareDepsFromBuildscript() throws IOException {
			setFile("build.gradle.kts").toContent("""
					import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare

					buildscript {
					    repositories { mavenCentral() }
					}
					plugins {
					    id("com.diffplug.spotless")
					}
					spotless {
					    predeclareDepsFromBuildscript()
					}
					configure<SpotlessExtensionPredeclare> {
					    kotlin { ktfmt("0.56") }
					}
					spotless {
					    kotlin {
					        target("src/**/*.kt")
					        ktfmt("0.56")
					    }
					}
					""");
			setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktfmt/basic.dirty");

			BuildResult result = gradleRunner()
					.withGradleVersion(GRADLE_VERSION)
					.withArguments("spotlessApply", "--stacktrace")
					.build();

			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktfmt/basic.clean");
		}

		@Test
		void gradle951CanUsePredeclareWithProjectRepositories() throws IOException {
			setFile("build.gradle.kts").toContent("""
					import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare

					plugins {
					    id("com.diffplug.spotless")
					}
					repositories { mavenCentral() }
					spotless {
					    predeclareDeps()
					}
					configure<SpotlessExtensionPredeclare> {
					    kotlin { ktfmt("0.56") }
					}
					spotless {
					    kotlin {
					        target("src/**/*.kt")
					        ktfmt("0.56")
					    }
					}
					""");
			setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktfmt/basic.dirty");

			BuildResult withoutConfigurationCache = gradleRunner()
					.withGradleVersion(GRADLE_VERSION)
					.withArguments("spotlessApply", "--stacktrace")
					.build();
			assertThat(withoutConfigurationCache.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktfmt/basic.clean");

			setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktfmt/basic.dirty");
			BuildResult withConfigurationCache = gradleRunner()
					.withGradleVersion(GRADLE_VERSION)
					.withArguments("spotlessApply", "--configuration-cache", "--stacktrace")
					.build();
			assertThat(withConfigurationCache.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktfmt/basic.clean");
		}
	}

	@Nested
	class EdgeCases {
		@Test
		void unrelatedTasksDoNotResolvePredeclaredDependencies() throws IOException {
			// Omitting repositories makes any formatter dependency resolution fail.
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					spotlessPredeclare {
					    java { googleJavaFormat() }
					}
					""");

			BuildResult result = gradleRunner().withArguments("help").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

			BuildResult provisioning = gradleRunner().withArguments(RegisterDependenciesTask.TASK_NAME).buildAndFail();
			assertThat(provisioning.getOutput()).contains("com.google.googlejavaformat:google-java-format:", "because no repositories are defined");
		}

		@Test
		void predeclareBlockEnablesPredeclareDeps() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotlessPredeclare {
					    format('misc') { trimTrailingWhitespace() }
					}
					""");

			BuildResult result = gradleRunner().withArguments(RegisterDependenciesTask.TASK_NAME).build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
		}

		@Test
		void predeclareBlockCanComeBeforePredeclareDeps() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotlessPredeclare {
					    format('misc') { trimTrailingWhitespace() }
					}
					spotless { predeclareDeps() }
					""");

			BuildResult result = gradleRunner().withArguments(RegisterDependenciesTask.TASK_NAME).build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
		}

		@Test
		void predeclareBlockHasKotlinDslAccessor() throws IOException {
			setFile("build.gradle.kts").toContent("""
					plugins {
					    id("com.diffplug.spotless")
					}
					repositories { mavenCentral() }
					spotlessPredeclare {
					    format("misc") { trimTrailingWhitespace() }
					}
					""");

			BuildResult result = gradleRunner().withArguments(RegisterDependenciesTask.TASK_NAME).build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
		}

		@Test
		void predeclareBlockCanSelectBuildscriptRepositories() throws IOException {
			setFile("build.gradle.kts").toContent("""
					plugins {
					    id("com.diffplug.spotless")
					}
					spotlessPredeclare {
					    format("misc") { trimTrailingWhitespace() }
					    fromBuildscriptRepositories()
					}
					""");

			BuildResult result = gradleRunner().withArguments(RegisterDependenciesTask.TASK_NAME).build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
		}

		@Test
		void emptyPredeclareBlockIsValid() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotless { predeclareDeps() }
					spotlessPredeclare {
					}
					""");

			BuildResult result = gradleRunner().withArguments(RegisterDependenciesTask.TASK_NAME).build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
		}
	}
}
