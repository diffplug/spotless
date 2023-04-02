package com.diffplug.spotless.javascript;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Platform;
import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.rome.RomeExecutableDownloader;

/**
 * formatter step that formats JavaScript and TypeScript code with Rome:
 * <a href= "https://github.com/rome/tools">https://github.com/rome/tools</a>.
 * It delegates to the Rome executable.
 */
public class RomeStep {
	public static String name() {
		return "rome";
	}

	private final String version;

	private final String pathToExe;

	private final String pathToExeDownloadDir;

	private RomeStep(String version, String pathToExe, String pathToExeDownloadDir) {
		this.version = version != null && !version.isBlank() ? version : RomeStep.defaultVersion();
		this.pathToExe = pathToExe;
		this.pathToExeDownloadDir = pathToExeDownloadDir;
	}

	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	private State createState() throws IOException, InterruptedException {
		var resolvedPathToExe = resolveExe();
		makeExecutable(resolvedPathToExe);
		return new State(this, resolvedPathToExe);
	}

	private String resolveExe() throws IOException, InterruptedException {
		if (pathToExe != null) {
			return pathToExe;
		} else {
			var downloader = new RomeExecutableDownloader(Paths.get(pathToExeDownloadDir));
			var platform = Platform.guess();
			if (!downloader.isSupported(platform)) {
				throw new IllegalStateException(
						"Unsupported platform " + platform + ", please specifiy the Rome executable directly");
			}
			var downloaded = downloader.ensureDownloaded(version, platform).toString();
			makeExecutable(downloaded);
			return downloaded;
		}
	}

	public static RomeStep withVersionAndExe(String version, String pathToExe) {
		return new RomeStep(version, pathToExe, null);
	}

	public static RomeStep withVersionAndExeDownload(String version, String pathToExeDownloadDir) {
		return new RomeStep(version, null, pathToExeDownloadDir);
	}

	private static String defaultVersion() {
		return "12.0.0";
	}

	private static void makeExecutable(String exe) {
		var exePath = Paths.get(exe);
		addPosixPermission(exePath, PosixFilePermission.GROUP_EXECUTE);
		addPosixPermission(exePath, PosixFilePermission.OTHERS_EXECUTE);
		addPosixPermission(exePath, PosixFilePermission.OWNER_EXECUTE);
	}

	private static boolean addPosixPermission(Path file, PosixFilePermission permission) {
		try {
			var newPermissions = new HashSet<>(Files.getPosixFilePermissions(file));
			newPermissions.add(permission);
			Files.setPosixFilePermissions(file, newPermissions);
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

	static class State implements Serializable {
		private static final long serialVersionUID = -1825662356883926318L;

		// used for up-to-date checks and caching
		final String version;

		final String pathToExe;

		State(RomeStep step, String exe) {
			this.version = step.version;
			this.pathToExe = exe;
		}

		String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			var stdin = input.getBytes(StandardCharsets.UTF_8);
			var args = new String[] { pathToExe, "format", "--stdin-file-path", file.getName() };
			return runner.exec(stdin, args).assertExitZero(StandardCharsets.UTF_8);
		}

		FormatterFunc.Closeable toFunc() {
			var runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
