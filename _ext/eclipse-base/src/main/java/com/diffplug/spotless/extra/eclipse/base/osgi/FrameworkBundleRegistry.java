/*
 * Copyright 2016-2020 DiffPlug
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

import java.util.Optional;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.connect.FrameworkUtilHelper;

/**
 * Framework bundle registry service for bundles which do not come with an activator.
 *
 * Instead of returning the system bundle, a new bundle is created
 * to provide an individual resource accessor, so that
 * the correct JAR is searched for relative resource paths.
 * Note that this can also be used to override
 * original resources by providing a resource in the
 * corresponding fat JAR location.
 */
public class FrameworkBundleRegistry implements FrameworkUtilHelper {
	static BundleController INSTANCE = null;

	static void initialize(BundleController bundleController) {
		if (INSTANCE != null) {
			throw new RuntimeException(FrameworkBundleRegistry.class.getName() + " already initialized.");
		}
		INSTANCE = bundleController;
	}

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		try {
			return Optional.of(new SimpleBundle(INSTANCE, classFromBundle));
		} catch (BundleException e) {
			//If the class cannot be associated to a JAR or fat JAR resource location, just retun null
		}
		return Optional.empty();
	}
}
