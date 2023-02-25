/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.extra.eclipse.cdt;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseConfig;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipsePluginConfig;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseServiceConfig;

/** Formatter step which calls out to the Eclipse CDT formatter. */
public class EclipseCdtFormatterStepImpl {
	private final CodeFormatter codeFormatter;

	public EclipseCdtFormatterStepImpl(Properties settings) throws Exception {
		SpotlessEclipseFramework.setup(new FrameworkConfig());
		Stream<Entry<Object, Object>> stream = settings.entrySet().stream();
		Map<String, String> settingsMap = stream.collect(Collectors.toMap(
				e -> String.valueOf(e.getKey()),
				e -> String.valueOf(e.getValue())));
		codeFormatter = org.eclipse.cdt.core.ToolFactory.createDefaultCodeFormatter(settingsMap);
	}

	private static class FrameworkConfig implements SpotlessEclipseConfig {
		@Override
		public void registerServices(SpotlessEclipseServiceConfig config) {
			config.applyDefault();
			config.useSlf4J(EclipseCdtFormatterStepImpl.class.getPackage().getName());
		}

		@Override
		public void activatePlugins(SpotlessEclipsePluginConfig config) {
			config.applyDefault();
			config.add(new CCorePlugin());
		}
	}

	/** Formatting C/C++ string */
	public String format(String raw) throws Exception {
		//The 'kind' can be set to CodeFormatter.K_UNKNOWN, since it is anyway ignored by the internal formatter
		TextEdit edit = codeFormatter.format(CodeFormatter.K_UNKNOWN, raw, 0, raw.length(), 0, SpotlessEclipseFramework.LINE_DELIMITER);
		if (edit == null) {
			throw new IllegalArgumentException("Invalid C/C++ syntax for formatting.");
		} else {
			IDocument doc = new Document(raw);
			edit.apply(doc);
			return doc.get();
		}
	}
}
