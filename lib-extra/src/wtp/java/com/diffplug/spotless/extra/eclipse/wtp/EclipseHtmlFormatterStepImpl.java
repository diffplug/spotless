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
package com.diffplug.spotless.extra.eclipse.wtp;

import java.util.Properties;

import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.cleanup.HTMLCleanupProcessorImpl;
import org.eclipse.wst.html.core.internal.encoding.HTMLDocumentLoader;
import org.eclipse.wst.html.core.internal.format.HTMLFormatProcessorImpl;
import org.eclipse.wst.html.core.text.IHTMLPartitions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.sse.core.internal.cleanup.AbstractStructuredCleanupProcessor;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import com.diffplug.spotless.extra.eclipse.wtp.html.JsRegionProcessor;
import com.diffplug.spotless.extra.eclipse.wtp.html.StructuredDocumentProcessor;
import com.diffplug.spotless.extra.eclipse.wtp.sse.CleanupStep;
import com.diffplug.spotless.extra.eclipse.wtp.sse.PluginPreferences;

/** Formatter step which calls out to the Eclipse HTML cleanup and formatter. */
public class EclipseHtmlFormatterStepImpl extends CleanupStep {
	static {
		SolsticeSetup.init();
	}

	private final String htmlFormatterIndent;
	private final CodeFormatter jsFormatter;

	public EclipseHtmlFormatterStepImpl(Properties properties) throws Exception {
		super(new CleanupProcessor());
		PluginPreferences.assertNoChanges(HTMLCorePlugin.getDefault(), properties);
		htmlFormatterIndent = ((CleanupProcessor) processorAccessor).getIndent();
		jsFormatter = ToolFactory.createCodeFormatter(JavaScriptCore.getOptions(), ToolFactory.M_FORMAT_EXISTING);
	}

	@Override
	public String format(String raw) throws Exception {
		raw = super.format(raw);

		// Not sure how Eclipse binds the JS formatter to HTML. The formatting is accomplished manually instead.
		IStructuredDocument document = (IStructuredDocument) new HTMLDocumentLoader().createNewStructuredDocument();
		document.setPreferredLineDelimiter("\n");
		document.set(raw);
		StructuredDocumentProcessor<CodeFormatter> jsProcessor = new StructuredDocumentProcessor<CodeFormatter>(
				document, IHTMLPartitions.SCRIPT, JsRegionProcessor.createFactory(htmlFormatterIndent));
		jsProcessor.apply(jsFormatter);

		return document.get();
	}

	/**
	 * * The WTP {@link HTMLFormatProcessorImpl} does not allow a strict case formatting.
	 * Hence additionally the {@link HTMLCleanupProcessorImpl} is used.
	 * <p>
	 * Note that a preferences like {@code TAG_NAME_CASE} are not used by the
	 * formatter, though configurable in the formatters preference GUI.
	 * The user must instead configure for example {@code CLEANUP_TAG_NAME_CASE}
	 * in the cleanup GUI.
	 * </p>
	 */
	private static class CleanupProcessor extends HTMLCleanupProcessorImpl implements CleanupStep.ProcessorAccessor {
		private HTMLFormatProcessorImpl processor;

		CleanupProcessor() {
			processor = new HTMLFormatProcessorImpl();
		}

		@Override
		public String getTypeId() {
			return getContentType();
		}

		@Override
		public void refreshPreferences() {
			refreshCleanupPreferences();
			processor = new HTMLFormatProcessorImpl(); //Constructor reads new preferences
			processor.refreshFormatPreferences = false; //Don't refresh when cloning
		}

		@Override
		public AbstractStructuredCleanupProcessor get() {
			return this;
		}

		@Override
		protected IStructuredFormatProcessor getFormatProcessor() {
			return processor;
		}

		String getIndent() {
			return processor.getFormatPreferences().getIndent();
		}
	}
}
