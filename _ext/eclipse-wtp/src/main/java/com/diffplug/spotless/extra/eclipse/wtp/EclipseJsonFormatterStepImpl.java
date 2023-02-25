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

import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.json.core.JSONCorePlugin;
import org.eclipse.wst.json.core.cleanup.CleanupProcessorJSON;
import org.eclipse.wst.json.core.format.FormatProcessorJSON;
import org.eclipse.wst.json.core.internal.preferences.JSONCorePreferenceInitializer;
import org.eclipse.wst.sse.core.internal.cleanup.AbstractStructuredCleanupProcessor;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipsePluginConfig;
import com.diffplug.spotless.extra.eclipse.wtp.sse.CleanupStep;
import com.diffplug.spotless.extra.eclipse.wtp.sse.PluginPreferences;

/**
 * Formatter step which calls out to the Eclipse JSON cleanup processor and formatter.
 * Note that the cleanup is escaped, since it has known bugs and is currently not used by Eclipse.
 */
public class EclipseJsonFormatterStepImpl extends CleanupStep {

	public EclipseJsonFormatterStepImpl(Properties properties) throws Exception {
		super(new CleanupProcessor(), new FrameworkConfig(properties));
		PluginPreferences.assertNoChanges(JSONCorePlugin.getDefault(), properties);
	}

	/**
	 *  The JSON CleanUp is partly implemented.
	 *  <p>
	 *  For example the abstract formatter supports the
	 *  CASE_PROPERTY_NAME configuration item.
	 *  However, this seems to be all dead code and there seems
	 *  to be no way in the latest Eclipse GUI to configure
	 *  or trigger the clean-up process.
	 *  </p>
	 *  <p>
	 *  Here we just use the CleanupProcessorJSON to reuse the common
	 *  interface to trigger the formatting.
	 *  </p>
	 *  See {@code org.eclipse.wst.json.core.internal.format.AbstractJSONSourceFormatter} for details.
	 */
	private static class CleanupProcessor extends CleanupProcessorJSON implements CleanupStep.ProcessorAccessor {
		private final FormatProcessorJSON formatter;

		CleanupProcessor() {
			formatter = new FormatProcessorJSON();
		}

		@Override
		public String getTypeId() {
			return getContentType();
		}

		@Override
		public AbstractStructuredCleanupProcessor get() {
			return this;
		}

		@Override
		public void refreshPreferences() {
			refreshCleanupPreferences();
		}

		@Override
		public String cleanupContent(String input) throws IOException, CoreException {
			/*
			 * The CleanupProcessorJSON.cleanupContent is erroneous and disabled in IDE.
			 * Hence the clean-up itself is replaced by a format processor.
			 * The SpotlessJsonCleanup still derives from the CleanupStep base class
			 * to use the common Spotless WTP configuration.
			 *
			 * See Spotless issue #344 for details.
			 */
			return formatter.formatContent(input);
		}
	}

	private static class FrameworkConfig extends CleanupStep.FrameworkConfig {
		private final Properties properties;

		FrameworkConfig(Properties properties) {
			this.properties = properties;
		}

		@Override
		public void activatePlugins(SpotlessEclipsePluginConfig config) {
			super.activatePlugins(config);
			config.add(new JSONCorePlugin());
		}

		@Override
		public void customize() {
			PluginPreferences.configure(JSONCorePlugin.getDefault(), new JSONCorePreferenceInitializer(), properties);
		}
	}

}
