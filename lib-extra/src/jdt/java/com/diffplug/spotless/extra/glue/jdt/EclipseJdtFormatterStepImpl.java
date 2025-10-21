/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless.extra.glue.jdt;

import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

/** Formatter step which calls out to the Eclipse JDT formatter. */
public class EclipseJdtFormatterStepImpl {
	/** Spotless demands for internal formatter chains Unix (LF) line endings. */
	public static final String LINE_DELIMITER = "\n";

	private final CodeFormatter codeFormatter;
	private final EclipseJdtSortMembers.SortProperties sortProperties;

	public EclipseJdtFormatterStepImpl(Properties formatterSettings, Map<String, String> sortProperties) {
		Map<String, String> options = formatterSettings.entrySet().stream().collect(toMap(
				e -> String.valueOf(e.getKey()),
				e -> String.valueOf(e.getValue()),
				(prev, next) -> next,
				HashMap::new));
		this.codeFormatter = new DefaultCodeFormatter(options);
		this.sortProperties = EclipseJdtSortMembers.SortProperties.from(sortProperties);
	}

	/** Formatting Java string, distinguishing module-info and compilation unit by file name */
	public String format(String raw, File file) throws Exception {
		raw = sort(raw);
		int kind = (file.getName().equals(IModule.MODULE_INFO_JAVA) ? CodeFormatter.K_MODULE_INFO
				: CodeFormatter.K_COMPILATION_UNIT) | CodeFormatter.F_INCLUDE_COMMENTS;
		TextEdit edit = codeFormatter.format(kind, raw, 0, raw.length(), 0, LINE_DELIMITER);
		if (edit == null) {
			throw new IllegalArgumentException("Invalid java syntax for formatting.");
		} else {
			IDocument doc = new Document(raw);
			edit.apply(doc);
			return doc.get();
		}
	}

	/** Sort members in Java string */
	public String sort(String raw) {
		return EclipseJdtSortMembers.sortMember(raw, sortProperties);
	}
}
