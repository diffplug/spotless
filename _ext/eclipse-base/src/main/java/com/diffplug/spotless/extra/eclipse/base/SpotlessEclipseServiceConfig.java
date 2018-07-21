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
package com.diffplug.spotless.extra.eclipse.base;

import static com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.LINE_DELIMITER;

import java.util.Map;

import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

import com.diffplug.spotless.extra.eclipse.base.service.*;

/**
 * Configuration/Provision of services which shall be provided by the {@link SpotlessEclipseFramework}.
 * <p>
 * The services provide basic functions/configuration to the {@link SpotlessEclipseFramework} bundles use the services.
 * Hence the services can be used to customize the behavior of core bundles and plugins configured
 * in the {@link SpotlessEclipseCoreConfig} and {@link SpotlessEclipsePluginConfig}.
 * </p>
 */
public interface SpotlessEclipseServiceConfig {

	/** Sets property/preference value available to all bundles, plugins and services. */
	void set(String key, String value);

	/** Sets property/preference values available to all bundles, plugins and services. */
	default void set(Map<String, String> properties) {
		properties.forEach((k, v) -> this.set(k, v));
	}

	/**
	 * Add custom service to collection.
	 * <p>
	 * Only one service per interface is allowed.
	 * A service instance implementing multiple interfaces, can be added for each interface.
	 * <p>
	 * Please refer to the default method implementation for examples.
	 *
	 * @param interfaceClass Service interface
	 * @param service Service instance
	 */
	public <S> void add(Class<S> interfaceClass, S service);

	/**
	 * Spotless formatters should not be configured by environment variables, and
	 * they shall be OS independent.
	 */
	default public void hideEnvironment() {
		add(EnvironmentInfo.class, new HiddenEnvironment());
	}

	/**
	 * Eclipse provides means to lookup the file content type, e.g. by file name extensions.
	 * This possibility is not required by most Spotless formatters.
	 */
	default public void ignoreContentType() {
		add(IContentTypeManager.class, new NoContentTypeSpecificHandling());
	}

	/** Disable Eclipse internal debugging. */
	default public void disableDebugging() {
		add(DebugOptions.class, new NoDebugging());
	}

	/** Ignore accesses of unsupported preference. */
	default public void ignoreUnsupportedPreferences() {
		add(IPreferencesService.class, new NoEclipsePreferences());
	}

	/**
	 * Use temporary locations in case plugins require to store file.
	 * These files (for example workspace preferences) will be deleted
	 * as soon as the application terminates.
	 */
	default public void useTemporaryLocations() {
		add(Location.class, new TemporaryLocation());
	}

	/**
	 * In case the string which will be formatted does not contain any
	 * line delimiter (single line), Eclipse falls back to use the
	 * system property.
	 * Change the system default to the UNIX line separator as required
	 * by Spotless.
	 */
	default public void changeSystemLineSeparator() {
		System.setProperty("line.separator", LINE_DELIMITER);
	}

	/** Applies the default configurations. */
	default public void applyDefault() {
		hideEnvironment();
		ignoreContentType();
		disableDebugging();
		ignoreUnsupportedPreferences();
		useTemporaryLocations();
		changeSystemLineSeparator();
	}
}
