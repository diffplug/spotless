/*
 * Copyright 2021 DiffPlug
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
package com.diffplug.spotless.pom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Vector;

public class DelegatingClassLoader extends ClassLoader {

	private final ClassLoader[] delegateClassLoaders;

	public DelegatingClassLoader(ClassLoader... delegateClassLoaders) {
		super(null);
		this.delegateClassLoaders = delegateClassLoaders;
	}

	protected Class<?> findClass(String name) throws ClassNotFoundException {
		String path = name.replace('.', '/') + ".class";
		URL url = findResource(path);
		if (url == null) {
			throw new ClassNotFoundException(name);
		}
		try {
			ByteBuffer byteCode = loadResource(url);
			return defineClass(name, byteCode, null);
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	private ByteBuffer loadResource(URL url) throws IOException {
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

	protected URL findResource(String name) {
		for (ClassLoader delegate : delegateClassLoaders) {
			URL resource = delegate.getResource(name);
			if (resource != null) {
				return resource;
			}
		}
		return null;
	}

	protected Enumeration<URL> findResources(String name) throws IOException {
		Vector<URL> vector = new Vector<>();
		for (ClassLoader delegate : delegateClassLoaders) {
			Enumeration<URL> enumeration = delegate.getResources(name);
			while (enumeration.hasMoreElements()) {
				vector.add(enumeration.nextElement());
			}
		}
		return vector.elements();
	}

}
