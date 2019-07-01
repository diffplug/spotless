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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * Simplified bundle capability ignoring internal wiring and versions
 * <p/>
 * Since multiple versions/implementations of bundles for the same
 * capability is not supported a split of bundle capability and revision is not required.
 */
class SimpleBundleCapability implements BundleCapability, BundleRevision {
	private final Bundle bundle;

	SimpleBundleCapability(Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public BundleRevision getRevision() {
		return this;
	}

	@Override
	public String getNamespace() {
		return this.getClass().getName(); //All bundles live in th same namespace
	}

	@Override
	public Map<String, String> getDirectives() {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.emptyMap();
	}

	@Override
	public BundleRevision getResource() {
		return this;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public String getSymbolicName() {
		return bundle.getSymbolicName();
	}

	@Override
	public Version getVersion() {
		return bundle.getVersion();
	}

	@Override
	public List<BundleCapability> getDeclaredCapabilities(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<BundleRequirement> getDeclaredRequirements(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public int getTypes() {
		return 0; //It does not matter whether this bunddle is a fragment of not since all bundles are initially provided
	}

	@Override
	public BundleWiring getWiring() {
		return null; //No wiring information
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}

}
