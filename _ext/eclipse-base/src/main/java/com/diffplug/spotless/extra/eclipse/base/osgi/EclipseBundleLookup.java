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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.osgi.internal.framework.FilterImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

/**
 *
 * {@link PackageAdmin} and {@link FrameworkWiring} service for bundle look-up.
 * <p>
 * The wiring information will always claim that all required bundles are present, since
 * Spotlss does on purpose not provide all dependencies requested by plugins, since
 * only small parts of the plugins are used.
 * Removal and addition requests for bundles will always claim that there is nothing to do.
 * <p>
 * PackageAdmin interface is deprecated, but might still be used by bundles.
 * It is kept for backward compatibility until removed from Eclipse.
 */
@SuppressWarnings("deprecation")
class EclipseBundleLookup implements FrameworkWiring, PackageAdmin {

	private static final Set<String> OSGI_KEYS_FOR_SYMBOLIC_NAMES = Collections.unmodifiableSet(Stream.of(IdentityNamespace.IDENTITY_NAMESPACE, IdentityNamespace.TYPE_BUNDLE).collect(Collectors.toSet()));
	private final Bundle systemBundle;
	private final BundleSet bundles;

	EclipseBundleLookup(final Bundle systemBundle, final BundleSet bundles) {
		this.systemBundle = systemBundle;
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

	@Override
	public Bundle getBundle() {
		return systemBundle;
	}

	@Override
	public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
		//Spotless bundles cannot be loaded dynamically
	}

	@Override
	public boolean resolveBundles(Collection<Bundle> bundles) {
		return true;
	}

	@Override
	public Collection<Bundle> getRemovalPendingBundles() {
		return Collections.emptyList(); //Nothing to remove
	}

	@Override
	public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
		return Collections.emptyList(); //No dependencies
	}

	@Override
	public Collection<BundleCapability> findProviders(Requirement requirement) {
		// requirement must not be null (according to interface description)!
		String filterSpec = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (null == filterSpec) {
			throw new IllegalArgumentException("Requirement filter diretive '" + Namespace.REQUIREMENT_FILTER_DIRECTIVE + "' not found.");
		}
		try {
			FilterImpl requirementFilter = FilterImpl.newInstance(filterSpec);
			Collection<String> requiredSymbolicNames = getRequestedSymbolicNames(requirementFilter);
			Collection<BundleCapability> capabilities = new ArrayList<BundleCapability>(requiredSymbolicNames.size());
			requiredSymbolicNames.forEach(symbolicName -> {
				Bundle bundle = bundles.get(symbolicName);
				if (bundle != null) {
					capabilities.add(new SimpleBundleCapability(bundle));
				}
			});
			return capabilities;
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Filter specifiation invalid:\n" + filterSpec, e);
		}
	}

	/**
	 * Simplified parser irgnoreing the version.
	 * Parser is incomplete since it ignores the filter operation.
	 * It basicall implements the bespoke way Eclipse maps its old style bundle handling to OSGI.
	 */
	private static Collection<String> getRequestedSymbolicNames(FilterImpl filter) {
		List<String> symbolicNames = filter.getStandardOSGiAttributes().entrySet().stream().filter(entry -> OSGI_KEYS_FOR_SYMBOLIC_NAMES.contains(entry.getKey())).map(entry -> entry.getValue()).collect(Collectors.toList());
		filter.getChildren().forEach(childFilter -> {
			symbolicNames.addAll(getRequestedSymbolicNames(childFilter));
		});
		return symbolicNames;
	}
}
