/*
 * Copyright 2023 DiffPlug
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

import java.io.File;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.ProcessRunner.Result;

public class NodeModulesCachingNpmProcessFactory implements NpmProcessFactory {

	private static final Logger logger = LoggerFactory.getLogger(NodeModulesCachingNpmProcessFactory.class);

	private static final TimedLogger timedLogger = TimedLogger.forLogger(logger);

	private final File cacheDir;

	private final ShadowCopy shadowCopy;

	private NodeModulesCachingNpmProcessFactory(@Nonnull File cacheDir) {
		this.cacheDir = Objects.requireNonNull(cacheDir);
		assertDir(cacheDir);
		this.shadowCopy = new ShadowCopy(cacheDir);
	}

	private void assertDir(File cacheDir) {
		if (cacheDir.exists() && !cacheDir.isDirectory()) {
			throw new IllegalArgumentException("Cache dir must be a directory");
		}
		if (!cacheDir.exists()) {
			if (!cacheDir.mkdirs()) {
				throw new IllegalArgumentException("Cache dir could not be created.");
			}
		}
	}

	public static NodeModulesCachingNpmProcessFactory create(@Nonnull File cacheDir) {
		return new NodeModulesCachingNpmProcessFactory(cacheDir);
	}

	@Override
	public NpmProcess createNpmInstallProcess(NodeServerLayout nodeServerLayout, NpmFormatterStepLocations formatterStepLocations, OnlinePreferrence onlinePreferrence) {
		NpmProcess actualNpmInstallProcess = StandardNpmProcessFactory.INSTANCE.createNpmInstallProcess(nodeServerLayout, formatterStepLocations, onlinePreferrence);
		return new CachingNmpInstall(actualNpmInstallProcess, nodeServerLayout);
	}

	@Override
	public NpmLongRunningProcess createNpmServeProcess(NodeServerLayout nodeServerLayout, NpmFormatterStepLocations formatterStepLocations) {
		return StandardNpmProcessFactory.INSTANCE.createNpmServeProcess(nodeServerLayout, formatterStepLocations);
	}

	private class CachingNmpInstall implements NpmProcess {

		private final NpmProcess actualNpmInstallProcess;
		private final NodeServerLayout nodeServerLayout;

		public CachingNmpInstall(NpmProcess actualNpmInstallProcess, NodeServerLayout nodeServerLayout) {
			this.actualNpmInstallProcess = actualNpmInstallProcess;
			this.nodeServerLayout = nodeServerLayout;
		}

		@Override
		public Result waitFor() {
			String entryName = entryName();
			if (shadowCopy.entryExists(entryName, NodeServerLayout.NODE_MODULES)) {
				timedLogger.withInfo("Using cached node_modules for {} from {}", entryName, cacheDir)
						.run(() -> shadowCopy.copyEntryInto(entryName(), NodeServerLayout.NODE_MODULES, nodeServerLayout.nodeModulesDir()));
				return new CachedResult();
			} else {
				Result result = timedLogger.withInfo("calling actual npm install {}", actualNpmInstallProcess.describe())
						.call(actualNpmInstallProcess::waitFor);
				assert result.exitCode() == 0;
				storeShadowCopy(entryName);
				return result;
			}
		}

		private void storeShadowCopy(String entryName) {
			timedLogger.withInfo("Caching node_modules for {} in {}", entryName, cacheDir)
					.run(() -> shadowCopy.addEntry(entryName(), new File(nodeServerLayout.nodeModulesDir(), NodeServerLayout.NODE_MODULES)));
		}

		private String entryName() {
			return nodeServerLayout.nodeModulesDir().getName();
		}

		@Override
		public String describe() {
			return String.format("Wrapper around [%s] to cache node_modules in [%s]", actualNpmInstallProcess.describe(), cacheDir.getAbsolutePath());
		}
	}

	private class CachedResult extends Result {

		public CachedResult() {
			super(List.of("(from cache dir " + cacheDir + ")"), 0, new byte[0], new byte[0]);
		}
	}
}
