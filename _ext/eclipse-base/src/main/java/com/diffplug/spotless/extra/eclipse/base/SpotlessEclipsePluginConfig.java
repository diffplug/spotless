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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.DefaultPlugins;
import com.diffplug.spotless.extra.eclipse.base.osgi.BundleConfig;

/**
 * Configuration of plugins which shall be provided by the
 * {@link SpotlessEclipseFramework}.
 * <p>
 * Plugins are registered via the Eclipse registry. Therefore they
 * required a {@code plugin.xml} configuration description.
 * Note that a plugin does not necessarily derive from the
 * Eclipse {@link org.eclipse.core.runtime.Plugin} class.
 * </p>
 * @see org.eclipse.core.runtime.RegistryFactory
 */
public class SpotlessEclipsePluginConfig extends BundleConfig<SpotlessEclipseFramework.DefaultPlugins> {

	@Override
	public void applyDefault() {
		add(SpotlessEclipseFramework.DefaultPlugins.createAll());
	}

	@Override
	protected BundleActivator create(DefaultPlugins bundle) {
		return bundle.create();
	}

	@Override
	protected int getDefaultState(DefaultPlugins bundle) {
		return Bundle.ACTIVE;
	}

}
