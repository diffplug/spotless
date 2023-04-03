/*
 * Copyright 2016-2023 DiffPlug
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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Objects;

/**
 * This class loader is used to load classes of Spotless features from a search
 * path of URLs.<br/>
 * Features shall be independent from build tools. Hence the class loader of the
 * underlying build tool is e.g. skipped during the search for classes.<br/>
 *
 * For `com.diffplug.spotless.glue.`, classes are redefined from within the lib jar
 * but linked against the `Url[]`. This allows us to ship classfiles which function as glue
 * code but delay linking/definition to runtime after the user has specified which version
 * of the formatter they want.
 *
 *  For `"org.slf4j.` and (`com.diffplug.spotless.` but not `com.diffplug.spotless.extra.`)
 * 	the classes are loaded from the buildToolClassLoader.
 */
class FeatureClassLoader extends URLClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final ClassLoader buildToolClassLoader;

	/**
	 * Constructs a new FeatureClassLoader for the given URLs, based on an {@code URLClassLoader},
	 * using the system class loader as parent.
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
		if (name.startsWith("com.diffplug.spotless.glue.") || name.startsWith("com.diffplug.spotless.extra.glue.")) {
			var path = name.replace('.', '/') + ".class";
			var url = findResource(path);
			if (url == null) {
				throw new ClassNotFoundException(name);
			}
			try {
				return defineClass(name, urlToByteBuffer(url), (ProtectionDomain) null);
			} catch (IOException e) {
				throw new ClassNotFoundException(name, e);
			}
		} else if (useBuildToolClassLoader(name)) {
			return buildToolClassLoader.loadClass(name);
		} else {
			return super.findClass(name);
		}
	}

	private static boolean useBuildToolClassLoader(String name) {
		if (name.startsWith("org.slf4j.")) {
			return true;
		} else if (!name.startsWith("com.diffplug.spotless.extra") && name.startsWith("com.diffplug.spotless.")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public URL findResource(String name) {
		var resource = super.findResource(name);
		if (resource != null) {
			return resource;
		}
		return buildToolClassLoader.getResource(name);
	}

	private static ByteBuffer urlToByteBuffer(URL url) throws IOException {
		var buffer = new ByteArrayOutputStream();
		try (var inputStream = url.openStream()) {
			inputStream.transferTo(buffer);
		}
		buffer.flush();
		return ByteBuffer.wrap(buffer.toByteArray());
	}

	private static ClassLoader getParentClassLoader() {
		return ThrowingEx.get(() -> (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null));
	}
}
