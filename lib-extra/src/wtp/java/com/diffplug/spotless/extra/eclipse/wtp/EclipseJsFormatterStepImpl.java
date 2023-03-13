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

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.wst.jsdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.wst.jsdt.internal.ui.text.comment.CommentFormattingStrategy;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;

import com.diffplug.spotless.extra.eclipse.wtp.sse.PluginPreferences;

/** Formatter step which calls out to the Eclipse JS formatter. */
public class EclipseJsFormatterStepImpl {
	static {
		SolsticeSetup.init();
	}

	private final static String[] COMMENT_TYPES = {
			IJavaScriptPartitions.JAVA_DOC,
			IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT,
			IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT,
			IJavaScriptPartitions.JAVA_STRING,
			IJavaScriptPartitions.JAVA_CHARACTER
	};

	private final static Map<String, String> OPTION_2_COMMENT_TYPE = Collections.unmodifiableMap(Stream.of(
			new SimpleEntry<>(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT, IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT),
			new SimpleEntry<>(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT, IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT),
			new SimpleEntry<>(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, IJavaScriptPartitions.JAVA_DOC)).collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

	private final CodeFormatter formatter;
	private final Hashtable<?, ?> options;
	private final Set<String> commentTypesToBeFormatted;

	public EclipseJsFormatterStepImpl(Properties properties) throws Exception {
		PluginPreferences.store(JavaScriptCore.getPlugin(), properties);
		options = JavaScriptCore.getDefaultOptions();
		commentTypesToBeFormatted = OPTION_2_COMMENT_TYPE.entrySet().stream().filter(x -> DefaultCodeFormatterConstants.TRUE.equals(options.get(x.getKey()))).map(x -> x.getValue()).collect(Collectors.toSet());
		formatter = ToolFactory.createCodeFormatter(options, ToolFactory.M_FORMAT_EXISTING);
	}

	/** Formatting JavaScript string */
	public String format(String raw) throws Exception {
		raw = formatComments(raw);
		// The comment formatter messed up the code a little bit (adding some line breaks). Now we format the code.
		IDocument doc = new Document(raw);
		TextEdit edit = formatter.format(CodeFormatter.K_JAVASCRIPT_UNIT, raw, 0, raw.length(), 0, "\n");
		if (edit == null) {
			throw new IllegalArgumentException("Invalid JavaScript syntax for formatting.");
		} else {
			edit.apply(doc);
		}
		return doc.get();
	}

	/**
	 * Comment formats like it would be accomplished by the JDTS UI, without setting up the UI.
	 * @see org.eclipse.wst.jsdt.internal.ui.fix.CommentFormatFix
	 */
	private String formatComments(String raw) {
		Document doc = new Document(raw);
		IDocumentPartitioner commentPartitioner = new FastPartitioner(new FastJavaPartitionScanner(), COMMENT_TYPES);
		doc.setDocumentPartitioner(IJavaScriptPartitions.JAVA_PARTITIONING, commentPartitioner);
		commentPartitioner.connect(doc);
		CommentFormattingStrategy commentFormatter = new CommentFormattingStrategy();
		IFormattingContext context = new CommentFormattingContext();
		context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, options);
		context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.TRUE);
		context.setProperty(FormattingContextProperties.CONTEXT_MEDIUM, doc);
		try {
			ITypedRegion[] regions = TextUtilities.computePartitioning(doc, IJavaScriptPartitions.JAVA_PARTITIONING, 0, doc.getLength(), false);
			MultiTextEdit resultEdit = new MultiTextEdit();
			Arrays.asList(regions).stream().filter(reg -> commentTypesToBeFormatted.contains(reg.getType())).forEach(region -> {
				TypedPosition typedPosition = new TypedPosition(region.getOffset(), region.getLength(), region.getType());
				context.setProperty(FormattingContextProperties.CONTEXT_PARTITION, typedPosition);
				commentFormatter.formatterStarts(context);
				TextEdit edit = commentFormatter.calculateTextEdit();
				commentFormatter.formatterStops();
				if (null != edit && edit.hasChildren()) {
					resultEdit.addChild(edit);
				}
			});
			resultEdit.apply(doc);
			return doc.get();
		} catch (BadLocationException e) {
			//Silently ignore comment formatting exceptions and return the original string
			return raw;
		}
	}
}
