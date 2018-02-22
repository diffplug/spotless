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
package com.diffplug.spotless.maven;

import static com.diffplug.common.base.Strings.*;

import java.io.File;

import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.util.FileUtils;

public class FileLocator {

	private final ResourceManager resourceManager;

	public FileLocator(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	public File locateFile(String path) {
		if (isNullOrEmpty(path)) {
			return null;
		}

		String outputFile = tmpOutputFileName(path);
		try {
			return resourceManager.getResourceAsFile(path, outputFile);
		} catch (ResourceNotFoundException e) {
			throw new RuntimeException("Unable to locate file with path: " + path, e);
		} catch (FileResourceCreationException e) {
			throw new RuntimeException("Unable to create temporaty file '" + outputFile + "' in the output directory", e);
		}
	}

	private static String tmpOutputFileName(String path) {
		String nameWithExtension = FileUtils.filename(path);
		String extension = FileUtils.extension(path);
		String name = nameWithExtension.replace('.' + extension, "");
		return name + '-' + System.currentTimeMillis() + '.' + extension;
	}
}
