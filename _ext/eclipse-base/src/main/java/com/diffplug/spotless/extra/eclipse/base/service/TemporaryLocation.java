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
package com.diffplug.spotless.extra.eclipse.base.service;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.eclipse.osgi.service.datalocation.Location;

/** All files generated at runtime are stored in a temporary location. */
public class TemporaryLocation implements Location, AutoCloseable {
	private static final String TEMP_PREFIX = "com_diffplug_spotless_extra_eclipse";
	private final URL location;
	private Location parent;

	public TemporaryLocation() {
		this(null, createTemporaryDirectory());
	}

	private TemporaryLocation(Location parent, URL defaultValue) {
		this.location = defaultValue;
		this.parent = parent;
	}

	private static URL createTemporaryDirectory() {
		try {
			var location = Files.createTempDirectory(TEMP_PREFIX);
			return location.toUri().toURL();
		} catch (IOException e) {
			throw new IOError(e);
		}
	}

	@Override
	public boolean allowsDefault() {
		return false;
	}

	@Override
	public URL getDefault() {
		return null;
	}

	@Override
	public Location getParentLocation() {
		return parent;
	}

	@Override
	public URL getURL() {
		return location;
	}

	@Override
	public boolean isSet() {
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	@Deprecated
	public boolean setURL(URL value, boolean lock) throws IllegalStateException {
		throw new IllegalStateException("URL not modifyable.");
	}

	@Override
	public boolean set(URL value, boolean lock) throws IllegalStateException, IOException {
		throw new IllegalStateException("URL not modifyable.");
	}

	@Override
	public boolean set(URL value, boolean lock, String lockFilePath) throws IllegalStateException, IOException {
		throw new IllegalStateException("URL not modifyable.");
	}

	@Override
	public boolean lock() throws IOException {
		return false; //Lock not supported
	}

	@Override
	public void release() {
		//Lock not supported
	}

	@Override
	public boolean isLocked() throws IOException {
		return false; //Lock not supported
	}

	@Override
	public Location createLocation(Location parent, URL defaultValue, boolean readonly) {
		return new TemporaryLocation(parent, defaultValue);
	}

	@Override
	public URL getDataArea(String path) throws IOException {
		try {
			var locationPath = Paths.get(location.toURI());
			return locationPath.resolve(path).toUri().toURL();
		} catch (URISyntaxException e) {
			throw new IOException("Location not correctly formatted.", e);
		}
	}

	@Override
	public void close() throws Exception {
		try {
			var path = Path.of(location.toURI());
			Files.walk(path)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
			path.toFile().delete();
		} catch (IOException e) {
			//At shutdown everything is just done on best-efforts basis
		}
	}

}
