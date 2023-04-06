package com.diffplug.spotless.javascript;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.HashSet;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Platform;
import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.rome.RomeExecutableDownloader;

/**
 * formatter step that formats JavaScript and TypeScript code with Rome:
 * <a href= "https://github.com/rome/tools">https://github.com/rome/tools</a>.
 * It delegates to the Rome executable. The Rome executable is downloaded from
 * the network when no executable path is provided explicitly.
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

	/**
	 * Creates a formatter step with the current configuration, which formats code
	 * by passing it to the Rome executable.
	 * 
	 * @return A new formatter step for formatting with Rome.
	 */
	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	private State createState() throws IOException, InterruptedException {
		var resolvedPathToExe = resolveExe();
		var exeSignature = FileSignature.signAsList(Collections.singleton(new File(resolvedPathToExe)));
		makeExecutable(resolvedPathToExe);
		return new State(resolvedPathToExe, exeSignature);
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

	/**
	 * Creates a Rome step that uses an executable from the given path.
	 * 
	 * @param pathToExe Path to the Rome executable to use.
	 * @return A new Rome step that format with the given executable.
	 */
	public static RomeStep withExePath(String pathToExe) {
		return new RomeStep(null, pathToExe, null);
	}

	/**
	 * Creates a Rome step that downloads the Rome executable for the given version.
	 * 
	 * @param version     Version of the Rome executable to download.
	 * @param downloadDir Directory where to place the downloaded executable.
	 * @return A new Rome step that download the executable from the network.
	 */
	public static RomeStep withExeDownload(String version, String downloadDir) {
		return new RomeStep(version, null, downloadDir);
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
		private static final long serialVersionUID = -5884229077231467806L;

		/** Path to the exe file */
		final String pathToExe;

		/** The signature of the exe file, if any, used for caching. */
		final FileSignature exeSignature;

		State(String exe, FileSignature exeSignature) throws IOException {
			this.pathToExe = exe;
			this.exeSignature = exeSignature;
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
