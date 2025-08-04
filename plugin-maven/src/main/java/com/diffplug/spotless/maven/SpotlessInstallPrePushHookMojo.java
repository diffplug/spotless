/*
 * Copyright 2025 DiffPlug
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.diffplug.spotless.GitPrePushHookInstaller.GitPreHookLogger;
import com.diffplug.spotless.GitPrePushHookInstallerMaven;

/**
 * A Maven Mojo responsible for installing a Git pre-push hook for the Spotless plugin.
 * This hook ensures that Spotless formatting rules are automatically checked and applied
 * before performing a Git push operation.
 *
 * <p>The class leverages {@link GitPrePushHookInstallerMaven} to perform the installation process
 * and uses a Maven logger to log installation events and errors to the console.
 */
@Mojo(name = AbstractSpotlessMojo.GOAL_PRE_PUSH_HOOK, threadSafe = true)
public class SpotlessInstallPrePushHookMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Executes the Mojo, installing the Git pre-push hook for the Spotless plugin.
	 *
	 * <p>This method creates an instance of {@link GitPrePushHookInstallerMaven},
	 * providing a logger for logging the process of hook installation and any potential errors.
	 * The installation process runs in the root directory of the current Maven project.
	 *
	 * @throws MojoExecutionException if an error occurs during the installation process.
	 * @throws MojoFailureException   if the hook fails to install for any reason.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// if is not root project, skip it
		if (!project.isExecutionRoot()) {
			getLog().debug("Skipping Spotless pre-push hook installation for non-root project: " + project.getName());
			return;
		}

		final var logger = new GitPreHookLogger() {
			@Override
			public void info(String format, Object... arguments) {
				getLog().info(String.format(format, arguments));
			}

			@Override
			public void warn(String format, Object... arguments) {
				getLog().warn(String.format(format, arguments));
			}

			@Override
			public void error(String format, Object... arguments) {
				getLog().error(String.format(format, arguments));
			}
		};

		try {
			final var installer = new GitPrePushHookInstallerMaven(logger, project.getBasedir());
			installer.install();
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to install pre-push hook", e);
		}
	}
}
