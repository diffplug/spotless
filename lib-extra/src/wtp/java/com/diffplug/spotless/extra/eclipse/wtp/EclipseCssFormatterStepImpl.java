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

import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.cleanup.CleanupProcessorCSS;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceInitializer;
import org.eclipse.wst.sse.core.internal.cleanup.AbstractStructuredCleanupProcessor;

import com.diffplug.spotless.extra.eclipse.wtp.sse.CleanupStep;
import com.diffplug.spotless.extra.eclipse.wtp.sse.PluginPreferences;

/** Formatter step which calls out to the Eclipse CSS cleanup and formatter. */
public class EclipseCssFormatterStepImpl extends CleanupStep {
	static {
		SolsticeSetup.init();
	}

	public EclipseCssFormatterStepImpl(Properties properties) throws Exception {
		super(new CleanupProcessor());
		PluginPreferences.configure(CSSCorePlugin.getDefault(), new CSSCorePreferenceInitializer(), properties);
		PluginPreferences.assertNoChanges(CSSCorePlugin.getDefault(), properties);
	}

	/**
	 * The FormatProcessorCSS does not allow a strict case formatting.
	 * Hence additionally the CleanupProcessorCSS is used.
	 */
	private static class CleanupProcessor extends CleanupProcessorCSS implements CleanupStep.ProcessorAccessor {
		@Override
		public String getTypeId() {
			return getContentType();
		}

		@Override
		public void refreshPreferences() {
			refreshCleanupPreferences();
		}

		@Override
		public AbstractStructuredCleanupProcessor get() {
			return this;
		}
	}
}
