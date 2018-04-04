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
package com.diffplug.gradle.spotless.xml.eclipse.service;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.Preferences;

public class NoEclipsePreferences implements IPreferencesService {
	private static final String UNUSED = "unused";

	@Override
	public IEclipsePreferences getRootNode() {
		//Return value is not effectively used. 
		return DefaultScope.INSTANCE.getNode(UNUSED);
	}

	@Override
	public String get(String key, String defaultValue, Preferences[] nodes) {
		return null;
	}

	@Override
	public boolean getBoolean(String qualifier, String key, boolean defaultValue, IScopeContext[] contexts) {
		return false;
	}

	@Override
	public byte[] getByteArray(String qualifier, String key, byte[] defaultValue, IScopeContext[] contexts) {
		return null;
	}

	@Override
	public double getDouble(String qualifier, String key, double defaultValue, IScopeContext[] contexts) {
		return 0;
	}

	@Override
	public float getFloat(String qualifier, String key, float defaultValue, IScopeContext[] contexts) {
		return 0;
	}

	@Override
	public int getInt(String qualifier, String key, int defaultValue, IScopeContext[] contexts) {
		return 0;
	}

	@Override
	public long getLong(String qualifier, String key, long defaultValue, IScopeContext[] contexts) {
		return 0;
	}

	@Override
	public String getString(String qualifier, String key, String defaultValue, IScopeContext[] contexts) {
		return null;
	}

	@Override
	public IStatus exportPreferences(IEclipsePreferences node, OutputStream output, String[] excludesList) throws CoreException {
		return null;
	}

	@Override
	public IStatus importPreferences(InputStream input) throws CoreException {
		return null;
	}

	@Override
	public IStatus applyPreferences(IExportedPreferences preferences) throws CoreException {
		return null;
	}

	@Override
	public IExportedPreferences readPreferences(InputStream input) throws CoreException {
		return null;
	}

	@Override
	public String[] getDefaultLookupOrder(String qualifier, String key) {
		return null;
	}

	@Override
	public String[] getLookupOrder(String qualifier, String key) {
		return null;
	}

	@Override
	public void setDefaultLookupOrder(String qualifier, String key, String[] order) {}

	@Override
	public void exportPreferences(IEclipsePreferences node, IPreferenceFilter[] filters, OutputStream output) throws CoreException {}

	@Override
	public IPreferenceFilter[] matches(IEclipsePreferences node, IPreferenceFilter[] filters) throws CoreException {
		return null;
	}

	@Override
	public void applyPreferences(IEclipsePreferences node, IPreferenceFilter[] filters) throws CoreException {}

}
