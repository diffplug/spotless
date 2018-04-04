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
package com.diffplug.gradle.spotless.xml.eclipse.osgi;

import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

/** 
 * PackageAdmin service for bundle look-up and bypassing wiring.
 * <p>
 * The wiring information will always claim that all required bundles are present.
 * Other functionality is not supported.
 * Unsupported methods are marked as deprecated an lead to an UnsupportedOperationException.
 * <p>
 * Interface is deprecated, but for example the InternalPlatform still uses PackageAdmin. 
 */
@SuppressWarnings("deprecation")
class EclipseBundleLookup implements PackageAdmin {

	private final BundleCollection bundles;

	public EclipseBundleLookup(final BundleCollection bundles) {
		this.bundles = bundles;
	}

	@Override
	@Deprecated
	public ExportedPackage[] getExportedPackages(Bundle bundle) {
		return null;
	}

	@Override
	@Deprecated
	public ExportedPackage[] getExportedPackages(String name) {
		return null;
	}

	@Override
	@Deprecated
	public ExportedPackage getExportedPackage(String name) {
		return null;
	}

	@Override
	@Deprecated
	public void refreshPackages(Bundle[] bundles) {}

	@Override
	public boolean resolveBundles(Bundle[] bundles) {
		return true; //All required bundles can be considered available (to be confirmed by UT)
	}

	@Override
	public RequiredBundle[] getRequiredBundles(String symbolicName) {
		return null; // No unresolved required bundles exist 
	}

	@Override
	public Bundle[] getBundles(String symbolicName, String versionRange) {
		//Bundles with different versions cannot be supported due to the usage of same class-loader
		Bundle bundle = bundles.get(symbolicName);
		return (null == bundle) ? null : new Bundle[]{bundle};
	}

	@Override
	public Bundle[] getFragments(Bundle bundle) {
		return null; //No fragments
	}

	@Override
	public Bundle[] getHosts(Bundle bundle) {
		return null; //No fragments
	}

	@Override
	public Bundle getBundle(@SuppressWarnings("rawtypes") Class clazz) {
		return bundles.get(clazz.getName());
	}

	@Override
	public int getBundleType(Bundle bundle) {
		return 0; //No fragments
	}

}
