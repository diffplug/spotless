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
package com.diffplug.spotless.extra.eclipse.wtp.sse;

import java.util.Collection;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.wst.sse.core.internal.cleanup.AbstractStructuredCleanupProcessor;
import org.eclipse.wst.sse.core.internal.format.AbstractStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.osgi.framework.BundleActivator;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;

/**
 * Common base class for step implementations based on an SSE cleanup processor.
 * <p>
 * Some WTP formatters do not apply all formatting within a formatter process,
 * but provide a cleanup and a formatter process, whereas the cleanup process
 * calls the formatter process.
 * </p>
 * <p>
 * Not all cleanup processes provided by WTP are suitable for Spotless formatting.
 * For example the XML cleanup processor focus on syntax and line delimiter
 * corrections. The syntax correction is based on the corresponding XSD/DTD,
 * but it must be assumed by the Spotless formatter that the provided files
 * are valid. It cannot be the purpose of a formatter to correct syntax.
 * A Java formatter should also not attempt to correct compilation errors.
 * A line delimiter correction would furthermore violate the Spotless rule
 * that the strings processed by a Spotless formatter chain must use
 * UNIX style delimiters.
 * </p>
 * @see org.eclipse.wst.sse.core.internal.cleanup.AbstractStructuredCleanupProcessor
 */
public class CleanupStep<T extends AbstractStructuredCleanupProcessor & CleanupStep.ProcessorAccessor> {

	/**
	 * Some of the SEE AbstractStructuredCleanupProcessor interface shall be
	 * made public to provide cleaner interfaces. */
	public interface ProcessorAccessor {
		/** Returns this.getContentType() */
		String getThisContentType();

		/** Returns this.getFormatProcessor() */
		IStructuredFormatProcessor getThisFormatProcessor();

		/** Calls this.refreshCleanupPreferences() */
		void refreshThisCleanupPreferences();
	}

	// The formatter cannot per configured per instance
	private final static Properties CONFIG = new Properties();
	private static boolean FIRST_CONFIG = true;

	protected final T processor;

	protected CleanupStep(T processor, Consumer<Collection<BundleActivator>> addptionalPlugins) throws Exception {
		SpotlessEclipseFramework.setup(
				config -> {
					config.disableDebugging();
					config.hideEnvironment();
					config.useTemporaryLocations();
					config.changeSystemLineSeparator();
					//Allow association of string passed in the CleanupStep.format to its type/plugin
					config.add(IContentTypeManager.class, new ContentTypeManager(processor));
					//The preference lookup via the ContentTypeManager, requires a preference service
					config.add(IPreferencesService.class, PreferencesService.getDefault());
				},
				plugins -> {
					plugins.addAll(SpotlessEclipseFramework.DefaultPlugins.createAll());
					addptionalPlugins.accept(plugins);
					/*
					 * The core preferences require do lookup the resources "config/override.properties"
					 * from the plugin ID.
					 * The values are never used, nor do we require the complete SSE core plugin to be started.
					 * Hence we just provide the internal plugin.
					 */
					plugins.add(new org.eclipse.wst.sse.core.internal.encoding.util.CodedResourcePlugin());
				});
		this.processor = processor;
		/*
		 *  Don't refresh the preferences every time a clone of the processor is created.
		 *  All processors shall use the preferences of its parent.
		 */
		this.processor.refreshCleanupPreferences = false;
	}

	protected final void configure(Properties properties, boolean usesPreferenceService, Plugin plugin, AbstractPreferenceInitializer preferencesInit) {
		synchronized (CONFIG) {
			if (usesPreferenceService) {
				assertConfigHasNotChanged(properties);
			}
			preferencesInit.initializeDefaultPreferences();
			SpotlessPreferences.configurePluginPreferences(plugin, properties);
			processor.refreshThisCleanupPreferences(); // Initialize cleanup processor preferences (if there are any)
			IStructuredFormatProcessor formatter = processor.getThisFormatProcessor(); // Initialize processor preferences by creating the formatter for the first time
			if (formatter instanceof AbstractStructuredFormatProcessor) {
				((AbstractStructuredFormatProcessor) formatter).refreshFormatPreferences = false;
			}
		}
	}

	private static void assertConfigHasNotChanged(final Properties properties) {
		synchronized (CONFIG) {
			if (FIRST_CONFIG) {
				FIRST_CONFIG = false;
				CONFIG.putAll(properties);
			} else if (!CONFIG.equals(properties)) {
				throw new IllegalArgumentException("The Eclipse formatter does not support multiple configurations.");
			}
		}
	}

	/**
	 * Calls the cleanup and formatting task of the processor and returns the formatted string.
	 * @param raw Dirty string
	 * @return Formatted string
	 * @throws Exception All exceptions are considered fatal to the build process (Gradle, Maven, ...) and should not be caught.
	 */
	public String format(String raw) throws Exception {
		return processor.cleanupContent(raw);
	}

}
