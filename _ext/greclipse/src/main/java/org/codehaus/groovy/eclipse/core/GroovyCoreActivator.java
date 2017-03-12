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
package org.codehaus.groovy.eclipse.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.eclipse.GroovyLogManager;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Overrides original class allowing Spotless to run formatter without activating Eclipse core.
 * <p>
 * Class provides access to central logger as required by GroovyCore.
 * Furthermore GroovyCore requires a OSGI bundle, which is just used for accessing the plug-in id.
 * This class provides a corresponding mock for the bundle.
 */
public class GroovyCoreActivator {

	public static final String PLUGIN_ID = "com.diffplug.gradle.spotless.groovy.eclipse";

	private static GroovyCoreActivator INSTANCE = null;

	private final ILog logger;
	private final Bundle bundle;

	public GroovyCoreActivator() {
		bundle = new MockOsgiBundle();
		logger = new LogAdapter(bundle);
		GroovyLogManager.manager.setUseDefaultLogger(false);
	}

	/** Lazy constructor returning the shared activator instance */
	public static GroovyCoreActivator getDefault() {
		if (null == INSTANCE) {
			INSTANCE = new GroovyCoreActivator();
		}
		return INSTANCE;
	}

	/** Returns a OSIG bundle mock for GroovyCore, just providing functionality for Groovy error handling */
	public Bundle getBundle() {
		return bundle;
	}

	/** Provides Eclipse logger interface without requiring an active Eclipse core */
	public ILog getLog() {
		return logger;
	}

	private static class LogAdapter implements ILog {

		private final static Integer[] QUIET_SEVERITY_VALUES = new Integer[]{IStatus.CANCEL, IStatus.INFO, IStatus.OK};
		private final static Set<Integer> QUIET_SEVERITIES = new HashSet<Integer>(Arrays.asList(QUIET_SEVERITY_VALUES));

		private final Set<ILogListener> listeners;
		private final Bundle bundle;

		public LogAdapter(Bundle bundle) {
			listeners = new HashSet<ILogListener>();
			this.bundle = bundle;
		}

		@Override
		public void addLogListener(ILogListener arg0) {
			listeners.add(arg0);
		}

		@Override
		public Bundle getBundle() {
			return bundle;
		}

		@Override
		public void log(IStatus status) {
			for (ILogListener listener : listeners) {
				listener.logging(status, PLUGIN_ID);
			}
			if (QUIET_SEVERITIES.contains(status.getSeverity())) {
				System.out.println(status.getMessage());
			} else {
				System.err.println(status.getMessage());
			}
		}

		@Override
		public void removeLogListener(ILogListener arg0) {
			listeners.remove(arg0);
		}

	}

	private static class MockOsgiBundle implements Bundle {

		@Override
		public int compareTo(Bundle arg0) {
			return 0; //Always equal. There is only one bundle
		}

		@Override
		public <A> A adapt(Class<A> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Enumeration<URL> findEntries(String arg0, String arg1, boolean arg2) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BundleContext getBundleContext() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getBundleId() {
			return 0;
		}

		@Override
		public File getDataFile(String arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public URL getEntry(String arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Enumeration<String> getEntryPaths(String arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Dictionary<String, String> getHeaders() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Dictionary<String, String> getHeaders(String arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getLastModified() {
			return 0; //No modifications
		}

		@Override
		public String getLocation() {
			return PLUGIN_ID;
		}

		@Override
		public ServiceReference<?>[] getRegisteredServices() {
			return new ServiceReference<?>[0];
		}

		@Override
		public URL getResource(String arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Enumeration<URL> getResources(String arg0) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ServiceReference<?>[] getServicesInUse() {
			return new ServiceReference<?>[0];
		}

		@Override
		public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int arg0) {
			return new HashMap<X509Certificate, List<X509Certificate>>();
		}

		@Override
		public int getState() {
			return Bundle.ACTIVE;
		}

		@Override
		public String getSymbolicName() {
			return PLUGIN_ID;
		}

		@Override
		public Version getVersion() {
			return new Version(0, 0, 0);
		}

		@Override
		public boolean hasPermission(Object arg0) {
			return true;
		}

		@Override
		public Class<?> loadClass(String arg0) throws ClassNotFoundException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void start() throws BundleException {
			//Nothing to do
		}

		@Override
		public void start(int arg0) throws BundleException {
			//Nothing to do
		}

		@Override
		public void stop() throws BundleException {
			//Nothing to do
		}

		@Override
		public void stop(int arg0) throws BundleException {
			//Nothing to do
		}

		@Override
		public void uninstall() throws BundleException {
			//Nothing to do
		}

		@Override
		public void update() throws BundleException {
			//Nothing to do
		}

		@Override
		public void update(InputStream arg0) throws BundleException {
			//Nothing to do
		}

	}

}
