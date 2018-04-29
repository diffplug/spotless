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
package com.diffplug.spotless.extra.eclipse.wtp.sse;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.wst.css.core.internal.provisional.contenttype.ContentTypeIdForCSS;
import org.eclipse.wst.html.core.internal.provisional.contenttype.ContentTypeIdForHTML;
import org.eclipse.wst.json.core.contenttype.ContentTypeIdForJSON;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;

import com.diffplug.spotless.extra.eclipse.base.service.NoContentTypeSpecificHandling;

/**
 * For some embedded formatters, the WTP uses the content type ID for
 * preferences lookup.
 * <p>
 * The preference lookup is accomplished via the Eclipse preference service,
 * which must be provided in combination with this service.
 * For cleanup tasks, the ID mapping is also used by the model handler
 * to determine the model which a string stream requires.
 * </p>
 * @see org.eclipse.wst.sse.core.internal.modelhandler.ModelHandlerRegistry
 */
class ContentTypeManager extends NoContentTypeSpecificHandling {
	private final Map<String, IContentType> id2Object;
	private final IContentType processorStepType;

	/**
	 * Content type manager as required for cleanup steps.
	 * Only the
	 * @param formatterContentTypeID The content type of the formatter step
	 */
	ContentTypeManager(CleanupStep.ProcessorAccessor processor) {
		id2Object = new HashMap<String, IContentType>();
		Arrays.asList(
				ContentTypeIdForCSS.ContentTypeID_CSS,
				ContentTypeIdForXML.ContentTypeID_XML,
				ContentTypeIdForHTML.ContentTypeID_HTML,
				ContentTypeIdForJSON.ContentTypeID_JSON)
				.stream().forEach(id -> id2Object.put(id, new ContentTypeId(id)));
		processorStepType = id2Object.get(processor.getThisContentType());
		if (null == processorStepType) {
			throw new IllegalArgumentException(
					String.format(
							"The manager does not support content type '%s' of processor '%s'.",
							processor.getThisContentType(), processor.getClass().getName()));
		}
	}

	@Override
	public IContentType getContentType(String contentTypeIdentifier) {
		/*
		 *  It is OK to return null here since the manager is only used as an additional
		 *  helper to alter default behavior.
		 */
		return id2Object.get(contentTypeIdentifier);
	}

	@Override
	public IContentType findContentTypeFor(InputStream contents, String fileName) throws IOException {
		//We only format things here with the given processor, so this answer is always correct.
		return processorStepType;
	}

	/**
	 * The WTP uses the manager only for ID mapping, so most of the methods are not used.
	 * Actually it has a hand stitched way for transforming the content type ID
	 * 'org.eclipse.wst...source' to the plugin ID 'org.eclipse.wst...core'.
	 * @see org.eclipse.wst.sse.core.internal.encoding.ContentBasedPreferenceGateway
	 */
	private static class ContentTypeId implements IContentType {

		private final String id;

		ContentTypeId(String id) {
			this.id = id;
		}

		@Override
		public void addFileSpec(String fileSpec, int type) throws CoreException {}

		@Override
		public void removeFileSpec(String fileSpec, int type) throws CoreException {}

		@Override
		public void setDefaultCharset(String userCharset) throws CoreException {}

		@Override
		public boolean isUserDefined() {
			return false;
		}

		@Override
		public IContentType getBaseType() {
			return null;
		}

		@Override
		public IContentDescription getDefaultDescription() {
			return null;
		}

		@Override
		public IContentDescription getDescriptionFor(InputStream contents, QualifiedName[] options) throws IOException {
			return null;
		}

		@Override
		public IContentDescription getDescriptionFor(Reader contents, QualifiedName[] options) throws IOException {
			return null;
		}

		@Override
		public String getDefaultCharset() {
			return null;
		}

		@Override
		public String[] getFileSpecs(int type) {
			return null;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getName() {
			return id;
		}

		@Override
		public boolean isAssociatedWith(String fileName) {
			return false;
		}

		@Override
		public boolean isAssociatedWith(String fileName, IScopeContext context) {
			return false;
		}

		@Override
		public boolean isKindOf(IContentType another) {
			if (null == another) {
				return false;
			}
			return this.id.equals(another.getId());
		}

		@Override
		public IContentTypeSettings getSettings(IScopeContext context) throws CoreException {
			return null;
		}

	}

}
