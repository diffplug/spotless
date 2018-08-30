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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/** Helper for testing */
public class TestBundle implements StaticBundle, TemporaryBundle {
	private final String symbolicName;
	private final long id;

	public TestBundle(long id, String symbolicName) {
		this.id = id;
		this.symbolicName = symbolicName;
	}

	@Override
	public int getState() {
		return 0;
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return null;
	}

	@Override
	public long getBundleId() {
		return id;
	}

	@Override
	public ServiceReference<?>[] getRegisteredServices() {
		return null;
	}

	@Override
	public boolean hasPermission(Object permission) {
		return false;
	}

	@Override
	public URL getResource(String name) {
		return null;
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		return null;
	}

	@Override
	public String getSymbolicName() {
		return symbolicName;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return null;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return null;
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		return null;
	}

	@Override
	public URL getEntry(String path) {
		return null;
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		return null;
	}

	@Override
	public BundleContext getBundleContext() {
		return null;
	}

	@Override
	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
		return null;
	}

	@Override
	public Version getVersion() {
		return null;
	}

	@Override
	public <A> A adapt(Class<A> type) {
		return null;
	}

	@Override
	public File getDataFile(String filename) {
		return null;
	}

	@Override
	public int compareTo(Bundle o) {
		return 0;
	}

}
