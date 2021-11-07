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
package com.diffplug.spotless.maven.incremental;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.diffplug.spotless.Formatter;

public interface UpToDateChecker extends AutoCloseable {

	boolean isUpToDate(File file);

	void setUpToDate(File file);

	void close();

	static UpToDateChecker noop() {
		return NoopChecker.INSTANCE;
	}

	static UpToDateChecker forProject(MavenProject project, Iterable<Formatter> formatters, Log log) {
		return IndexBasedChecker.create(project, formatters, log);
	}
}
