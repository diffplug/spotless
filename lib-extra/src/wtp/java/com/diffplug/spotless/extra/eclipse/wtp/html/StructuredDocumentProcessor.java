/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.extra.eclipse.wtp.html;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

/**
 * The HTML formatter uses for different regions of the structured document,
 * other formatters, explicitly for CSS and JS.
 * Adaptations of the current formatter modifications are tedious,
 * since the adaptations e.g. overlap with the required additional modifications.
 * Hence the modifications is split in steps, whereas the regions of the
 * structured documents and their offsets are regenerate between the first
 * ad second step.
 */
public class StructuredDocumentProcessor<T> {
	private final String type;
	private final BiFunction<IStructuredDocument, ITypedRegion, ? extends RegionProcessor<T>> factory;
	private final IStructuredDocument document;
	private final int numberOfRegions;

	/**
	 * Constructs a document processor
	 * @param document Document to be processed
	 * @param type Document type ID recognized by {@code IContentTypeManager} service
	 * @param factory Factory for structured document processor
	 */
	public StructuredDocumentProcessor(IStructuredDocument document, String type,
			BiFunction<IStructuredDocument, ITypedRegion, ? extends RegionProcessor<T>> factory) {
		this.type = type;
		this.factory = factory;
		this.document = document;
		numberOfRegions = getRegions().size();
	}

	/** Applies processor on document, using a given formatter */
	public void apply(T formatter) {
		for (int currentRegionId = 0; currentRegionId < numberOfRegions; currentRegionId++) {
			applyOnRegion(currentRegionId, formatter);
		}
	}

	private List<ITypedRegion> getRegions() {
		try {
			return Arrays.asList(document.computePartitioning(0, document.getLength())).stream().filter(reg -> type == reg.getType()).collect(Collectors.toList());
		} catch (BadLocationException e) {
			/*
			 * This prevents the processing in case the entire document
			 * cannot be processed (e.g. the document is incomplete).
			 */
			return new ArrayList<ITypedRegion>(0);
		}
	}

	private void applyOnRegion(int number, T formatter) {
		RegionProcessor<T> adapter = getRegionProcessor(number);
		try {
			adapter.applyFirst(formatter);
			adapter = getRegionProcessor(number);
			adapter.applySecond(formatter);
		} catch (MalformedTreeException | BadLocationException e) {
			throw new IllegalArgumentException(
					String.format("%s formatting failed between lines %d and %d. Most likely the syntax is not recognized.",
							type, adapter.getFirstLine(), adapter.getLastLine()),
					e);
		}
	}

	private RegionProcessor<T> getRegionProcessor(int number) {
		List<ITypedRegion> regions = getRegions();
		if (numberOfRegions != regions.size()) {
			//Don't catch this. This is a severe internal bug!
			throw new IllegalArgumentException(
					String.format(
							"During first '%s' formatting step, the number of detected regions changed from '%d' to '%d'",
							type, numberOfRegions, regions.size()));
		}
		ITypedRegion region = regions.get(number);
		return factory.apply(document, region);
	}

	/** Base class for region adaptations. */
	public static abstract class RegionProcessor<T> {
		protected final IStructuredDocument document;
		protected final ITypedRegion region;
		protected final int indentationLevel;
		protected final int firstLine;
		protected final int lastLine;

		protected RegionProcessor(IStructuredDocument document, ITypedRegion region, String htmlIndent) {
			this.document = document;
			this.region = region;
			indentationLevel = computeIndent(document, region, htmlIndent);
			firstLine = document.getLineOfOffset(region.getOffset());
			lastLine = document.getLineOfOffset(region.getOffset() + region.getLength());
		}

		public int getFirstLine() {
			return firstLine;
		}

		public int getLastLine() {
			return lastLine;
		}

		private static int computeIndent(IStructuredDocument document, ITypedRegion region, String htmlIndent) {
			int indent = 0;
			try {
				int lineNumber = document.getLineOfOffset(region.getOffset());
				document.getNumberOfLines();
				int lineOffset = document.getLineOffset(lineNumber);
				String lineStart = document.get(lineOffset, region.getOffset() - lineOffset);
				while (lineStart.length() > htmlIndent.length()) {
					if (lineStart.startsWith(htmlIndent)) {
						indent++;
					} else {
						break;
					}
					lineStart = lineStart.substring(htmlIndent.length());
				}
			} catch (BadLocationException e) {
				/*
				 * Skip addition indentation. This normally indicates a malformed HTML
				 * outside of this region, which cannot be handled here.
				 */
				indent = 0;
			}
			return indent;
		}

		/** Add delimiter at or after given offset, if there is none at the offset position. Returns the number of characters inserted. */
		protected int fixDelimiter(MultiTextEdit modifications, int offset, boolean addAfter) throws BadLocationException {
			int delimiterLength = "\n".length();
			String delimiter = document.get(offset, delimiterLength);
			if (!"\n".equals(delimiter)) {
				if (addAfter) {
					offset += 1;
				}
				modifications.addChild(new InsertEdit(offset, "\n"));
				return "\n".length();
			}
			return 0;
		}

		/** Fix the tag indentation at a given position with a predefined indentation. */
		protected void fixTagIndent(MultiTextEdit modifications, int offset, String indentString) throws BadLocationException {
			int lineNumber = document.getLineOfOffset(offset);
			if (lineNumber >= document.getNumberOfLines()) {
				//Nothing to change for last line. If syntax is correct, there is no indentation. If syntax is not correct, there is nothing to do.
				return;
			}
			int lineStart = document.getLineOffset(lineNumber);
			int lineEnd = document.getLineOffset(lineNumber + 1);
			String lineContent = document.get(lineStart, lineEnd - lineStart);
			StringBuilder currentIndent = new StringBuilder();
			lineContent.chars().filter(c -> {
				if (c == ' ' || c == '\t') {
					currentIndent.append(c);
					return false;
				}
				return true;
			}).findFirst();
			if (!indentString.equals(currentIndent.toString())) {
				TextEdit replaceIndent = new ReplaceEdit(lineStart, currentIndent.length(), indentString);
				replaceIndent.apply(document);
			}
		}

		/** First application of modifications */
		abstract protected void applyFirst(T formatter) throws MalformedTreeException, BadLocationException;

		/** Second application of modifications (based on new regions) */
		abstract protected void applySecond(T formatter) throws MalformedTreeException, BadLocationException;

	}

}
