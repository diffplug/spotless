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
package com.diffplug.gradle.spotless.eclipse.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Temporary bundle loaded by common class loader and added temporarily.
 * It shall not be recognized as a Eclipse plugin (providing plugin.xml)
 * but as a mere OSGI bundle.
 * <p>
 * META data look-ups are not supported (only required for Eclipse plugins).
 * Entry look-up in the bundle space (findEntries) is not supported.
 * Installation related methods (update/uninstall) are not supported.
 * Unsupported methods are marked as deprecated and causing an exception.
 */
interface TemporaryBundle extends Bundle {

	@Override
	default public Version getVersion() {
		return Version.emptyVersion; //Cannot support multiple version using single class loader.
	}

	@Override
	default public int compareTo(Bundle o) {
		//Symbolic name is sufficient to distinguish bundles
		return getSymbolicName().compareTo(o.getSymbolicName());
	}

	@Override
	default public <A> A adapt(Class<A> type) {
		return null; //Adaptation is not successful
	}

	@Override
	@Deprecated
	default public Dictionary<String, String> getHeaders() {
		throw new UnsupportedOperationException("Bundle META information is not available.");
	}

	@Override
	@Deprecated
	default public Dictionary<String, String> getHeaders(String locale) {
		return getHeaders();
	}

	@Override
	default public URL getResource(String name) {
		return getClass().getClassLoader().getResource(name);
	}

	@Override
	default public Enumeration<URL> getResources(String name) throws IOException {
		return getClass().getClassLoader().getResources(name);
	}

	@Override
	@Deprecated
	default public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		return null; //Local JAR look-up are not supported per default
	}

	@Override
	default public Class<?> loadClass(String name) throws ClassNotFoundException {
		return getClass().getClassLoader().loadClass(name);
	}

	@Override
	default public boolean hasPermission(Object permission) {
		return true; //Dedicated permissions are not supported
	}

	@Override
	default public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
		return new HashMap<X509Certificate, List<X509Certificate>>(0); //Bundle is not signed
	}

	@Override
	default public File getDataFile(String filename) {
		return null; //No file system support for persistent files
	}

}
