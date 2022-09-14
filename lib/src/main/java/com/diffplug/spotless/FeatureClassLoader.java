/*
 * Copyright 2016-2022 DiffPlug
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
import java.util.Objects;

import javax.annotation.Nullable;

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
