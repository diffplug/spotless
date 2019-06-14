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
package com.diffplug.spotless.extra.eclipse.java;

import org.eclipse.core.internal.content.ContentType;
import org.eclipse.core.internal.content.ContentTypeCatalog;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;

import com.diffplug.spotless.extra.eclipse.base.service.NoContentTypeSpecificHandling;

/**
 * Java compilation unit validation requires Java content type to be recognized.
 * All source is assumed to be Java. A content description is not required/provided.
 * <p>
 * See {@code org.eclipse.jdt.internal.core.util.Util} for details.
 * </p>
 */
public class JavaContentTypeManager extends NoContentTypeSpecificHandling {

	private final IContentType contentType;

	public JavaContentTypeManager() {
		contentType = ContentType.createContentType(
				new ContentTypeCatalog(null, 0),
				"",
				"",
				(byte) 0,
				new String[]{SuffixConstants.EXTENSION_java, SuffixConstants.EXTENSION_JAVA},
				new String[0],
				new String[0],
				"",
				"",
				null,
				null);
	}

	@Override
	public IContentType getContentType(String contentTypeIdentifier) {
		return contentType;
	}

	@Override
	public IContentType[] getAllContentTypes() {
		return new IContentType[]{contentType};
	}

	@Override
	public IContentTypeMatcher getMatcher(ISelectionPolicy customPolicy, IScopeContext context) {
		return this;
	}
}
