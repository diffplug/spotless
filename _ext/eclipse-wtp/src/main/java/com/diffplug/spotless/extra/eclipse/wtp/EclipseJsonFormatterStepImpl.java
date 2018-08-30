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

import org.eclipse.wst.json.core.JSONCorePlugin;
import org.eclipse.wst.json.core.cleanup.CleanupProcessorJSON;
import org.eclipse.wst.json.core.internal.preferences.JSONCorePreferenceInitializer;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;

import com.diffplug.spotless.extra.eclipse.wtp.sse.CleanupStep;

/** Formatter step which calls out to the Eclipse JSON cleanup processor and formatter. */
public class EclipseJsonFormatterStepImpl extends CleanupStep<EclipseJsonFormatterStepImpl.SpotlessJsonCleanup> {

	public EclipseJsonFormatterStepImpl(Properties properties) throws Exception {
		super(new SpotlessJsonCleanup(), plugin -> plugin.add(new JSONCorePlugin()));
		configure(properties, true, JSONCorePlugin.getDefault(), new JSONCorePreferenceInitializer());
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
	public static class SpotlessJsonCleanup extends CleanupProcessorJSON implements CleanupStep.ProcessorAccessor {
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
