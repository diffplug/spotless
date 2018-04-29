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
package com.diffplug.spotless.extra.eclipse.wtp;

import static com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.LINE_DELIMITER;
import static org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames.*;
import static org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames.CMDOCUMENT_GLOBAL_CACHE_ENABLED;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverPlugin;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceInitializer;
import org.eclipse.wst.dtd.core.internal.DTDCorePlugin;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.cleanup.HTMLCleanupProcessorImpl;
import org.eclipse.wst.html.core.internal.encoding.HTMLDocumentLoader;
import org.eclipse.wst.html.core.internal.format.HTMLFormatProcessorImpl;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceInitializer;
import org.eclipse.wst.html.core.text.IHTMLPartitions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceInitializer;

import com.diffplug.spotless.extra.eclipse.wtp.html.JsRegionProcessor;
import com.diffplug.spotless.extra.eclipse.wtp.html.StructuredDocumentProcessor;
import com.diffplug.spotless.extra.eclipse.wtp.sse.CleanupStep;
import com.diffplug.spotless.extra.eclipse.wtp.sse.SpotlessPreferences;

/** Formatter step which calls out to the Eclipse HTML formatter and cleanup. */
public class EclipseHtmlFormatterStepImpl extends CleanupStep<EclipseHtmlFormatterStepImpl.SpotlessHtmlCleanup> {

	private final String htmlFormatterIndent;
	private final CodeFormatter jsFormatter;

	public EclipseHtmlFormatterStepImpl(Properties properties) throws Exception {
		super(new SpotlessHtmlCleanup(), additionalPlugins -> {
			additionalPlugins.add(new CSSCorePlugin());
			additionalPlugins.add(new XMLCorePlugin());
			//DTDs must be resolved by URI
			additionalPlugins.add(new URIResolverPlugin());
			//Support parsing of the DTD (required, though only the internal EMF models are used)
			additionalPlugins.add(new DTDCorePlugin());
			// The JS core uses EFS for determination of temporary storage location
			additionalPlugins.add(new org.eclipse.core.internal.filesystem.Activator());
			additionalPlugins.add(new JavaScriptCore());
			additionalPlugins.add(new HTMLCorePlugin());
		});
		/*
		 * The cleanup processor tries to load DTDs into the cache (which we have not setup).
		 * Anyhow, the attempt is bogus since it anyway just silently fails to read the internal DTDs.
		 * So we forbid to use the cache in the first place.
		 */
		properties.setProperty(CMDOCUMENT_GLOBAL_CACHE_ENABLED, Boolean.toString(false));
		configure(getCSSFormattingProperties(properties), true, CSSCorePlugin.getDefault(), new CSSCorePreferenceInitializer());
		configure(properties, false, XMLCorePlugin.getDefault(), new XMLCorePreferenceInitializer());
		configure(properties, false, HTMLCorePlugin.getDefault(), new HTMLCorePreferenceInitializer());
		htmlFormatterIndent = processor.getIndent();

		//Create JS formatter
		Map<Object, Object> jsOptions = EclipseJsFormatterStepImpl.createFormatterOptions(properties);
		jsFormatter = ToolFactory.createCodeFormatter(jsOptions, ToolFactory.M_FORMAT_EXISTING);
		SpotlessPreferences.configurePluginPreferences(CSSCorePlugin.getDefault(), properties);
	}

	@Override
	public String format(String raw) throws Exception {
		raw = super.format(raw);

		// Not sure how Eclipse binds the JS formatter to HTML. The formatting is accomplished manually instead.
		IStructuredDocument document = (IStructuredDocument) new HTMLDocumentLoader().createNewStructuredDocument();
		document.setPreferredLineDelimiter(LINE_DELIMITER);
		document.set(raw);
		StructuredDocumentProcessor<CodeFormatter> jsProcessor = new StructuredDocumentProcessor<CodeFormatter>(
				document, IHTMLPartitions.SCRIPT, JsRegionProcessor.createFactory(htmlFormatterIndent));
		jsProcessor.apply(jsFormatter);

		return document.get();
	}

	/**
	 * * The HTMLFormatProcessorImpl does not allow a strict case formatting.
	 * Hence additionally the HTMLCleanupProcessorImpl is used.
	 * <p>
	 * Note that a preferences like TAG_NAME_CASE are not used by the
	 * formatter, though configurable in the formatters preference GUI.
	 * The user must instead configure for example CLEANUP_TAG_NAME_CASE
	 * in the cleanup GUI.
	 * </p>
	 */
	public static class SpotlessHtmlCleanup extends HTMLCleanupProcessorImpl implements CleanupStep.ProcessorAccessor {
		private HTMLFormatProcessorImpl processor = null;

		@Override
		public String getThisContentType() {
			return getContentType();
		}

		@Override
		public IStructuredFormatProcessor getThisFormatProcessor() {
			return getFormatProcessor();
		}

		@Override
		public void refreshThisCleanupPreferences() {
			refreshCleanupPreferences();
		}

		@Override
		protected IStructuredFormatProcessor getFormatProcessor() {
			if (null == processor) {
				processor = new HTMLFormatProcessorImpl();
			}
			return processor;
		}

		String getIndent() {
			/*
			 *  The processor must not be null,
			 *  otherwise it has not been configured yet,
			 *  and the result would be incorrect.
			 */
			return processor.getFormatPreferences().getIndent();
		}

	}

	private final static Set<String> SUPPORTED_CSS_FORMAT_PREFS = new HashSet<String>(Arrays.asList(
			CASE_IDENTIFIER,
			CASE_SELECTOR,
			CASE_PROPERTY_NAME,
			CASE_PROPERTY_VALUE,
			FORMAT_BETWEEN_VALUE,
			FORMAT_PROP_POST_DELIM,
			FORMAT_PROP_PRE_DELIM,
			FORMAT_QUOTE,
			FORMAT_QUOTE_IN_URI,
			FORMAT_SPACE_BETWEEN_SELECTORS,
			WRAPPING_NEWLINE_ON_OPEN_BRACE,
			WRAPPING_ONE_PER_LINE,
			WRAPPING_PROHIBIT_WRAP_ON_ATTR,
			LINE_WIDTH,
			INDENTATION_CHAR,
			INDENTATION_SIZE,
			QUOTE_ATTR_VALUES,
			CLEAR_ALL_BLANK_LINES));

	private static Properties getCSSFormattingProperties(final Properties properties) {
		Properties filteredProperties = new Properties();
		properties.entrySet().stream().filter(
				entry -> SUPPORTED_CSS_FORMAT_PREFS.contains(entry.getKey())).forEach(entry -> filteredProperties.put(entry.getKey(), entry.getValue()));
		return filteredProperties;
	}
}
