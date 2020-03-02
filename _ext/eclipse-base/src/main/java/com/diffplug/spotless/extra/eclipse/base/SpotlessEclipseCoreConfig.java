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

import org.osgi.framework.BundleActivator;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.DefaultBundles;
import com.diffplug.spotless.extra.eclipse.base.osgi.BundleConfig;

/**
 * Configuration of core bundles which shall be provided by the
 * {@link SpotlessEclipseFramework}.
 * <p>
 * Core bundles are not accessed via the plugin registry, but by static methods.
 * Hence they do not require a registration, which allows a lightweight
 * setup.
 * </p>
 * See {@code org.eclipse.core.internal.runtime.PlatformActivator} implementation for details.
 */
public class SpotlessEclipseCoreConfig extends BundleConfig<SpotlessEclipseFramework.DefaultBundles> {

	/**
	 * Don't instantiate and call {@link SpotlessEclipseConfig} directly.
	 * Registered bundles should only be instantiated once, since
	 * older bundles still abusing singletons for access.
	 */
	SpotlessEclipseCoreConfig() {}

	@Override
	public void applyDefault() {
		add(SpotlessEclipseFramework.DefaultBundles.createAll());
	}

	@Override
	protected BundleActivator create(DefaultBundles bundle) {
		return bundle.create();
	}

	@Override
	protected int getDefaultState(DefaultBundles bundle) {
		return bundle.getDesiredState();
	}

}
