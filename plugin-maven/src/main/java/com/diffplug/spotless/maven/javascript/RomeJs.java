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
	 * Optional directory where the downloaded Rome executable is placed. If this is
	 * a relative path, it is resolved against the project's base directory.
	 * Defaults to
	 * <code>~/.m2/repository/com/diffplug/spotless/spotless-data/rome</code>.
	 * <p>
	 * You can use an expression like <code>${user.home}/rome</code> if you want to
	 * use the home directory, or <code>${project.build.directory</code> if you want
	 * to use the ptarget directory of the current project.
	 */
	@Parameter
	private String downloadDir;

	/**
	 * Optional path to the Rome executable. Either a <code>version</code> or a
	 * <code>pathToExe</code> should be specified. When not given, an attempt is
	 * made to download the executable for the given version from the network. When
	 * given, the executable is used and the <code>version</code> parameter is
	 * ignored.
	 */
	@Parameter
	private String pathToExe;

	/**
	 * Rome version to download, applies only when no <code>pathToExe</code> is
	 * specified explicitly. Either a <code>version</code> or a
	 * <code>pathToExe</code> should be specified. When not given, a default known
	 * version is used. For stable builds, it is recommended that you always set the
	 * version explicitly. This parameter is ignored when you specify a
	 * <code>pathToExe</code> explicitly.
	 */
	@Parameter
	private String version;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		RomeStep rome;
		if (pathToExe != null) {
			rome = RomeStep.withExePath(pathToExe);
		} else {
			var downloadDir = resolveDownloadDir(config);
			rome = RomeStep.withExeDownload(version, downloadDir);
		}
		return rome.create();
	}

	/**
	 * Resolves the directory to use for storing downloaded Rome executable. When a
	 * {@link #downloadDir} is given, use that directory, resolved against the
	 * current project's directory. Otherwise, use the {@code Rome} sub folder in
	 * the shared data directory.
	 * 
	 * @param config Configuration for this step.
	 * @return The download directory for the Rome executable.
	 */
	private String resolveDownloadDir(FormatterStepConfig config) {
		final var fileLocator = config.getFileLocator();
		if (downloadDir != null && !downloadDir.isBlank()) {
			return fileLocator.getBaseDir().toPath().resolve(downloadDir).toAbsolutePath().toString();
		} else {
			return fileLocator.getDataDir().toPath().resolve("rome").toString();
		}
	}
}
