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
package com.diffplug.spotless.maven.npm;

import java.io.File;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import com.diffplug.spotless.npm.NpmPathResolver;

public abstract class AbstractNpmFormatterStepFactory implements FormatterStepFactory {

	public static final String SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME = "spotless-npm-install-cache";

	@Parameter
	private String npmExecutable;

	@Parameter
	private String nodeExecutable;

	@Parameter
	private String npmrc;

	@Parameter
	private String npmInstallCache;

	protected File npm(FormatterStepConfig stepConfig) {
		File npm = npmExecutable != null ? stepConfig.getFileLocator().locateFile(npmExecutable) : null;
		return npm;
	}

	protected File node(FormatterStepConfig stepConfig) {
		File node = nodeExecutable != null ? stepConfig.getFileLocator().locateFile(nodeExecutable) : null;
		return node;
	}

	protected File npmrc(FormatterStepConfig stepConfig) {
		File npmrc = this.npmrc != null ? stepConfig.getFileLocator().locateFile(this.npmrc) : null;
		return npmrc;
	}

	protected File buildDir(FormatterStepConfig stepConfig) {
		return stepConfig.getFileLocator().getBuildDir();
	}

	protected File cacheDir(FormatterStepConfig stepConfig) {
		if (this.npmInstallCache == null) {
			return null;
		}
		if ("true".equals(this.npmInstallCache.toLowerCase(Locale.ROOT))) {
			return new File(buildDir(stepConfig), SPOTLESS_NPM_INSTALL_CACHE_DEFAULT_NAME);
		}
		return Path.of(this.npmInstallCache).toFile();
	}

	protected File baseDir(FormatterStepConfig stepConfig) {
		return stepConfig.getFileLocator().getBaseDir();
	}

	protected NpmPathResolver npmPathResolver(FormatterStepConfig stepConfig) {
		return new NpmPathResolver(npm(stepConfig), node(stepConfig), npmrc(stepConfig), Collections.singletonList(baseDir(stepConfig)));
	}

	protected boolean moreThanOneNonNull(Object... objects) {
		return Arrays.stream(objects)
				.filter(Objects::nonNull)
				.filter(o -> !(o instanceof String) || !((String) o).isEmpty()) // if it is a string, it should not be empty
				.count() > 1;
	}

	protected Map<String, String> propertiesAsMap(Properties devDependencyProperties) {
		return devDependencyProperties.stringPropertyNames()
				.stream()
				.map(name -> new AbstractMap.SimpleEntry<>(name, devDependencyProperties.getProperty(name)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
