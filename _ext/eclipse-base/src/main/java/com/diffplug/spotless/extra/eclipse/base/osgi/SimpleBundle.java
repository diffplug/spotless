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

import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/** Fixed state simple bundle. */
class SimpleBundle implements StaticBundle, TemporaryBundle {
	private final String name;
	private final int state;
	private final BundleContext context;
	private final int id;
	private final ResourceAccessor resources;

	/** System bundle corresponding to the SpotlessFramework JARs manifest */
	SimpleBundle(BundleContext context, int state) throws BundleException {
		this(context, state, new ResourceAccessor());
	}

	/** System bundle for a dedicated bundle activator */
	SimpleBundle(BundleContext context, int state, BundleActivator activator) throws BundleException {
		this(context, state, new ResourceAccessor(activator.getClass()));
	}

	/** Internal constructor  */
	private SimpleBundle(BundleContext context, int state, ResourceAccessor resources) throws BundleException {
		this.state = state;
		this.context = context;
		this.resources = resources;
		id = context.getBundles().length;
		name = resources.getManifestName();
	}

	/** Additional bundle with a different symbolic name and state */
	SimpleBundle(SimpleBundle master, String name, int state) {
		this.name = name;
		this.state = state;
		context = master.context;
		resources = master.resources;
		id = context.getBundles().length;
	}

	/** Bundle clone with a different state */
	SimpleBundle(SimpleBundle master, int state) {
		this.state = state;
		context = master.context;
		resources = master.resources;
		id = master.id;
		name = master.name;
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public long getBundleId() {
		return id;
	}

	@Override
	public ServiceReference<?>[] getRegisteredServices() {
		try {
			return context.getAllServiceReferences(null, null);
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e); //Filter 'null' is valid for 'select all'.
		}
	}

	@Override
	public String getSymbolicName() {
		return name;
	}

	@Override
	public BundleContext getBundleContext() {
		return context;
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		return resources.getEntries(path);
	}

	@Override
	public URL getEntry(String path) {
		return resources.getEntry(path);
	}

}
