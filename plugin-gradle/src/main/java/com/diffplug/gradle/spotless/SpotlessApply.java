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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.TaskAction;

public abstract class SpotlessApply extends SpotlessTaskService.ClientTask {
	@TaskAction
	public void performAction() {
		getTaskService().get().registerApplyAlreadyRan(this);
		ConfigurableFileTree files = getConfigCacheWorkaround().fileTree().from(getSpotlessOutDirectory().get());
		if (files.isEmpty()) {
			getState().setDidWork(sourceDidWork());
		} else {
			files.visit(new FileVisitor() {
				@Override
				public void visitDir(FileVisitDetails fileVisitDetails) {

				}

				@Override
				public void visitFile(FileVisitDetails fileVisitDetails) {
					String path = fileVisitDetails.getPath();
					File originalSource = new File(getProjectDir().get().getAsFile(), path);
					try {
						getLogger().debug("Copying " + fileVisitDetails.getFile() + " to " + originalSource);
						Files.copy(fileVisitDetails.getFile().toPath(), originalSource.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}
}
