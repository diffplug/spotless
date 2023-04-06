package com.diffplug.spotless.javascript;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ForeignExe;
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
	private static final Logger logger = LoggerFactory.getLogger(RomeStep.class);

	private final String version;

	private final String pathToExe;

	private final String pathToExeDownloadDir;

	/**
	 * @return The name of this format step, i.e. {@code rome}.
	 */
	public static String name() {
		return "rome";
	}

	/**
	 * Creates a Rome step that format code by downloading to the given Rome
	 * version. The executable is downloaded from the network.
	 * 
	 * @param version     Version of the Rome executable to download.
	 * @param downloadDir Directory where to place the downloaded executable.
	 * @return A new Rome step that download the executable from the network.
	 */
	public static RomeStep withExeDownload(String version, String downloadDir) {
		version = version != null && !version.isBlank() ? version : defaultVersion();
		return new RomeStep(version, null, downloadDir);
	}

	/**
	 * Creates a Rome step that formats code by delegating to the Rome executable
	 * located at the given path.
	 * 
	 * @param pathToExe Path to the Rome executable to use.
	 * @return A new Rome step that format with the given executable.
	 */
	public static RomeStep withExePath(String pathToExe) {
		return new RomeStep(null, pathToExe, null);
	}

	/**
	 * Finds the default version for Rome when no version is specified explicitly.
	 * Over time this will become outdated -- people should always specify the
	 * version explicitly!
	 * 
	 * @return The default version for Rome.
	 */
	private static String defaultVersion() {
		return "12.0.0";
	}

	/**
	 * Attempts to add a POSIX permission to the given file, ignoring any errors.
	 * All existing permissions on the file are preserved and the new permission is
	 * added, if possible.
	 * 
	 * @param file       File or directory to which to add a permission.
	 * @param permission The POSIX permission to add.
	 */
	private static void attemptToAddPosixPermission(Path file, PosixFilePermission permission) {
		try {
			var newPermissions = new HashSet<>(Files.getPosixFilePermissions(file));
			newPermissions.add(permission);
			Files.setPosixFilePermissions(file, newPermissions);
		} catch (final Exception ignore) {
		}
	}

	/**
	 * Attempts to make the given file executable. This is a best-effort attempt,
	 * any errors are swallowed. Depending on the OS, the file might still be
	 * executable even if this method fails. The user will get a descriptive error
	 * later when we attempt to execute the Rome executable.
	 * 
	 * @param filePath Path to the file to make executable.
	 */
	private static void makeExecutable(String filePath) {
		var exePath = Paths.get(filePath);
		attemptToAddPosixPermission(exePath, PosixFilePermission.GROUP_EXECUTE);
		attemptToAddPosixPermission(exePath, PosixFilePermission.OTHERS_EXECUTE);
		attemptToAddPosixPermission(exePath, PosixFilePermission.OWNER_EXECUTE);
	}

	/**
	 * Finds the absolute path of a command on the user's path. Uses {@code which}
	 * for Linux and {@code where} for Windows.
	 * 
	 * @param name Name of the command to resolve.
	 * @return The absolute path of the command's executable.
	 * @throws IOException          When the command could not be resolved.
	 * @throws InterruptedException When this thread was interrupted while waiting
	 *                              to the which command to finish.
	 */
	private static String resolveNameAgainstPath(String name) throws IOException, InterruptedException {
		try (var runner = new ProcessRunner()) {
			var cmdWhich = runner.shellWinUnix("where " + name, "which " + name);
			if (cmdWhich.exitNotZero()) {
				throw new IOException("Unable to find " + name + " on path via command " + cmdWhich);
			} else {
				return cmdWhich.assertExitZero(Charset.defaultCharset()).trim();
			}
		}
	}

	private RomeStep(String version, String pathToExe, String pathToExeDownloadDir) {
		this.version = version;
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

	/**
	 * Resolves the Rome executable, possibly downloading it from the network, and
	 * creates a new state instance with the resolved executable that can format
	 * code via Rome.
	 * 
	 * @return The state instance for formatting code via Rome.
	 * @throws IOException          When any file system or network operations
	 *                              failed, such as when the Rome executable could
	 *                              not be downloaded, or when the given executable
	 *                              does not exist.
	 * @throws InterruptedException When the Rome executable needs to be downloaded
	 *                              and this thread was interrupted while waiting
	 *                              for the download to complete.
	 */
	private State createState() throws IOException, InterruptedException {
		var resolvedPathToExe = resolveExe();
		var resolvedExeFile = new File(resolvedPathToExe);
		if (!resolvedExeFile.isFile()) {
			throw new IllegalArgumentException("Rome executable does not exist: " + resolvedExeFile);
		}
		logger.debug("Using Rome executable located at  '{}'", resolvedPathToExe);
		var exeSignature = FileSignature.signAsList(Collections.singleton(resolvedExeFile));
		makeExecutable(resolvedPathToExe);
		return new State(resolvedPathToExe, exeSignature);
	}

	/**
	 * Resolves the path to the Rome executable, given the configuration of this
	 * step. When the path to the Rome executable is given explicitly, that path is
	 * used as-is. Otherwise, at attempt is made to download the Rome executable for
	 * the configured version from the network, unless it was already downloaded and
	 * is available in the cache.
	 * 
	 * @return The path to the resolved Rome executable.
	 * @throws IOException          When any file system or network operations
	 *                              failed, such as when the Rome executable could
	 *                              not be downloaded.
	 * @throws InterruptedException When the Rome executable needs to be downloaded
	 *                              and this thread was interrupted while waiting
	 *                              for the download to complete.
	 */
	private String resolveExe() throws IOException, InterruptedException {
		new ForeignExe();
		if (pathToExe != null) {
			if (Paths.get(pathToExe).getNameCount() == 1) {
				return resolveNameAgainstPath(pathToExe);
			} else {
				return pathToExe;
			}
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

	private static class State implements Serializable {
		private static final long serialVersionUID = -5884229077231467806L;

		/** Path to the exe file */
		private final String pathToExe;

		/** The signature of the exe file, if any, used for caching. */
		@SuppressWarnings("unused")
		private final FileSignature exeSignature;

		private State(String exe, FileSignature exeSignature) throws IOException {
			this.pathToExe = exe;
			this.exeSignature = exeSignature;
		}

		/**
		 * Formats the given piece of code by delegating to the Rome executable. The
		 * code is passed to Rome via stdin, the file name is used by Rome only to
		 * determine the code syntax (e.g. JavaScript or TypeScript).
		 * 
		 * @param runner Process runner for invoking the Rome executable.
		 * @param input  Code to format.
		 * @param file   File to format.
		 * @return The formatted code.
		 * @throws IOException          When a file system error occurred while
		 *                              executing Rome.
		 * @throws InterruptedException When this thread was interrupted while waiting
		 *                              for Rome to finish formatting.
		 */
		private String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			var stdin = input.getBytes(StandardCharsets.UTF_8);
			var args = new String[] { pathToExe, "format", "--stdin-file-path", file.getName() };
			if (logger.isDebugEnabled()) {
				logger.debug("Running Rome comand to fomrat code: '{}'", String.join(", ", args));
			}
			return runner.exec(stdin, args).assertExitZero(StandardCharsets.UTF_8);
		}

		private FormatterFunc.Closeable toFunc() {
			var runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
