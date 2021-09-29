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
package com.diffplug.spotless;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * This class loader is used to load classes of Spotless features from a search
 * path of URLs.<br/>
 * Features shall be independent from build tools. Hence the class loader of the
 * underlying build tool is e.g. skipped during the the search for classes.<br/>
 * Only {@link #BUILD_TOOLS_PACKAGES } are explicitly looked up from the class loader of
 * the build tool and the provided URLs are ignored. This allows the feature to use
 * distinct functionality of the build tool.
 */
class FeatureClassLoader extends URLClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	/**
	 * The following packages must be provided by the build tool or the corresponding Spotless plugin:
	 * <ul>
	 *   <li>org.slf4j - SLF4J API must be provided. If no SLF4J binding is provided, log messages are dropped.</li>
	 * </ul>
	 */
	static final List<String> BUILD_TOOLS_PACKAGES = Collections.unmodifiableList(Arrays.asList("org.slf4j."));
	// NOTE: if this changes, you need to also update the `JarState.getClassLoader` methods.

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

	FeatureClassLoader(URL[] urls, ClassLoader buildToolClassLoader) {
		super(urls, getParentClassLoader());
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
		if (name.startsWith("com.diffplug.spotless.glue.")) {
			String path = name.replace('.', '/') + ".class";
			URL url = findResource(path);
			if (url == null) {
				throw new ClassNotFoundException(name);
			}
			try {
				return defineClass(name, urlToByteBuffer(url), (ProtectionDomain) null);
			} catch (IOException e) {
				throw new ClassNotFoundException(name, e);
			}
		} else if (name.startsWith("com.diffplug.spotless.")) {
			return buildToolClassLoader.loadClass(name);
		} else {
			return super.findClass(name);
		}
	}

	@Override
	public URL findResource(String name) {
		URL resource = super.findResource(name);
		if (resource != null) {
			return resource;
		}
		return buildToolClassLoader.getResource(name);
	}

	private static ByteBuffer urlToByteBuffer(URL url) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1024];
		InputStream inputStream = url.openStream();
		while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();
		return ByteBuffer.wrap(buffer.toByteArray());
	}

	/**
	 * Making spotless Java 9+ compatible. In Java 8 (and minor) the bootstrap
	 * class loader saw every platform class. In Java 9+ it was changed so the
	 * bootstrap class loader does not see all classes anymore. This might lead
	 * to ClassNotFoundException in formatters (e.g. freshmark).
	 *
	 * @return <code>null</code> on Java 8 (and minor), otherwise <code>PlatformClassLoader</code>
	 */
	@Nullable
	private static ClassLoader getParentClassLoader() {
		double version = Double.parseDouble(System.getProperty("java.specification.version"));
		if (version > 1.8) {
			try {
				return (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
			} catch (Exception e) {
				throw ThrowingEx.asRuntime(e);
			}
		} else {
			return null;
		}
	}
}
