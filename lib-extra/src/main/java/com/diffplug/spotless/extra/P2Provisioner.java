/*
 * Copyright 2016-2026 DiffPlug
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
package com.diffplug.spotless.extra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.diffplug.spotless.Provisioner;

import dev.equo.solstice.NestedJars;
import dev.equo.solstice.p2.CacheLocations;
import dev.equo.solstice.p2.P2ClientCache;
import dev.equo.solstice.p2.P2Model;
import dev.equo.solstice.p2.P2QueryCache;
import dev.equo.solstice.p2.P2QueryResult;

/**
 * Provisions dependencies from Eclipse P2 repositories.
 * Similar to {@link Provisioner} but for P2/OSGi bundles.
 */
@FunctionalInterface
public interface P2Provisioner {
	/**
	 * Resolves P2 dependencies and returns the classpath.
	 *
	 * @param modelWrapper wrapper around P2Model describing repositories and plugins to install
	 * @param mavenProvisioner provisioner for Maven dependencies (some P2 bundles are on Maven Central)
	 * @param cacheDirectory optional cache directory override
	 * @return ordered list of JAR files forming the classpath
	 */
	List<File> provisionP2Dependencies(
			P2ModelWrapper modelWrapper,
			Provisioner mavenProvisioner,
			@Nullable File cacheDirectory) throws IOException;

	/** Creates a non-caching P2Provisioner for simple use cases. */
	static P2Provisioner createDefault() {
		return (modelWrapper, mavenProvisioner, cacheDirectory) -> {
			try {
				if (cacheDirectory != null) {
					CacheLocations.override_p2data = cacheDirectory.toPath().resolve("dev/equo/p2-data").toFile();
				}
				P2Model model = modelWrapper.unwrap();
				P2QueryResult query = model.query(P2ClientCache.PREFER_OFFLINE, P2QueryCache.ALLOW);
				var classpath = new ArrayList<File>();
				var mavenDeps = new ArrayList<String>();
				mavenDeps.add("dev.equo.ide:solstice:1.8.1");
				mavenDeps.add("com.diffplug.durian:durian-swt.os:4.3.1");
				mavenDeps.addAll(query.getJarsOnMavenCentral());
				classpath.addAll(mavenProvisioner.provisionWithTransitives(false, mavenDeps));
				classpath.addAll(query.getJarsNotOnMavenCentral());
				for (var nested : NestedJars.inFiles(query.getJarsNotOnMavenCentral()).extractAllNestedJars()) {
					classpath.add(nested.getValue());
				}
				return classpath;
			} catch (Exception e) {
				throw new IOException("Failed to provision P2 dependencies", e);
			}
		};
	}
}
