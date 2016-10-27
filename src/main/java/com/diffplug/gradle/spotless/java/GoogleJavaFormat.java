/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.gradle.spotless.java;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
class GoogleJavaFormat {
	static final String NAME = "google-java-format";
	static final String DEFAULT_VERSION = "1.1";
	private static final String MAVEN_COORDINATE = "com.google.googlejavaformat:google-java-format:";
	private static final String FORMATTER_CLASS = "com.google.googlejavaformat.java.Formatter";
	private static final String FORMATTER_METHOD = "formatSource";

	/** Returns a function which will call the google-java-format tool. */
	@SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
	static Throwing.Function<String, String> createRule(String version, Project project) throws Exception {
		// get the googleJavaFormat configuration
		Dependency googleJavaFormatJar = project.getDependencies().create(MAVEN_COORDINATE + version);
		Configuration configuration = project.getConfigurations().detachedConfiguration(googleJavaFormatJar);
		configuration.setDescription("google-java-format");

		// get a classloader which contains only the jars in this configuration
		Set<File> jars;
		try {
			jars = configuration.resolve();
		} catch (Exception e) {
			System.err.println("You probably need to add a repository containing the `google-java-format` artifact to your buildscript,");
			System.err.println("e.g.: repositories { mavenCentral() }");
			throw e;
		}
		URL[] jarUrls = jars.stream().map(Errors.rethrow().wrapFunction(
				file -> file.toURI().toURL())).toArray(size -> new URL[size]);
		URLClassLoader classLoader = new URLClassLoader(jarUrls);
		// TODO: dispose the classloader when the function
		// that we return gets garbage-collected

		// instantiate the formatter and get its format method
		Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
		Object formatter = formatterClazz.getConstructor().newInstance();
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
		return input -> (String) method.invoke(formatter, input);
	}
}
