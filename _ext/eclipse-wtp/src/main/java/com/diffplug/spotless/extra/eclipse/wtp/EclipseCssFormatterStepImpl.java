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

import java.util.Properties;

import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.cleanup.CleanupProcessorCSS;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceInitializer;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;

import com.diffplug.spotless.extra.eclipse.wtp.sse.CleanupStep;

/** Formatter step which calls out to the Eclipse CSS cleanup and formatter. */
public class EclipseCssFormatterStepImpl extends CleanupStep<EclipseCssFormatterStepImpl.SpotlessCssCleanup> {

	public EclipseCssFormatterStepImpl(Properties properties) throws Exception {
		super(new SpotlessCssCleanup(), plugin -> plugin.add(new CSSCorePlugin()));
		configure(properties, true, CSSCorePlugin.getDefault(), new CSSCorePreferenceInitializer());
	}

	/**
	 * The FormatProcessorCSS does not allow a strict case formatting.
	 * Hence additionally the CleanupProcessorCSS is used.
	 */
	public static class SpotlessCssCleanup extends CleanupProcessorCSS implements CleanupStep.ProcessorAccessor {
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

	}
}
