/*
 * Copyright 2021 DiffPlug
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
package com.diffplug.spotless.pom;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

// Class and members must be public, otherwise we get failed to access class com.diffplug.spotless.pom.SortPomInternalState from class com.diffplug.spotless.pom.SortPomFormatterFunc (com.diffplug.spotless.pom.SortPomInternalState is in unnamed module of loader org.codehaus.plexus.classworlds.realm.ClassRealm @682bd3c4; com.diffplug.spotless.pom.SortPomFormatterFunc is in unnamed module of loader com.diffplug.spotless.pom.DelegatingClassLoader @573284a5)
public final class SortPomState implements Serializable {
	private static final long serialVersionUID = 1L;

	public final JarState jarState;

	public final String encoding;

	public final String lineSeparator;

	public final boolean expandEmptyElements;

	public final boolean spaceBeforeCloseEmptyElement;

	public final boolean keepBlankLines;

	public final int nrOfIndentSpace;

	public final boolean indentBlankLines;

	public final boolean indentSchemaLocation;

	public final String predefinedSortOrder;

	public final String sortOrderFile;

	public final String sortDependencies;

	public final String sortDependencyExclusions;

	public final String sortPlugins;

	public final boolean sortProperties;

	public final boolean sortModules;

	public final boolean sortExecutions;

	SortPomState(String encoding, String lineSeparator, boolean expandEmptyElements, boolean spaceBeforeCloseEmptyElement, boolean keepBlankLines, int nrOfIndentSpace, boolean indentBlankLines, boolean indentSchemaLocation, String predefinedSortOrder, String sortOrderFile, String sortDependencies, String sortDependencyExclusions, String sortPlugins, boolean sortProperties, boolean sortModules, boolean sortExecutions, Provisioner provisioner) throws IOException {
		this.jarState = JarState.from("com.github.ekryd.sortpom:sortpom-sorter:3.0.0", provisioner);
		this.encoding = encoding;
		this.lineSeparator = lineSeparator;
		this.expandEmptyElements = expandEmptyElements;
		this.spaceBeforeCloseEmptyElement = spaceBeforeCloseEmptyElement;
		this.keepBlankLines = keepBlankLines;
		this.nrOfIndentSpace = nrOfIndentSpace;
		this.indentBlankLines = indentBlankLines;
		this.indentSchemaLocation = indentSchemaLocation;
		this.predefinedSortOrder = predefinedSortOrder;
		this.sortOrderFile = sortOrderFile;
		this.sortDependencies = sortDependencies;
		this.sortDependencyExclusions = sortDependencyExclusions;
		this.sortPlugins = sortPlugins;
		this.sortProperties = sortProperties;
		this.sortModules = sortModules;
		this.sortExecutions = sortExecutions;
	}

	FormatterFunc createFormat() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
		ClassLoader classLoader = new DelegatingClassLoader(this.getClass().getClassLoader(), jarState.getClassLoader());
		Constructor<?> constructor = classLoader.loadClass(SortPomFormatterFunc.class.getName()).getConstructor(classLoader.loadClass(SortPomState.class.getName()));
		constructor.setAccessible(true);
		return (FormatterFunc) constructor.newInstance(this);
	}
}
