/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.extra.glue.jdt;

import java.util.Comparator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.SortElementsOperation;

public class EclipseJdtSortMembers {

	private static CompilationUnit compilationUnit(String code) {
		return new CompilationUnit(null, null, null) {
			private final Buffer buffer = new Buffer(code);

			@Override
			public IBuffer getBuffer() {
				return buffer;
			}

			@Override
			public JavaProject getJavaProject() {
				return new JavaProject(null, null) {
					@Override
					public Map<String, String> getOptions(boolean inheritJavaCoreOptions) {
						return Map.of();
					}
				};
			}

			@Override
			public Map<String, String> getOptions(boolean inheritJavaCoreOptions) {
				return Map.of();
			}

			@Override
			public ICompilationUnit getPrimary() {
				return this;
			}
		};
	}

	static String sortMember(String code, SortProperties properties) {
		if (!properties.enabled) {
			return code;
		}

		try {
			CompilationUnit compilationUnit = compilationUnit(code);
			DefaultJavaElementComparator comparator = DefaultJavaElementComparator.of(
					properties.doNotSortFields,
					properties.membersOrder,
					properties.sortByVisibility,
					properties.visibilityOrder);
			new Sorter(AST.getJLSLatest(), compilationUnit, null, comparator).sort();
			String content = compilationUnit.getBuffer().getContents();
			if (content != null) {
				code = content;
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		return code;
	}

	private static class Buffer implements IBuffer {

		private String contents;

		Buffer(String contents) {
			this.contents = contents;
		}

		public void addBufferChangedListener(IBufferChangedListener listener) {}

		public void append(char[] text) {}

		public void append(String text) {}

		public void close() {}

		public char getChar(int position) {
			return '\u0000';
		}

		public char[] getCharacters() {
			return contents.toCharArray();
		}

		public String getContents() {
			return contents;
		}

		public int getLength() {
			return 0;
		}

		public IOpenable getOwner() {
			return null;
		}

		public String getText(int offset, int length) {
			return null;
		}

		public IResource getUnderlyingResource() {
			return null;
		}

		public boolean hasUnsavedChanges() {
			return false;
		}

		public boolean isClosed() {
			return false;
		}

		public boolean isReadOnly() {
			return true;
		}

		public void removeBufferChangedListener(IBufferChangedListener listener) {}

		public void replace(int position, int length, char[] text) {}

		public void replace(int position, int length, String text) {}

		public void save(IProgressMonitor progress, boolean force) {}

		public void setContents(char[] contents) {}

		public void setContents(String contents) {
			this.contents = contents;
		}
	}

	private static class Sorter extends SortElementsOperation {

		Sorter(int level, CompilationUnit compilationUnit, int[] positions, Comparator comparator) {
			super(level, new IJavaElement[]{compilationUnit}, positions, comparator);
		}

		void sort() throws JavaModelException {
			executeOperation();
		}
	}

	static class SortProperties {
		final boolean doNotSortFields;
		final boolean enabled;
		final String membersOrder;
		final boolean sortByVisibility;
		final String visibilityOrder;

		SortProperties(
				boolean enabled,
				String membersOrder,
				boolean doNotSortFields,
				boolean sortByVisibility,
				String visibilityOrder) {
			this.enabled = enabled;
			this.membersOrder = membersOrder;
			this.doNotSortFields = doNotSortFields;
			this.sortByVisibility = sortByVisibility;
			this.visibilityOrder = visibilityOrder;
		}

		static SortProperties from(Map<String, String> properties) {
			boolean enabled = Boolean.parseBoolean(properties.getOrDefault("members.order.enabled", "false"));
			String membersOrder = properties.getOrDefault("members.order", "");
			boolean doNotSortFields = Boolean.parseBoolean(properties.getOrDefault("members.doNotSortFields", "true"));
			boolean sortByVisibility = Boolean.parseBoolean(properties.getOrDefault("visibility.order.enabled", "false"));
			String visibilityOrder = properties.getOrDefault("visibility.order", "");
			// At the moment we see no need for the following options, but they may become important, idk.
			Map<String, String> compilationUnitOptions = Map.of();
			Map<String, String> compilerOptions = Map.of();
			return new SortProperties(enabled, membersOrder, doNotSortFields, sortByVisibility, visibilityOrder);
		}
	}
}
