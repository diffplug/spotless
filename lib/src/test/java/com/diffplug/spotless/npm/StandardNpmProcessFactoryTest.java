/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.npm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StandardNpmProcessFactoryTest {

	@Test
	void npmServeCommandLineOmitsRemovedScriptsPrependNodePathFlag(@TempDir Path tmp) {
		File npm = tmp.resolve("npm").toFile();
		File node = tmp.resolve("node").toFile();
		File project = tmp.resolve("project").toFile();
		File build = tmp.resolve("build").toFile();
		project.mkdirs();
		build.mkdirs();

		NpmFormatterStepLocations locations = new NpmFormatterStepLocations(
				project,
				build,
				null,
				new NpmPathResolver(npm, node, null, List.of()));
		NodeServerLayout layout = new NodeServerLayout(build, "{\"name\":\"spotless-prettier\"}", "console.log('hi');");
		UUID serverId = UUID.fromString("00000000-0000-0000-0000-000000000001");

		NpmLongRunningProcess process = StandardNpmProcessFactory.INSTANCE.createNpmServeProcess(layout, locations, serverId);

		assertThat(process.describe())
				.contains("start")
				.contains("--node-server-instance-id=" + serverId)
				.doesNotContain("scripts-prepend-node-path");
	}
}
