/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.glue.java;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatterFunc;

import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.formatter.PathAndContent;

/**
 * The glue for CleanThat: it is build over the version in build.gradle, but at runtime it will be executed over
 * the version loaded in JarState, which is by default defined in com.diffplug.spotless.java.CleanthatJavaStep#JVM_SUPPORT
 */
public class JavaCleanthatRefactorerFunc implements FormatterFunc.NeedsFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaCleanthatRefactorerFunc.class);

	private String jdkVersion;
	private List<String> included;
	private List<String> excluded;
	private boolean includeDraft;

	public JavaCleanthatRefactorerFunc(String jdkVersion, List<String> included, List<String> excluded, boolean includeDraft) {
		this.jdkVersion = jdkVersion == null ? IJdkVersionConstants.JDK_8 : jdkVersion;
		this.included = included == null ? emptyList() : included;
		this.excluded = excluded == null ? emptyList() : excluded;
		this.includeDraft = includeDraft;
	}

	public JavaCleanthatRefactorerFunc() {
		this(IJdkVersionConstants.JDK_8, Arrays.asList("SafeAndConsensual"), Arrays.asList(), false);
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		// https://stackoverflow.com/questions/1771679/difference-between-threads-context-class-loader-and-normal-classloader
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			// Ensure CleanThat main Thread has its custom classLoader while executing its refactoring
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			return doApply(unix, file);
		} finally {
			// Restore the originalClassLoader
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private String doApply(String input, File file) throws IOException {
		// call some API that uses reflection without taking ClassLoader param
		CleanthatEngineProperties engineProperties = CleanthatEngineProperties.builder().engineVersion(jdkVersion).build();

		// Spotless will push us LF content
		engineProperties.setSourceCode(SourceCodeProperties.builder().lineEnding(LineEnding.LF).build());

		JavaRefactorerProperties refactorerProperties = new JavaRefactorerProperties();

		refactorerProperties.setIncluded(included);
		refactorerProperties.setExcluded(excluded);

		refactorerProperties.setIncludeDraft(includeDraft);

		JavaRefactorer refactorer = new JavaRefactorer(engineProperties, refactorerProperties);

		LOGGER.debug("Processing sourceJdk={} included={} excluded={}", jdkVersion, included, excluded, includeDraft);
		LOGGER.debug("Available mutators: {}", JavaRefactorer.getAllIncluded());

		PathAndContent pathAndContent = new PathAndContent(file.toPath(), input);

		return refactorer.doFormat(pathAndContent);
	}

}
