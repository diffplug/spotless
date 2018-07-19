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

import java.util.Properties;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;

/** Formatter step which calls out to the Eclipse JDT formatter. */
public class EclipseJdtFormatterStepImpl {

	private final CodeFormatter codeFormatter;

	public EclipseJdtFormatterStepImpl(Properties settings) throws Exception {
		SpotlessEclipseFramework.setup(
				plugins -> {
					plugins.applyDefault();
					plugins.add(new JavaCore());
				});
		this.codeFormatter = ToolFactory.createCodeFormatter(settings, ToolFactory.M_FORMAT_EXISTING);
	}

	public String format(String raw) throws Exception {
		TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS, raw, 0, raw.length(), 0, SpotlessEclipseFramework.LINE_DELIMITER);
		if (edit == null) {
			throw new IllegalArgumentException("Invalid java syntax for formatting.");
		} else {
			IDocument doc = new Document(raw);
			edit.apply(doc);
			return doc.get();
		}
	}

}
