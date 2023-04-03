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
package com.diffplug.spotless.extra.eclipse.base.runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/** Registers Eclipse plugins at runtime based on a loaded bundle.
 * <p>
 * Note that the functionality provided by this class uses incubation features of the Eclipse core.
 */
public class PluginRegistrar {
	private static final String PLUGIN_XML = "plugin.xml";
	private static final String PLUGIN_PROPERTIES = "plugin.properties";

	public static BundleException register(Bundle bundle) {
		var registrar = new PluginRegistrar(bundle);
		try {
			registrar.register();
		} catch (BundleException e) {
			return e;
		}
		return null;
	}

	private final Bundle bundle;

	private PluginRegistrar(Bundle bundle) {
		this.bundle = bundle;
	}

	private void register() throws BundleException {
		IExtensionRegistry reg = RegistryFactory.getRegistry();
		Object registryUser = ((ExtensionRegistry) reg).getTemporaryUserToken();
		if (!reg.addContribution(getStreamForEntry(PLUGIN_XML), createContributor(), false, null, getPluginProperties(), registryUser)) {
			throw new BundleException("Could not add plugin: " + bundle.getSymbolicName(), BundleException.REJECTED_BY_HOOK);
		}
	}

	private IContributor createContributor() {
		return new RegistryContributor(
				Long.toString(bundle.getBundleId()),
				bundle.getSymbolicName(),
				// Local host
				null,
				null);
	}

	private ResourceBundle getPluginProperties() throws BundleException {
		//Some plugins, like the org.codehaus.groovy.eclipse.core, do not provide a property file.
		InputStream is = entryExists(PLUGIN_PROPERTIES) ? getStreamForEntry(PLUGIN_PROPERTIES) : new ByteArrayInputStream(new byte[0]);
		try {
			return new PropertyResourceBundle(is);
		} catch (IOException e) {
			throw new BundleException(String.format("Bund resource '%s' is not encoded with ISO-8859-1.", PLUGIN_PROPERTIES), BundleException.MANIFEST_ERROR, e);
		}
	}

	private InputStream getStreamForEntry(String path) throws BundleException {
		try {
			return getEntry(path).openStream();
		} catch (IOException e) {
			throw new BundleException(String.format("Cannot access mandatory resource '%s'.", path), BundleException.MANIFEST_ERROR, e);
		}
	}

	private URL getEntry(String path) throws BundleException {
		URL url = bundle.getEntry(path);
		if (null == url) {
			throw new BundleException(String.format("Cannot find mandatory resource '%s'.", path), BundleException.MANIFEST_ERROR);
		}
		return url;
	}

	private boolean entryExists(String path) {
		return null != bundle.getEntry(path);
	}
}
