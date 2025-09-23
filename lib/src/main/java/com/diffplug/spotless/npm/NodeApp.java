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

import static com.diffplug.spotless.npm.NpmProcessFactory.OnlinePreferrence.PREFER_OFFLINE;
import static com.diffplug.spotless.npm.NpmProcessFactory.OnlinePreferrence.PREFER_ONLINE;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.ProcessRunner;

public class NodeApp {

	private static final Logger logger = LoggerFactory.getLogger(NodeApp.class);

	private static final TimedLogger timedLogger = TimedLogger.forLogger(logger);

	@Nonnull
	protected final NodeServerLayout nodeServerLayout;

	@Nonnull
	protected final NpmConfig npmConfig;

	@Nonnull
	protected final NpmProcessFactory npmProcessFactory;

	@Nonnull
	protected final NpmFormatterStepLocations formatterStepLocations;

	public NodeApp(@Nonnull NodeServerLayout nodeServerLayout, @Nonnull NpmConfig npmConfig, @Nonnull NpmFormatterStepLocations formatterStepLocations) {
		this.nodeServerLayout = Objects.requireNonNull(nodeServerLayout);
		this.npmConfig = Objects.requireNonNull(npmConfig);
		this.npmProcessFactory = processFactory(formatterStepLocations);
		this.formatterStepLocations = Objects.requireNonNull(formatterStepLocations);
	}

	private static NpmProcessFactory processFactory(NpmFormatterStepLocations formatterStepLocations) {
		if (formatterStepLocations.cacheDir() != null) {
			logger.info("Caching npm install results in {}.", formatterStepLocations.cacheDir());
			return NodeModulesCachingNpmProcessFactory.create(formatterStepLocations.cacheDir());
		}
		logger.debug("Not caching npm install results.");
		return StandardNpmProcessFactory.INSTANCE;
	}

	boolean needsNpmInstall() {
		return !this.nodeServerLayout.isNodeModulesPrepared();
	}

	boolean needsPrepareNodeAppLayout() {
		return !this.nodeServerLayout.isLayoutPrepared();
	}

	void prepareNodeAppLayout() {
		timedLogger.withInfo("Preparing {} for npm step {}.", this.nodeServerLayout, getClass().getName()).run(() -> {
			NpmResourceHelper.assertDirectoryExists(nodeServerLayout.nodeModulesDir());
			NpmResourceHelper.writeUtf8StringToFile(nodeServerLayout.packageJsonFile(), this.npmConfig.getPackageJsonContent());
			if (this.npmConfig.getServeScriptContent() != null) {
				NpmResourceHelper.writeUtf8StringToFile(nodeServerLayout.serveJsFile(), this.npmConfig.getServeScriptContent());
			} else {
				NpmResourceHelper.deleteFileIfExists(nodeServerLayout.serveJsFile());
			}
			if (this.npmConfig.getNpmrcContent() != null) {
				NpmResourceHelper.writeUtf8StringToFile(nodeServerLayout.npmrcFile(), this.npmConfig.getNpmrcContent());
			} else {
				NpmResourceHelper.deleteFileIfExists(nodeServerLayout.npmrcFile());
			}
		});
	}

	void npmInstall() {
		timedLogger.withInfo("Installing npm dependencies for {} with {}.", this.nodeServerLayout, this.npmProcessFactory.describe())
				.run(this::optimizedNpmInstall);
	}

	private void optimizedNpmInstall() {
		try {
			npmProcessFactory.createNpmInstallProcess(nodeServerLayout, formatterStepLocations, PREFER_OFFLINE).waitFor();
		} catch (NpmProcessException e) {
			if (!offlineInstallFailed(e.getResult())) {
				throw e; // pass through
			}
			// if the npm install fails in a way that might be caused by missing dependency information, we try again without the offline flag
			npmProcessFactory.createNpmInstallProcess(nodeServerLayout, formatterStepLocations, PREFER_ONLINE).waitFor();
		}
	}

	private static boolean offlineInstallFailed(ProcessRunner.Result result) {
		if (result == null) {
			return false; // no result, something else must have happened
		}
		if (result.exitCode() == 0) {
			return false; // all is well
		}
		var installOutput = result.stdOutUtf8();
		// offline install failed, needs online install
		return isNoMatchingVersionFound(installOutput) || isCannotResolveDependencyTree(installOutput);
	}

	private static boolean isNoMatchingVersionFound(String installOutput) {
		return installOutput.contains("code ETARGET") && installOutput.contains("No matching version found for");
	}

	private static boolean isCannotResolveDependencyTree(String installOutput) {
		return installOutput.contains("code ERESOLVE") && installOutput.contains("unable to resolve dependency tree");
	}
}
