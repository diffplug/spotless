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

import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

/**
 * Unmodifiable bundle with a fixed life-cycle.
 * <p>
 * All state related modifications are ignored.
 * Installation related methods (update/uninstall) are unsupported.
 * Unsupported methods are marked as deprecated and causing an exception.
 */
public interface StaticBundle extends Bundle {

	@Override
	default public void start(int options) throws BundleException {}

	@Override
	default public void start() throws BundleException {}

	@Override
	default public void stop(int options) throws BundleException {}

	@Override
	default public void stop() throws BundleException {}

	@Override
	@Deprecated
	default public void update(InputStream input) throws BundleException {
		update();
	}

	@Override
	@Deprecated
	default public void update() throws BundleException {
		throw new UnsupportedOperationException("Bundle modifications are not supported.");
	}

	@Override
	@Deprecated
	default public void uninstall() throws BundleException {
		throw new UnsupportedOperationException("Bundles cannot be uninstalled.");
	}

	@Override
	default public long getLastModified() {
		return 0;
	}

	@Override
	@Deprecated
	default public String getLocation() {
		throw new UnsupportedOperationException("Bundle lookup by location only required for installation/update.");
	}

	@Override
	default public ServiceReference<?>[] getServicesInUse() {
		return getRegisteredServices(); //There is no distinction between available services and services in use.
	}

}
