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
package com.diffplug.gradle.spotless;

import java.io.File;

import org.gradle.api.tasks.TaskAction;

public class ApplyFormatTask extends BaseFormatTask {
	@TaskAction
	public void apply() throws Exception {
		Formatter formatter = buildFormatter();
		for (File file : target) {
			getLogger().debug("Applying format to " + file);
			// keep track of the problem toFormat
			if (paddedCell) {
				PaddedCellTaskMisc.apply(this, formatter, file);
			} else {
				formatter.applyFormat(file);
			}
		}
	}
}
