/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.core;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class CliJarProvisioner implements Provisioner {

	public static final CliJarProvisioner INSTANCE = new CliJarProvisioner();

	public static final File OWN_JAR = createSentinelFile();

	public CliJarProvisioner() {
		JarState.setOverrideClassLoader(getClass().getClassLoader()); // use the classloader of this class
		// TODO (simschla, 11.11.2024): THIS IS A HACK, replace with proper solution
	}

	private static File createSentinelFile() {
		try {
			File file = File.createTempFile("spotless-cli", ".jar");
			Files.write(file.toPath(), List.of("@@@@PLACEHOLDER_FOR_OWN_JAR@@@@"), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
			file.deleteOnExit();
			return file;
		} catch (Exception e) {
			throw new RuntimeException("Could not create sentinel file", e);
		}
	}

	@Override
	public Set<File> provisionWithTransitives(boolean withTransitives, Collection<String> mavenCoordinates) {
		return Set.of(OWN_JAR);
	}

}
