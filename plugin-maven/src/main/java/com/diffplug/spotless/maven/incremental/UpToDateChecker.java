/*
 * Copyright 2021-2022 DiffPlug
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

import java.nio.file.Path;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.diffplug.spotless.Formatter;

public interface UpToDateChecker extends AutoCloseable {

	boolean isUpToDate(Path file);

	void setUpToDate(Path file);

	@Override
	void close();

	static UpToDateChecker noop(MavenProject project, Path indexFile, Log log) {
		return NoopChecker.create(project, indexFile, log);
	}

	static UpToDateChecker forProject(MavenProject project, Path indexFile, Iterable<Formatter> formatters, Log log) {
		return IndexBasedChecker.create(project, indexFile, formatters, log);
	}

	static UpToDateChecker wrapWithBuildContext(UpToDateChecker delegate, BuildContext buildContext) {
		return new UpToDateChecker() {

			@Override
			public void setUpToDate(Path file) {
				delegate.setUpToDate(file);
			}

			@Override
			public boolean isUpToDate(Path file) {
				if (buildContext.hasDelta(file.toFile())) {
					return delegate.isUpToDate(file);
				}
				return true;
			}

			@Override
			public void close() {
				delegate.close();
			}
		};
	}

}
