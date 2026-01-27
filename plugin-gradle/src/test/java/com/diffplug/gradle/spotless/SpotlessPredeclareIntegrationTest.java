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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the spotlessPredeclare feature, which allows dependencies
 * to be predeclared in the root project and reused across all subprojects to avoid
 * OutOfMemoryErrors when multiple projects resolve the same dependencies concurrently.
 */
class SpotlessPredeclareIntegrationTest extends GradleIntegrationHarness {

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
					    java { googleJavaFormat('1.17.0') }
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
					        googleJavaFormat('1.17.0')
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
					        googleJavaFormat('1.17.0')
					    }
					}
					""");
			setFile("sub/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

			BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
			assertThat(result.getOutput())
					.contains("Add a step with [com.google.googlejavaformat:google-java-format:1.17.0]")
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
					    java { googleJavaFormat('1.17.0') }
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
					        googleJavaFormat('1.17.0')
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
					        googleJavaFormat('1.17.0')
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
			assertThat(result.getOutput())
					.contains("P2 dependencies not predeclared")
					.contains("Add Eclipse formatter configuration to the `spotlessPredeclare` block in the root project");
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
		@Test
		void predeclareSucceedsWithGroovyGrEclipseFormatter() throws IOException {
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

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			// Verify the file was formatted
			String formatted = read("sub/test.groovy");
			assertThat(formatted).contains("class Foo");
			assertThat(formatted).contains("def callBar()");
		}

		@Test
		void predeclareFailsWhenGroovyGrEclipseNotPredeclared() throws IOException {
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

			BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
			assertThat(result.getOutput())
					.contains("P2 dependencies not predeclared")
					.contains("Add Eclipse formatter configuration to the `spotlessPredeclare` block in the root project");
		}

		@Test
		void predeclareWorksWithMultipleGroovyProjects() throws IOException {
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
				setFile("sub" + i + "/test.groovy").toResource("groovy/greclipse/format/unformatted.test");
			}

			BuildResult result = gradleRunner().withArguments("spotlessApply").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
			// Verify all files were formatted
			for (int i = 1; i <= 2; i++) {
				String formatted = read("sub" + i + "/test.groovy");
				assertThat(formatted).contains("class Foo");
				assertThat(formatted).contains("def callBar()");
			}
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
					        googleJavaFormat('1.17.0')
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
					        googleJavaFormat('1.17.0')
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
	class EdgeCases {
		@Test
		void predeclareRequiresPredeclareDepsCall() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotlessPredeclare {
					    java { googleJavaFormat('1.17.0') }
					}
					""");

			BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
			assertThat(result.getOutput())
					.contains("Could not find method spotlessPredeclare() for arguments");
		}

		@Test
		void predeclareBlockMustComeAfterPredeclareDeps() throws IOException {
			setFile("build.gradle").toContent("""
					plugins {
					    id 'com.diffplug.spotless'
					}
					repositories { mavenCentral() }
					spotlessPredeclare {
					    java { googleJavaFormat('1.17.0') }
					}
					spotless { predeclareDeps() }
					""");

			BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
			assertThat(result.getOutput())
					.contains("Could not find method spotlessPredeclare() for arguments");
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

			BuildResult result = gradleRunner().withArguments("help").build();
			assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
		}
	}
}
