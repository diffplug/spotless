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
package com.diffplug.spotless;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class loader is used to load classes of Spotless features from a search
 * path of URLs.<br/>
 * Features shall be independent from build tools. Hence the class loader of the
 * underlying build tool is e.g. skipped during the the search for classes.<br/>
 * Only {@link #BUILD_TOOLS_PACKAGES } are explicitly looked up from the class loader of
 * the build tool and the provided URLs are ignored. This allows the feature to use
 * distinct functionality of the build tool.
 */
public class FeatureClassLoader extends URLClassLoader {
	static {
		try {
			ClassLoader.registerAsParallelCapable();
		} catch (NoSuchMethodError ignore) {
			// Not supported on Java 6
		}
	}

	/** Packages which must be provided by the build tool or the corresponding Spotless plugin. */
	private static List<String> BUILD_TOOLS_PACKAGES = Arrays.asList("org.slf4j");

	private final ClassLoader buildToolClassLoader;

	/**
	 * Constructs a new FeatureClassLoader for the given URLs, based on an {@code URLClassLoader},
	 * using the system class loader as parent. For {@link #BUILD_TOOLS_PACKAGES }, the build
	 * tool class loader is used.
	 *
	 * @param urls the URLs from which to load classes and resources
	 * @param buildToolClassLoader The build tool class loader
	 * @exception  SecurityException  If a security manager exists and prevents the creation of a class loader.
	 * @exception  NullPointerException if {@code urls} is {@code null}.
	 */

	public FeatureClassLoader(URL[] urls, ClassLoader buildToolClassLoader) {
		super(urls, null);
		Objects.requireNonNull(buildToolClassLoader);
		this.buildToolClassLoader = buildToolClassLoader;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		for (String buildToolPackage : BUILD_TOOLS_PACKAGES) {
			if (name.startsWith(buildToolPackage)) {
				return buildToolClassLoader.loadClass(name);
			}
		}
		return super.findClass(name);
	}

}
