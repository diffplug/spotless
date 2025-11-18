/*
 * Copyright 2023-2025 DiffPlug
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

import java.util.UUID;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.ProcessRunner;

public class NodeServeApp extends NodeApp {

	private static final Logger LOGGER = LoggerFactory.getLogger(NodeApp.class);

	private static final TimedLogger TIMED_LOGGER = TimedLogger.forLogger(LOGGER);

	public NodeServeApp(@Nonnull NodeServerLayout nodeServerLayout, @Nonnull NpmConfig npmConfig, @Nonnull NpmFormatterStepLocations formatterStepLocations) {
		super(nodeServerLayout, npmConfig, formatterStepLocations);
	}

	ProcessRunner.LongRunningProcess startNpmServeProcess(UUID nodeServerInstanceId) {
		return TIMED_LOGGER.withInfo("Starting npm based server in {} with {}.", this.nodeServerLayout.nodeModulesDir(), this.npmProcessFactory.describe())
				.call(() -> npmProcessFactory.createNpmServeProcess(nodeServerLayout, formatterStepLocations, nodeServerInstanceId).start());
	}

}
