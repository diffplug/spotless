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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/** Thread save collection of bundles */
class BundleCollection {
	private final Map<String, Bundle> symbolicName2bundle = new ConcurrentHashMap<String, Bundle>();
	private final Map<Long, Bundle> id2bundle = new ConcurrentHashMap<Long, Bundle>();

	/** Get all bundles in collection */
	public Collection<Bundle> getAll() {
		return symbolicName2bundle.values();
	}

	/** Get bundle by symbolic name or null if collection does not contain the corresponding bundle. */
	public Bundle get(String symbolicName) {
		return symbolicName2bundle.get(symbolicName);
	}

	/** Get bundle by its ID or null if collection does not contain the corresponding bundle. */
	public Bundle get(long id) {
		return id2bundle.get(id);
	}

	/** Add bundle to collection.
	 * @throws BundleException */
	public void add(Bundle bundle) throws BundleException {
		if (null != symbolicName2bundle.put(bundle.getSymbolicName(), bundle)) {
			throw new BundleException(
					String.format("Bundle '%s' is already part of collection.", bundle.getSymbolicName()), BundleException.DUPLICATE_BUNDLE_ERROR);
		}
		Bundle bundleWithSameID = id2bundle.put(bundle.getBundleId(), bundle);
		if (null != bundleWithSameID) {
			throw new BundleException(
					String.format("Bundle ID '%d' of '%s' is already used by '%s'.",
							bundle.getBundleId(),
							bundle.getSymbolicName(),
							bundleWithSameID),
					BundleException.DUPLICATE_BUNDLE_ERROR);
		}
	}
}
