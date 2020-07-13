/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.scala;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ScalaCompiler implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String version;
	private final Set<String> plugins;
	private final Set<String> compilerOptions;
	private final String classpath;
	private Boolean enabled = false;

	public ScalaCompiler(final String version, final String classpath) {
		this.plugins = new HashSet<>();
		this.compilerOptions = new HashSet<>();
		this.version = version;
		this.classpath = classpath;
	}

	public String getVersion() {
		return version;
	}

	public String getClasspath() {
		return classpath;
	}

	public Set<String> getPlugins() {
		return plugins;
	}

	public Set<String> getCompilerOptions() {
		return compilerOptions;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	public void addPlugin(final String dependency) {
		plugins.add(dependency);
	}

	public void addCompilerOptions(final Collection<String> options) {
		compilerOptions.addAll(options);
	}

	/**
	 * Call this to compile the project before spotless is applied
	 */
	public void enable() {
		enabled = true;
	}
}
