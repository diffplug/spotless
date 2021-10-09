/*
 * Copyright 2016-2021 DiffPlug
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

import java.io.File;
import java.util.Properties;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseConfig;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipsePluginConfig;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseServiceConfig;

/** Formatter step which calls out to the Eclipse JDT formatter. */
public class EclipseJdtFormatterStepImpl {

	private final CodeFormatter codeFormatter;

	public EclipseJdtFormatterStepImpl(Properties settings) throws Exception {
		SpotlessEclipseFramework.setup(new FrameworkConfig());
		this.codeFormatter = ToolFactory.createCodeFormatter(settings, ToolFactory.M_FORMAT_EXISTING);
	}

	private static class FrameworkConfig implements SpotlessEclipseConfig {
		@Override
		public void registerServices(SpotlessEclipseServiceConfig config) {
			config.applyDefault();
			config.useSlf4J(EclipseJdtFormatterStepImpl.class.getPackage().getName());
		}

		@Override
		public void activatePlugins(SpotlessEclipsePluginConfig config) {
			config.applyDefault();
			config.add(new JavaCore());
		}
	}

	/** @deprecated  Use {@link #format(String, File)} instead. */
	@Deprecated
	public String format(String raw) throws Exception {
		return format(raw, new File(""));
	}

	/** Formatting Java string, distinguishing module-info and compilation unit by file name */
	public String format(String raw, File file) throws Exception {
		int kind = (file.getName().equals(IModule.MODULE_INFO_JAVA) ? CodeFormatter.K_MODULE_INFO
				: CodeFormatter.K_COMPILATION_UNIT) | CodeFormatter.F_INCLUDE_COMMENTS;

		TextEdit edit = codeFormatter.format(kind, raw, 0, raw.length(), 0, SpotlessEclipseFramework.LINE_DELIMITER);
		if (edit == null) {
			throw new IllegalArgumentException("Invalid java syntax for formatting.");
		} else {
			IDocument doc = new Document(raw);
			edit.apply(doc);
			return doc.get();
		}
	}

}
