/*
 * Copyright 2021-2023 DiffPlug
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
package com.diffplug.spotless.maven.incremental;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Objects;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import com.diffplug.spotless.Formatter;

/**
 * Represents a particular Spotless Maven plugin setup using a Base64-encoded serialized form of:
 * <ol>
 *    <li>Plugin version as configured in the POM</li>
 *    <li>Formatter instances created according to the POM configuration</li>
 * </ol>
 */
class PluginFingerprint {

	private static final String SPOTLESS_PLUGIN_KEY = "com.diffplug.spotless:spotless-maven-plugin";

	private final String value;

	private PluginFingerprint(String value) {
		this.value = value;
	}

	static PluginFingerprint from(MavenProject project, Iterable<Formatter> formatters) {
		Plugin spotlessPlugin = project.getPlugin(SPOTLESS_PLUGIN_KEY);
		if (spotlessPlugin == null) {
			throw new IllegalArgumentException("Spotless plugin absent from the project: " + project);
		}
		byte[] digest = digest(spotlessPlugin, formatters);
		String value = Base64.getEncoder().encodeToString(digest);
		return new PluginFingerprint(value);
	}

	static PluginFingerprint from(String value) {
		return new PluginFingerprint(value);
	}

	static PluginFingerprint empty() {
		return new PluginFingerprint("");
	}

	String value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PluginFingerprint that = (PluginFingerprint) o;
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return "PluginFingerprint[" + value + "]";
	}

	private static byte[] digest(Plugin plugin, Iterable<Formatter> formatters) {
		try (ObjectDigestOutputStream out = ObjectDigestOutputStream.create()) {
			out.writeObject(plugin.getVersion());
			for (Formatter formatter : formatters) {
				out.writeObject(formatter);
			}
			out.flush();
			return out.digest();
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to serialize plugin " + plugin, e);
		}
	}
}
