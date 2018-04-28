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
package com.diffplug.spotless.extra.eclipse.base.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.preferences.IScopeContext;

/** No content type specific handling is supported. */
public class NoContentTypeSpecificHandling implements IContentTypeManager {

	@Override
	public IContentType findContentTypeFor(InputStream contents, String fileName) throws IOException {
		return null;
	}

	@Override
	public IContentType findContentTypeFor(String fileName) {
		return null;
	}

	@Override
	public IContentType[] findContentTypesFor(InputStream contents, String fileName) throws IOException {
		return null;
	}

	@Override
	public IContentType[] findContentTypesFor(String fileName) {
		return null;
	}

	@Override
	public IContentDescription getDescriptionFor(InputStream contents, String fileName, QualifiedName[] options) throws IOException {
		return null;
	}

	@Override
	public IContentDescription getDescriptionFor(Reader contents, String fileName, QualifiedName[] options) throws IOException {
		return null;
	}

	@Override
	public void addContentTypeChangeListener(IContentTypeChangeListener listener) {}

	@Override
	public IContentType[] getAllContentTypes() {
		return null;
	}

	@Override
	public IContentType getContentType(String contentTypeIdentifier) {
		return null;
	}

	@Override
	public IContentTypeMatcher getMatcher(ISelectionPolicy customPolicy, IScopeContext context) {
		return null;
	}

	@Override
	public void removeContentTypeChangeListener(IContentTypeChangeListener listener) {

	}

	@Override
	public IContentType addContentType(String contentTypeIdentifier, String name, IContentType baseType)
			throws CoreException {
		return null;
	}

	@Override
	public void removeContentType(String contentTypeIdentifier) throws CoreException {}

}
