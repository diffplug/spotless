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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class SortPomStep {

	public static final String NAME = "sortPom";

	private SortPomStep() {}

	public static FormatterStep create(String encoding, String lineSeparator, boolean expandEmptyElements, boolean spaceBeforeCloseEmptyElement, boolean keepBlankLines, int nrOfIndentSpace, boolean indentBlankLines, boolean indentSchemaLocation, String predefinedSortOrder, String sortOrderFile, String sortDependencies, String sortDependencyExclusions, String sortPlugins, boolean sortProperties, boolean sortModules, boolean sortExecutions, Provisioner provisioner) {
		return FormatterStep.createLazy(NAME, () -> new State(encoding, lineSeparator, expandEmptyElements, spaceBeforeCloseEmptyElement, keepBlankLines, nrOfIndentSpace, indentBlankLines, indentSchemaLocation, predefinedSortOrder, sortOrderFile, sortDependencies, sortDependencyExclusions, sortPlugins, sortProperties, sortModules, sortExecutions, provisioner), State::createFormat);
	}

	static final class InternalState implements Serializable {
		private static final long serialVersionUID = 1L;

		final String encoding;

		final String lineSeparator;

		final boolean expandEmptyElements;

		final boolean spaceBeforeCloseEmptyElement;

		final boolean keepBlankLines;

		final int nrOfIndentSpace;

		final boolean indentBlankLines;

		final boolean indentSchemaLocation;

		final String predefinedSortOrder;

		final String sortOrderFile;

		final String sortDependencies;

		final String sortDependencyExclusions;

		final String sortPlugins;

		final boolean sortProperties;

		final boolean sortModules;

		final boolean sortExecutions;

		InternalState(String encoding, String lineSeparator, boolean expandEmptyElements, boolean spaceBeforeCloseEmptyElement, boolean keepBlankLines, int nrOfIndentSpace, boolean indentBlankLines, boolean indentSchemaLocation, String predefinedSortOrder, String sortOrderFile, String sortDependencies, String sortDependencyExclusions, String sortPlugins, boolean sortProperties, boolean sortModules, boolean sortExecutions) {
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
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;
		final JarState jarState;

		final InternalState internalState;

		State(String encoding, String lineSeparator, boolean expandEmptyElements, boolean spaceBeforeCloseEmptyElement, boolean keepBlankLines, int nrOfIndentSpace, boolean indentBlankLines, boolean indentSchemaLocation, String predefinedSortOrder, String sortOrderFile, String sortDependencies, String sortDependencyExclusions, String sortPlugins, boolean sortProperties, boolean sortModules, boolean sortExecutions, Provisioner provisioner) throws IOException {
			this.jarState = JarState.from("com.github.ekryd.sortpom:sortpom-sorter:3.0.0", provisioner);
			this.internalState = new InternalState(encoding, lineSeparator, expandEmptyElements, spaceBeforeCloseEmptyElement, keepBlankLines, nrOfIndentSpace, indentBlankLines, indentSchemaLocation, predefinedSortOrder, sortOrderFile, sortDependencies, sortDependencyExclusions, sortPlugins, sortProperties, sortModules, sortExecutions);
		}

		FormatterFunc createFormat() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
			ClassLoader classLoader = new DelegatingClassLoader(this.getClass().getClassLoader(), jarState.getClassLoader());
			Constructor<?> constructor = classLoader.loadClass(SortPomFormatterFunc.class.getName()).getConstructor(classLoader.loadClass(InternalState.class.getName()));
			constructor.setAccessible(true);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(internalState);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())) {
				@Override
				protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
					return classLoader.loadClass(desc.getName());
				}
			};
			Object state = ois.readObject();
			Object formatterFunc = constructor.newInstance(state);
			Method apply = formatterFunc.getClass().getMethod("apply", String.class);
			apply.setAccessible(true);
			return input -> (String) apply.invoke(formatterFunc, input);
		}

	}
}
