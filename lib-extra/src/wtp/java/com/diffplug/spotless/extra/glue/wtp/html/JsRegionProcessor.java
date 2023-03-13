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
package com.diffplug.spotless.extra.glue.wtp.html;

import java.util.function.BiFunction;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

/**
 * Provides additional formating to the plain JS {@link CodeFormatter}:
 * <ul>
 *  <li> Eclipse HTML places the embedded JS in separated lines by adding a line break after/before &lt;script/&gt; tag. </li>
 *  <li> Eclipse HTML treats the text before the closing &lt;/script&gt; tag as part of the script region.</li>
 * </ul>
 * <p>
 * Note that the closing tag is indented by Eclipse using the embedded formatters indentation,
 * whereas the opening tag indentation is configured by the HTML preferences.
 * This is more a bug than a feature, but Spotless formatter output shall be identical
 * to the one of Eclipse.
 * </p>
 */
public class JsRegionProcessor extends StructuredDocumentProcessor.RegionProcessor<CodeFormatter> {
	public JsRegionProcessor(IStructuredDocument document, ITypedRegion scriptRegion, String htmlIndent) {
		super(document, scriptRegion, htmlIndent);
	}

	@Override
	protected void applyFirst(CodeFormatter formatter) throws MalformedTreeException, BadLocationException {
		MultiTextEdit modifications = new MultiTextEdit();
		String jsSource = document.get(region.getOffset(), region.getLength());
		TextEdit jsEdit = formatter.format(CodeFormatter.K_JAVASCRIPT_UNIT, jsSource, 0, jsSource.length(), indentationLevel + 1, "\n");
		if (null != jsEdit) {
			jsEdit.moveTree(region.getOffset());
			modifications.addChild(jsEdit);
		}
		modifications.apply(document);
	}

	@Override
	protected void applySecond(CodeFormatter formatter) throws MalformedTreeException, BadLocationException {
		MultiTextEdit modifications = new MultiTextEdit();
		int regionEnd = region.getOffset() + region.getLength();
		regionEnd += fixDelimiter(modifications, region.getOffset(), false);
		regionEnd += fixDelimiter(modifications, region.getOffset() + region.getLength() - 1, true);
		modifications.apply(document);
		modifications.removeChildren();
		fixTagIndent(modifications, regionEnd, formatter.createIndentationString(indentationLevel));
		modifications.apply(document);
	}

	/** Factory for {@link StructuredDocumentProcessor}*/
	public static BiFunction<IStructuredDocument, ITypedRegion, JsRegionProcessor> createFactory(String htmlIndent) {
		return new BiFunction<IStructuredDocument, ITypedRegion, JsRegionProcessor>() {

			@Override
			public JsRegionProcessor apply(IStructuredDocument document, ITypedRegion region) {
				return new JsRegionProcessor(document, region, htmlIndent);
			}

		};
	}

}
