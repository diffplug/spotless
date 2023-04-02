package com.diffplug.spotless.maven.javascript;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.javascript.RomeStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

/**
 * Factory for creating the Rome formatter step that formats JavaScript and
 * TypeScript code with Rome:
 * <a href= "https://github.com/rome/tools">https://github.com/rome/tools</a>.
 * It delegates to the Rome executable.
 */
public class RomeJs implements FormatterStepFactory {
	/**
	 * Optional directory where the downloaded Rome executable is placed. This
	 * defaults to <code>${project.buildDir}/spotless/rome</code>. You may want to
	 * change this to a directory outside the build directory to preserve downloaded
	 * files even when cleaning the project.
	 */
	@Parameter
	private String downloadDir;

	/**
	 * Optional path to the Rome executable. When not given, an attempt is made to
	 * download the executable for the given version from the network.
	 */
	@Parameter
	private String pathToExe;

	/**
	 * Rome version. When not given, a default known version is used. For stable
	 * builds, it is recommended that you always set the version explicitly. This
	 * parameter is ignored when you specify a <code>pathToExe</code> explicitly.
	 */
	@Parameter
	private String version;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		RomeStep rome;
		if (pathToExe != null) {
			rome = RomeStep.withVersionAndExe(version, pathToExe);
		} else {
			var downloadDir = resolveDownloadDir(config);
			rome = RomeStep.withVersionAndExeDownload(version, downloadDir);
		}
		return rome.create();
	}

	private String resolveDownloadDir(FormatterStepConfig config) {
		// e.g. /home/user/.m2/repository/com/diffplug/spotless/spotless-maven-plugin/2.35.1-SNAPSHOT/spotless-maven-plugin-2.35.1-SNAPSHOT.jar
		// var self = config.getProvisioner().provisionWithTransitives(false, "com.diffplug.spotless:spotless-maven-plugin:2.35.1-SNAPSHOT");
		// e.g. /home/user/.m2/repository/com/diffplug/spotless/spotless-maven-plugin
		// return self.iterator().next().getParentFile().getParent();
		var buildDir = config.getFileLocator().getBuildDir().toPath();
		return buildDir.resolve("spotless").resolve("rome").toAbsolutePath().toString();
	}
}
