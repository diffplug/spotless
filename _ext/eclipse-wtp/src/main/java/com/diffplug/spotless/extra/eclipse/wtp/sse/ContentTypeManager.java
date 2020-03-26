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

 * WTP ModelHandlerRegistry uses the content type mamanger clean-up formatters
 * to provide association of content to content type related functionality.
 * <p>
 * Preference lookup per content type is accomplished via the
 * Eclipse PreferencesService, which must be provided in combination with
 * this service.
 * </p>
 * The input byte steam encoding detection is accomplished by the
 * content type manager. Normally the encoding is bount do a document/file.
 * Spotless applies the formatting on strings already decoded.
 * The WTP AbstractStructuredCleanupProcessor provides for non-documents
 * a clean-up function converting the decoded string into an UTF-8 encoded byte stream.
 * WTP AbstractDocumentLoader uses content type mamanger to determine the encoding
 * of the input stream.
 * Only the steps are affected that are using the
 * AbstractStructuredCleanupProcessor. All other steps creating an empty document
 * (e.g. via WTP AbstractDocumentLoader) and setting the textual content of the new document.
 *
 * @see org.eclipse.core.internal.preferences.PreferencesService
 * @see org.eclipse.wst.sse.core.internal.cleanup.AbstractStructuredCleanupProcessor
 * @see org.eclipse.wst.sse.core.internal.document.AbstractDocumentLoader
 * @see org.eclipse.wst.sse.core.internal.modelhandler.ModelHandlerRegistry
 */
class ContentTypeManager extends NoContentTypeSpecificHandling {
	private final Map<String, IContentType> id2Object;
	private final IContentType processorStepType;
	private final IContentDescription processorStepDescription;

	/**
	 * Content type manager as required for cleanup steps.
	 * @param formatterContentTypeID The content type of the formatter step
	 */
	ContentTypeManager(String formatterContentTypeID) {
		id2Object = new HashMap<String, IContentType>();
		Arrays.asList(
				ContentTypeIdForCSS.ContentTypeID_CSS,
				ContentTypeIdForXML.ContentTypeID_XML,
				ContentTypeIdForHTML.ContentTypeID_HTML,
				ContentTypeIdForJSON.ContentTypeID_JSON)
				.stream().forEach(id -> id2Object.put(id, new ContentTypeId(id)));
		processorStepType = id2Object.get(formatterContentTypeID);
		if (null == processorStepType) {
			throw new IllegalArgumentException("The manager does not support content type " + formatterContentTypeID);
		}
		processorStepDescription = new StringDescription(processorStepType);
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

	@Override
	public IContentDescription getDescriptionFor(InputStream contents, String fileName, QualifiedName[] options) throws IOException {
		return processorStepDescription;
	}

	private static class StringDescription implements IContentDescription {

		private final IContentType type;

		public StringDescription(IContentType type) {
			this.type = type;
		}

		@Override
		public boolean isRequested(QualifiedName key) {
			return false; //Don't use set Property
		}

		@Override
		public String getCharset() {
			//Called by AbstractDocumentLoader.readInputStream
			return "UTF-8"; //UTF-8 encoded by AbstractStructuredCleanupProcessor.cleanupContent
		}

		@Override
		public IContentType getContentType() {
			return type;
		}

		@Override
		public Object getProperty(QualifiedName key) {
			return null; //Assume that the property map is empty
		}

		@Override
		public void setProperty(QualifiedName key, Object value) {
			throw new IllegalArgumentException("Content description key cannot be set: " + key);
		}
	}

	/**
	 * The WTP uses the manager mainly for ID mapping, so most of the methods are not used.
	 * Actually it has a hand stitched way for transforming the content type ID
	 * {@code org.eclipse.wst...source} to the plugin ID {@code org.eclipse.wst...core}.
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
