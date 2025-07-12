package com.diffplug.spotless.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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

	/**
	 * The base directory of the Maven project where the Git pre-push hook will be installed.
	 * This parameter is automatically set to the root directory of the current project.
	 */
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File baseDir;

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
		final var logger = new GitPreHookLogger() {
			@Override
			public void info(String format, Object... arguments) {
				getLog().info(String.format(format, arguments));
			}

			@Override
			public void error(String format, Object... arguments) {
				getLog().error(String.format(format, arguments));
			}
		};

		try {
			final var installer = new GitPrePushHookInstallerMaven(logger, baseDir);
			installer.install();
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to install pre-push hook", e);
		}
	}
}
