/*
 * Copyright 2016-2021 DiffPlug
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.DirBundleFile;
import org.eclipse.osgi.storage.bundlefile.ZipBundleFile;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.diffplug.spotless.extra.eclipse.base.service.NoDebugging;

/**
 * Helper to access resources.
 * <p>
 * Spotless Eclipse formatters using in many cases fat JARs, compressing multiple
 * bundles within one JAR. Their resources can get overridden in a fat JAR.
 * Hence they provided under the bundle activators path name.
 * </p>
 * <p>
 * The resource overriding prevention is required in case the Spotless
 * Eclipse formatters integrate third party JARs, not available via M2.
 * Additionally it can be used in case existing third party Eclipse plugins
 * are mocked by Spotless, requiring the provision of multiple customized
 * plugin information (META-INF, plugin.xml) within one JAR.
 * </p>
 */
class ResourceAccessor {
	private static final Debug NO_DEBUGGING = new Debug(new NoDebugging());
	private final String fatJarResourcePath;
	private final BundleFile bundleFile;

	/** Resources are located in the SpotlessFramework JAR */
	ResourceAccessor() throws BundleException {
		this(ResourceAccessor.class);
	}

	/** Resources are located in the JAR of the given class
	 * @throws BundleException */
	ResourceAccessor(Class<?> clazz) throws BundleException {
		String bundleObjPath = clazz.getName();
		fatJarResourcePath = bundleObjPath.substring(0, bundleObjPath.lastIndexOf('.'));
		try {
			bundleFile = getBundlFile(clazz);
		} catch (BundleException e) {
			throw new BundleException(String.format("Failed to locate resources for bundle '%s'.", clazz.getName()), e);
		}
	}

	private static BundleFile getBundlFile(Class<?> clazz) throws BundleException {
		URI objUri = getBundleUri(clazz);
		File jarOrDirectory = new File(objUri.getPath());
		if (!(jarOrDirectory.exists() && jarOrDirectory.canRead())) {
			throw new BundleException(String.format("Path '%s' for '%s' is not accessible exist on local file system.", objUri, clazz.getName()), BundleException.READ_ERROR);
		}
		try {
			return jarOrDirectory.isDirectory() ? new DirBundleFile(jarOrDirectory, false) : new ZipBundleFile(jarOrDirectory, null, null, NO_DEBUGGING, false);
		} catch (IOException e) {
			throw new BundleException(String.format("Cannot access bundle at '%s'.", jarOrDirectory), BundleException.READ_ERROR, e);
		}
	}

	private static URI getBundleUri(Class<?> clazz) throws BundleException {
		try {
			URL objUrl = clazz.getProtectionDomain().getCodeSource().getLocation();
			return objUrl.toURI();
		} catch (NullPointerException e) {
			//No bunlde should be used for RT classes lookup. See also org.eclipse.core.runtime.PerformanceStats.
			throw new BundleException(String.format("No code source can be located for class '%s'. Class is probably not within a bundle, but part of the RT.", clazz.getName()), BundleException.READ_ERROR, e);
		} catch (SecurityException e) {
			throw new BundleException(String.format("Access to class '%s' is denied.", clazz.getName()), BundleException.READ_ERROR, e);
		} catch (URISyntaxException e) {
			throw new BundleException(String.format("Path for '%s' is invalid.", clazz.getName()), BundleException.READ_ERROR, e);
		}
	}

	/** Get the manifest name from the resources. */
	String getManifestName() throws BundleException {
		URL manifestUrl = getEntry(JarFile.MANIFEST_NAME);
		if (null != manifestUrl) {
			try {
				Manifest manifest = new Manifest(manifestUrl.openStream());
				String headerValue = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
				if (null == headerValue) {
					throw new BundleException(String.format("Symbolic values not found in '%s'.", manifestUrl), BundleException.MANIFEST_ERROR);
				}
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, headerValue);
				if (null == elements) {
					throw new BundleException(String.format("Symbolic name not found '%s'. Value is '%s'.", manifestUrl, headerValue), BundleException.MANIFEST_ERROR);
				}
				//The parser already checked that at least one value exists
				return elements[0].getValueComponents()[0];

			} catch (IOException e) {
				throw new BundleException(String.format("Failed to parse Manifest '%s' in '%s'.", manifestUrl, bundleFile.toString()), BundleException.MANIFEST_ERROR, e);
			}
		}
		throw new BundleException(String.format("'%s' in '%s' not found. Tried also fat JAR location '%s'.", JarFile.MANIFEST_NAME, bundleFile.toString(), fatJarResourcePath), BundleException.MANIFEST_ERROR);
	}

	/** Get resource URL for relative path, or null if the path is not present */
	URL getEntry(String path) {
		BundleEntry entry = bundleFile.getEntry(getFatJarPath(path));
		if (null == entry) {
			entry = bundleFile.getEntry(path);
		}
		return null == entry ? null : entry.getLocalURL();
	}

	/**
	 * Enumeration of Strings that indicate the paths found or null if the path does not exist.
	 */
	Enumeration<String> getEntries(String path) {
		Enumeration<String> entries = bundleFile.getEntryPaths(getFatJarPath(path));
		if (null == entries) {
			entries = bundleFile.getEntryPaths(path);
		}
		return entries;
	}

	private String getFatJarPath(String path) {
		return fatJarResourcePath + "/" + path;
	}

}
