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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.jdt.internal.core.SortElementsOperation;

public class EclipseJdtSortMembers {

	private static final Pattern PATTERN_DO_NOT_SORT_FIELDS = Pattern.compile("@SortMembers:doNotSortFields\\s*=\\s*(false|true)");
	private static final Pattern PATTERN_ENABLED = Pattern.compile("@SortMembers:enabled\\s*=\\s*(false|true)");
	private static final Pattern PATTERN_SORT_BY_VISIBILITY = Pattern.compile("@SortMembers:sortByVisibility\\s*=\\s*(false|true)");

	static SortProperties localProperties(SortProperties globalProperties, String code) {
		Optional<Boolean> localDoNotSortFields = testOverwriteProperty(PATTERN_DO_NOT_SORT_FIELDS, code);
		Optional<Boolean> localEnabled = testOverwriteProperty(PATTERN_ENABLED, code);
		Optional<Boolean> localSortByVisibility = testOverwriteProperty(PATTERN_SORT_BY_VISIBILITY, code);
		if (localDoNotSortFields.isEmpty() && localEnabled.isEmpty() && localSortByVisibility.isEmpty()) {
			return globalProperties;
		}
		boolean doNotSortFields = localDoNotSortFields.orElse(globalProperties.doNotSortFields);
		boolean enabled = localEnabled.orElse(globalProperties.enabled);
		boolean sortByVisibility = localSortByVisibility.orElse(globalProperties.sortByVisibility);
		return new SortProperties(
				enabled,
				globalProperties.membersOrder,
				doNotSortFields,
				sortByVisibility,
				globalProperties.visibilityOrder);
	}

	static String sortMember(String code, SortProperties globalProperties) {
		SortProperties localProperties = localProperties(globalProperties, code);
		if (!localProperties.enabled) {
			return code;
		}

		try {
			CompilationUnit compilationUnit = new CompilationUnit(code);
			DefaultJavaElementComparator comparator = DefaultJavaElementComparator.of(
					localProperties.doNotSortFields,
					localProperties.membersOrder,
					localProperties.sortByVisibility,
					localProperties.visibilityOrder);
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

	static Optional<Boolean> testOverwriteProperty(Pattern pattern, String code) {
		Matcher matcher = pattern.matcher(code);
		if (matcher.find()) {
			String flag = matcher.group(1);
			return Optional.of(Boolean.valueOf(flag));
		}
		return Optional.empty();
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

	@SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "the equals method shouldn't be called in the sort members use case")
	private static class CompilationUnit extends org.eclipse.jdt.internal.core.CompilationUnit {
		private final Buffer buffer;

		CompilationUnit(String code) {
			super(null, null, null);
			buffer = new Buffer(code);
		}

		@Override
		public IBuffer getBuffer() {
			return buffer;
		}

		@Override
		public JavaProject getJavaProject() {
			return JavaProject.INSTANCE;
		}

		@Override
		public Map<String, String> getOptions(boolean inheritJavaCoreOptions) {
			return Map.of();
		}

		@Override
		public ICompilationUnit getPrimary() {
			return this;
		}
	}

	private static class JavaProject extends org.eclipse.jdt.internal.core.JavaProject {
		static final JavaProject INSTANCE = new JavaProject();

		JavaProject() {
			super(null, null);
		}

		@Override
		public Map<String, String> getOptions(boolean inheritJavaCoreOptions) {
			return Map.of();
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
			boolean enabled = Boolean.parseBoolean(properties.getOrDefault("sp_cleanup.sort_members", "false"));
			String membersOrder = properties.getOrDefault("outlinesortoption", "");
			boolean doNotSortFields = !Boolean.parseBoolean(properties.getOrDefault("sp_cleanup.sort_members_all", "false"));
			boolean sortByVisibility = Boolean.parseBoolean(properties.getOrDefault("org.eclipse.jdt.ui.enable.visibility.order", "false"));
			String visibilityOrder = properties.getOrDefault("org.eclipse.jdt.ui.visibility.order", "");
			return new SortProperties(enabled, membersOrder, doNotSortFields, sortByVisibility, visibilityOrder);
		}
	}
}
