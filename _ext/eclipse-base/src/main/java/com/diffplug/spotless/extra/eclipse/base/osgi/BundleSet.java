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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/** Thread save set of bundles, whereas each bundle has a unique symbolic name and a unique ID. */
class BundleSet {
	private final Map<String, Bundle> symbolicName2bundle;
	private final Map<Long, Bundle> id2bundle;

	BundleSet() {
		symbolicName2bundle = new ConcurrentHashMap<String, Bundle>();
		id2bundle = new ConcurrentHashMap<Long, Bundle>();
	}

	/** Get all bundles in collection */
	Collection<Bundle> getAll() {
		return symbolicName2bundle.values();
	}

	/** Get bundle by symbolic name or null if collection does not contain the corresponding bundle. */
	Bundle get(String symbolicName) {
		return symbolicName2bundle.get(symbolicName);
	}

	/** Get bundle by its ID or null if collection does not contain the corresponding bundle. */
	Bundle get(long id) {
		return id2bundle.get(id);
	}

	/** Add bundle to collection.
	 * @throws BundleException */
	void add(Bundle bundle) throws BundleException {
		Bundle existingBundle = symbolicName2bundle.put(bundle.getSymbolicName(), bundle);
		if (null != existingBundle) {
			throw new BundleException(
					String.format("Bundle '%s' (ID: %d) is already part of collection with ID %d.",
							bundle.getSymbolicName(), bundle.getBundleId(), existingBundle.getBundleId()),
					BundleException.DUPLICATE_BUNDLE_ERROR);
		}
		Bundle bundleWithSameID = id2bundle.put(bundle.getBundleId(), bundle);
		if (null != bundleWithSameID) {
			throw new BundleException(
					String.format("Bundle ID '%d' for '%s' is already used by '%s'.",
							bundle.getBundleId(),
							bundle.getSymbolicName(),
							bundleWithSameID.getSymbolicName()),
					BundleException.DUPLICATE_BUNDLE_ERROR);
		}
	}

}
