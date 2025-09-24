/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.maven;

import static com.diffplug.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import com.diffplug.common.base.Unhandled;
import com.diffplug.common.io.Resources;
import com.diffplug.selfie.Selfie;
import com.diffplug.selfie.StringSelfie;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.ResourceHarness;

public class MavenIntegrationHarness extends ResourceHarness {
	/**
	 * To run tests in the IDE, run {@code gradlew :plugin-maven:changelogPrint}, then
	 * put the last version it prints into {@code SPOTLESS_MAVEN_VERSION_IDE}.  From now
	 * on, if you run {@code gradlew :plugin-maven:runMavenBuild}, then you can run tests
	 * in the IDE and they will run against the results of the last {@code runMavenBuild}
	 */
	private static final String SPOTLESS_MAVEN_VERSION_IDE = null;

	private static final String POM_TEMPLATE = "/pom-test.xml.mustache";
	private static final String SPOTLESS_MAVEN_PLUGIN_VERSION = "spotlessMavenPluginVersion";
	private static final String BUILD = "build";
	private static final String CONFIGURATION = "configuration";
	private static final String EXECUTIONS = "executions";
	private static final String MODULES = "modules";
	private static final String DEPENDENCIES = "dependencies";
	private static final String PLUGINS = "plugins";
	private static final String MODULE_NAME = "name";
	private static final int REMOTE_DEBUG_PORT = 5005;

	private final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

	/**
	 * Each test gets its own temp folder, and we create a maven
	 * build there and run it.
	 * <p>
	 * Because those test folders don't have a .gitattributes file,
	 * git on windows will default to \r\n. So now if you read a
	 * test file from the spotless test resources, and compare it
	 * to a build result, the line endings won't match.
	 * <p>
	 * By sticking this .gitattributes file into the test directory,
	 * we ensure that the default Spotless line endings policy of
	 * GIT_ATTRIBUTES will use \n, so that tests match the test
	 * resources on win and linux.
	 */
	@BeforeEach
	void gitAttributes() throws IOException {
		setFile(".gitattributes").toContent("* text eol=lf");
		if (Jvm.version() >= 16) {
			// for GJF https://github.com/diffplug/spotless/issues/834
			setFile(".mvn/jvm.config").toContent(
					"--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED" +
							" --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED" +
							" --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED" +
							" --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED" +
							" --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED" +
							// this last line is for Detekt
							" --add-opens java.base/java.lang=ALL-UNNAMED");
		}
		// copy the mvnw resources
		copy("mvnw").setExecutable(true);
		copy("mvnw.cmd");
		copy(".mvn/wrapper/maven-wrapper.jar");
		copy(".mvn/wrapper/maven-wrapper.properties");
	}

	private File copy(String path) throws IOException {
		byte[] bytes = Resources.toByteArray(ResourceHarness.class.getResource("/" + path));
		Path target = newFile(path).toPath();
		Files.createDirectories(target.getParent());
		Files.write(target, bytes);
		return target.toFile();
	}

	protected void writePomWithFormatSteps(String... steps) throws IOException {
		writePom(formats(groupWithSteps("format", including("src/**/java/**/*.java"), steps)));
	}

	protected void writePomWithAntlr4Steps(String... steps) throws IOException {
		writePom(groupWithSteps("antlr4", steps));
	}

	protected void writePomWithGroovySteps(String... steps) throws IOException {
		writePom(groupWithSteps("groovy", steps));
	}

	protected void writePomWithJavaSteps(String... steps) throws IOException {
		writePom(groupWithSteps("java", steps));
	}

	protected void writePomWithScalaSteps(String... steps) throws IOException {
		writePom(groupWithSteps("scala", steps));
	}

	protected void writePomWithKotlinSteps(String... steps) throws IOException {
		writePom(groupWithSteps("kotlin", steps));
	}

	protected void writePomWithCppSteps(String... steps) throws IOException {
		writePom(groupWithSteps("cpp", steps));
	}

	protected void writePomWithJavascriptSteps(String includes, String... steps) throws IOException {
		writePom(groupWithSteps("javascript", including(includes), steps));
	}

	protected void writePomWithTypescriptSteps(String includes, String... steps) throws IOException {
		writePom(groupWithSteps("typescript", including(includes), steps));
	}

	protected void writePomWithSqlSteps(String... steps) throws IOException {
		writePom(groupWithSteps("sql", including("**/*.sql"), steps));
	}

	protected void writePomWithPrettierSteps(String includes, String... steps) throws IOException {
		writePom(formats(groupWithSteps("format", including(includes), steps)));
	}

	protected void writePomWithBiomeSteps(String includes, String... steps) throws IOException {
		writePom(formats(groupWithSteps("format", including(includes), steps)));
	}

	@Deprecated
	protected void writePomWithRomeSteps(String includes, String... steps) throws IOException {
		writePom(formats(groupWithSteps("format", including(includes), steps)));
	}

	protected void writePomWithPrettierSteps(String[] plugins, String includes, String... steps) throws IOException {
		writePom(null, formats(groupWithSteps("format", including(includes), steps)), null, plugins);
	}

	protected void writePomWithPomSteps(String... steps) throws IOException {
		writePom(groupWithSteps("pom", including("pom_test.xml"), steps));
	}

	protected void writePomWithProtobufSteps(String... steps) throws IOException {
		writePom(groupWithSteps("protobuf", steps));
	}

	protected void writePomWithMarkdownSteps(String... steps) throws IOException {
		writePom(groupWithSteps("markdown", including("**/*.md"), steps));
	}

	protected void writePomWithJsonSteps(String... steps) throws IOException {
		writePom(groupWithSteps("json", including("**/*.json"), steps));
	}

	protected void writePomWithCssSteps(String... steps) throws IOException {
		writePom(groupWithSteps("css", including("**/*.css"), steps));
	}

	protected void writePomWithShellSteps(String... steps) throws IOException {
		writePom(groupWithSteps("shell", including("**/*.sh"), steps));
	}

	protected void writePomWithYamlSteps(String... steps) throws IOException {
		writePom(groupWithSteps("yaml", including("**/*.yaml"), steps));
	}

	protected void writePomWithGherkinSteps(String... steps) throws IOException {
		writePom(groupWithSteps("gherkin", including("**/*.feature"), steps));
	}

	protected void writePomWithGoSteps(String... steps) throws IOException {
		writePom(groupWithSteps("go", including("**/*.go"), steps));
	}

	protected void writePom(String... configuration) throws IOException {
		writePom(null, configuration, null, null);
	}

	protected void writePom(String[] executions, String[] configuration, String[] dependencies, String[] plugins) throws IOException {
		String pomXmlContent = createPomXmlContent(null, executions, configuration, dependencies, plugins);
		setFile("pom.xml").toContent(pomXmlContent);
	}

	protected MavenRunner mavenRunner() throws IOException {
		MavenRunner mavenRunner = MavenRunner.create()
				.withProjectDir(rootFolder())
				.withRunner(runner);
		System.getProperties().forEach((key, value) -> {
			if (key instanceof String && ((String) key).startsWith("spotless") && value instanceof String) {
				mavenRunner.withSystemProperty((String) key, (String) value);
			}
		});
		return mavenRunner;
	}

	private static ProcessRunner runner;

	@BeforeAll
	static void setupRunner() throws IOException {
		runner = new ProcessRunner();
	}

	@AfterAll
	static void closeRunner() throws IOException {
		runner.close();
	}

	/**
	 * Useful for local development. Allows debugging the Spotless Maven Plugin remotely.
	 * Effectively translates into running {@code mvnDebug} on port 5005. The forked JVM will be
	 * suspended until the debugger connects.
	 */
	protected MavenRunner mavenRunnerWithRemoteDebug() throws IOException {
		return mavenRunner().withRemoteDebug(REMOTE_DEBUG_PORT);
	}

	protected String createPomXmlContent(String pluginVersion, String[] executions, String[] configuration, String[] dependencies, String[] plugins) throws IOException {
		return createPomXmlContent(POM_TEMPLATE, pluginVersion, executions, configuration, dependencies, plugins);
	}

	protected String createPomXmlContent(String pomTemplate, String pluginVersion, String[] executions, String[] configuration, String[] dependencies, String[] plugins) throws IOException {
		Map<String, Object> params = buildPomXmlParams(pluginVersion, null, executions, configuration, null, dependencies, plugins);
		return createPomXmlContent(pomTemplate, params);
	}

	protected String createPomXmlContent(String pluginVersion, String[] executions, String[] configuration) throws IOException {
		return createPomXmlContent(pluginVersion, executions, configuration, null, null);
	}

	protected String createPomXmlContent(String[] build, String[] configuration) throws IOException {
		Map<String, Object> params = buildPomXmlParams(null, build, null, configuration, null, null, null);
		return createPomXmlContent(POM_TEMPLATE, params);
	}

	protected String createPomXmlContent(String pomTemplate, Map<String, Object> params) throws IOException {
		URL url = MavenIntegrationHarness.class.getResource(pomTemplate);
		try (BufferedReader reader = Resources.asCharSource(url, StandardCharsets.UTF_8).openBufferedStream()) {
			Mustache mustache = mustacheFactory.compile(reader, "pom");
			StringWriter writer = new StringWriter();
			mustache.execute(writer, params);
			return writer.toString();
		}
	}

	protected static Map<String, Object> buildPomXmlParams(String pluginVersion, String[] build, String[] executions, String[] configuration, String[] modules, String[] dependencies, String[] plugins) {
		Map<String, Object> params = new HashMap<>();
		params.put(SPOTLESS_MAVEN_PLUGIN_VERSION, pluginVersion == null ? getSystemProperty(SPOTLESS_MAVEN_PLUGIN_VERSION) : pluginVersion);

		if (build != null) {
			params.put(BUILD, String.join("\n", build));
		}

		if (configuration != null) {
			params.put(CONFIGURATION, String.join("\n", configuration));
		}

		if (executions != null) {
			params.put(EXECUTIONS, String.join("\n", executions));
		}

		if (modules != null) {
			List<Map<String, String>> moduleNames = stream(modules).map(name -> singletonMap(MODULE_NAME, name)).collect(toList());
			params.put(MODULES, moduleNames);
		}

		if (dependencies != null) {
			params.put(DEPENDENCIES, String.join("\n", dependencies));
		}

		if (plugins != null) {
			params.put(PLUGINS, String.join("\n", plugins));
		}

		return params;
	}

	private static String getSystemProperty(String name) {
		if (SPOTLESS_MAVEN_VERSION_IDE != null) {
			if (name.equals("spotlessMavenPluginVersion")) {
				return SPOTLESS_MAVEN_VERSION_IDE;
			} else {
				throw Unhandled.stringException(name);
			}
		}
		String value = System.getProperty(name);
		if (isNullOrEmpty(value)) {
			fail("System property '" + name + "' is not defined");
		}
		return value;
	}

	protected static String[] groupWithSteps(String group, String[] includes, String... steps) {
		String[] result = new String[steps.length + includes.length + 2];
		result[0] = "<" + group + ">";
		System.arraycopy(includes, 0, result, 1, includes.length);
		System.arraycopy(steps, 0, result, includes.length + 1, steps.length);
		result[result.length - 1] = "</" + group + ">";
		return result;
	}

	protected static String[] groupWithSteps(String group, String... steps) {
		return groupWithSteps(group, new String[]{}, steps);
	}

	protected static String[] including(String... includes) {
		return groupWithSteps("includes", groupWithSteps("include", includes));
	}

	protected static String[] formats(String... formats) {
		return groupWithSteps("formats", formats);
	}

	protected static String[] formats(String[]... formats) {
		String[] formatsArray = Arrays.stream(formats)
				.flatMap(Arrays::stream)
				.toArray(String[]::new);
		return formats(formatsArray);
	}

	private static final String ERROR_PREFIX = "[ERROR] ";

	protected StringSelfie expectSelfieErrorMsg(ProcessRunner.Result result) {
		String concatenatedError = result.stdOutUtf8().lines()
				.map(line -> line.startsWith(ERROR_PREFIX) ? line.substring(ERROR_PREFIX.length()) : null)
				.filter(line -> line != null)
				.collect(Collectors.joining("\n"));

		String sanitizedVersion = concatenatedError.replaceFirst("com\\.diffplug\\.spotless:spotless-maven-plugin:([^:]+):", "com.diffplug.spotless:spotless-maven-plugin:VERSION:");

		int help1 = sanitizedVersion.indexOf("-> [Help 1]");
		String trimTrailingString = sanitizedVersion.substring(0, help1);

		String sanitizeBiomeNative = trimTrailingString.replaceAll("[/|\\\\].m2(.*)[/|\\\\]biome\\-(.+),", "biome-exe");
		String sanitizeFilePath = sanitizeBiomeNative.replace(rootFolder().getAbsolutePath(), "${PROJECT_DIR}");
		String sanitizeUserHome = sanitizeFilePath.replace(System.getProperty("user.home"), "${user.home}");
		String sanitizeWindowsPathSep = sanitizeUserHome.replace('\\', '/');
		return Selfie.expectSelfie(sanitizeWindowsPathSep);
	}
}
