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
package com.diffplug.spotless.extra.eclipse.base.osgi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;

/** A bundle configuration is given by its activator and the desired state */
public abstract class BundleConfig<T extends Enum<T>> {
	public static class Entry {
		public final BundleActivator activator;
		public final int state;

		public Entry(BundleActivator activator, int state) {
			Objects.requireNonNull(activator, "activator");
			this.activator = activator;
			this.state = state;
		}
	}

	private final List<Entry> config;

	protected BundleConfig() {
		config = new ArrayList<Entry>();
	}

	/**
	 * Activate a bundle with a certain state. A non-active state is used by
	 * some bundles to allow a slim instantiation (for example in a headless
	 * Eclipse).
	 */
	public void add(BundleActivator activator, int state) {
		config.add(new Entry(activator, state));
	}

	/** Returns the current configuration */
	public List<BundleConfig.Entry> get() {
		return config;
	}

	/** Activate a set of bundles with certain states */
	public void add(List<Entry> config) {
		this.config.addAll(config);
	}

	/** Activate a bundle in active state, which is the nominal choice */
	public void add(BundleActivator activator) {
		add(activator, Bundle.ACTIVE);
	}

	/** Activate a set of bundles with in active state */
	public void add(Collection<BundleActivator> config) {
		config.stream().forEach(entry -> add(entry));
	}

	/** Activate a default bundle with its default state */
	public void add(T bundle) {
		add(create(bundle), getDefaultState(bundle));
	}

	/** Activate a set of default bundles with their default states */
	@SuppressWarnings("unchecked")
	public void add(T... bundles) {
		Arrays.asList(bundles).forEach(bundle -> add(bundle));
	}

	/** Activate a default bundle with a custom state */
	public void add(T bundle, int state) {
		add(create(bundle), state);
	}

	/** Applies the default configurations. */
	public abstract void applyDefault();

	protected abstract BundleActivator create(T bundle);

	protected abstract int getDefaultState(T bundle);

}
