/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.biome;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloader for the Biome executable:
 * <a href="https://github.com/biomejs/biome">https://github.com/biomejs/biome</a>.
 */
final class BiomeExecutableDownloader {
	private static final Logger LOGGER = LoggerFactory.getLogger(BiomeExecutableDownloader.class);

	/**
	 * The checksum algorithm to use for checking the integrity of downloaded files.
	 */
	private static final String CHECKSUM_ALGORITHM = "SHA-256";

	/**
	 * The pattern for {@link String#formatted(Object...) String.format()} for
	 * the platform part of the Biome executable download URL. First parameter is the
	 * OS, second parameter the architecture, the third the file extension.
	 */
	private static final String PLATFORM_PATTERN = "%s-%s%s";

	/**
	 * {@link OpenOption Open options} for reading an existing file without write
	 * access.
	 */
	private static final OpenOption[] READ_OPTIONS = {StandardOpenOption.READ};

	/**
	 * {@link OpenOption Open options} for creating a new file, overwriting the
	 * existing file if present.
	 */
	private static final OpenOption[] WRITE_OPTIONS = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
			StandardOpenOption.WRITE};

	private final Path downloadDir;

	/**
	 * Creates a new downloader for the Biome executable. The executable files are
	 * stored in the given download directory.
	 *
	 * @param downloadDir Directory where to store the downloaded executable.
	 */
	public BiomeExecutableDownloader(Path downloadDir) {
		this.downloadDir = downloadDir;
	}

	/**
	 * Downloads the Biome executable for the current platform from the network to
	 * the download directory. When the executable exists already, it is
	 * overwritten.
	 *
	 * @param version Desired Biome version.
	 * @return The path to the Biome executable.
	 * @throws IOException           When the executable cannot be downloaded from
	 *                               the network or the file system could not be
	 *                               accessed.
	 * @throws InterruptedException  When this thread was interrupted while
	 *                               downloading the file.
	 * @throws IllegalStateException When no information about the current OS and
	 *                               architecture could be obtained, or when the OS
	 *                               or architecture is not supported.
	 */
	public Path download(String version) throws IOException, InterruptedException {
		var platform = Platform.guess();
		var url = getDownloadUrl(version, platform);
		var executablePath = getExecutablePath(version, platform);
		var checksumPath = getChecksumPath(executablePath);
		var executableDir = executablePath.getParent();
		if (executableDir != null) {
			Files.createDirectories(executableDir);
		}
		LOGGER.info("Attempting to download Biome from '{}' to '{}'", url, executablePath);
		var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
		var handler = BodyHandlers.ofFile(executablePath, WRITE_OPTIONS);
		var response = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build().send(request, handler);
		if (response.statusCode() != 200) {
			throw new IOException("Failed to download file from " + url + ", server returned " + response.statusCode());
		}
		var downloadedFile = response.body();
		if (!Files.exists(downloadedFile) || Files.size(downloadedFile) == 0) {
			throw new IOException("Failed to download file from " + url + ", file is empty or does not exist");
		}
		writeChecksumFile(downloadedFile, checksumPath);
		LOGGER.debug("Biome was downloaded successfully to '{}'", downloadedFile);
		return downloadedFile;
	}

	/**
	 * Ensures that the Biome executable for the current platform exists in the
	 * download directory. When the executable does not exist in the download
	 * directory, an attempt is made to download the Biome executable from the
	 * network. When the executable exists already, no attempt to download it again
	 * is made.
	 *
	 * @param version Desired Biome version.
	 * @return The path to the Biome executable.
	 * @throws IOException           When the executable cannot be downloaded from
	 *                               the network or the file system could not be
	 *                               accessed.
	 * @throws InterruptedException  When this thread was interrupted while
	 *                               downloading the file.
	 * @throws IllegalStateException When no information about the current OS and
	 *                               architecture could be obtained, or when the OS
	 *                               or architecture is not supported.
	 */
	public Path ensureDownloaded(String version) throws IOException, InterruptedException {
		var platform = Platform.guess();
		LOGGER.debug("Ensuring that Biome for platform '{}' is downloaded", platform);
		var existing = findDownloaded(version);
		if (existing.isPresent()) {
			LOGGER.debug("Biome was already downloaded, using executable at '{}'", existing.orElseThrow());
			return existing.orElseThrow();
		} else {
			LOGGER.debug("Biome was not yet downloaded, attempting to download executable");
			return download(version);
		}
	}

	/**
	 * Attempts to find the Biome executable for the current platform in the download
	 * directory. No attempt is made to download the executable from the network.
	 *
	 * @param version Desired Biome version.
	 * @return The path to the Biome executable.
	 * @throws IOException           When the executable does not exists in the
	 *                               download directory, or when the file system
	 *                               could not be accessed.
	 * @throws IllegalStateException When no information about the current OS and
	 *                               architecture could be obtained, or when the OS
	 *                               or architecture is not supported.
	 */
	public Optional<Path> findDownloaded(String version) throws IOException {
		var platform = Platform.guess();
		var executablePath = getExecutablePath(version, platform);
		LOGGER.debug("Checking Biome executable at {}", executablePath);
		return checkFileWithChecksum(executablePath) ? Optional.ofNullable(executablePath) : Optional.empty();
	}

	/**
	 * Checks whether the given file exists and matches the checksum. The checksum
	 * must be contained in a file next to the file to check.
	 *
	 * @param filePath File to check.
	 * @return <code>true</code> if the file exists and matches the checksum,
	 *         <code>false</code> otherwise.
	 */
	private boolean checkFileWithChecksum(Path filePath) {
		if (!Files.exists(filePath)) {
			LOGGER.debug("File '{}' does not exist yet", filePath);
			return false;
		}
		if (Files.isDirectory(filePath)) {
			LOGGER.debug("File '{}' exists, but is a directory", filePath);
			return false;
		}
		var checksumPath = getChecksumPath(filePath);
		if (!Files.exists(checksumPath)) {
			LOGGER.debug("File '{}' exists, but checksum file '{}' does not", filePath, checksumPath);
			return false;
		}
		if (Files.isDirectory(checksumPath)) {
			LOGGER.debug("Checksum file '{}' exists, but is a directory", checksumPath);
			return false;
		}
		try {
			var actualChecksum = computeChecksum(filePath, CHECKSUM_ALGORITHM);
			var expectedChecksum = readTextFile(checksumPath, ISO_8859_1);
			LOGGER.debug("Expected checksum: {}, actual checksum: {}", expectedChecksum, actualChecksum);
			return Objects.equals(expectedChecksum, actualChecksum);
		} catch (final IOException ignored) {
			return false;
		}
	}

	/**
	 * Computes the checksum of the given file.
	 *
	 * @param file      File to process.
	 * @param algorithm The checksum algorithm to use.
	 * @return The checksum of the given file.
	 * @throws IOException When the file does not exist or could not be read.
	 */
	private String computeChecksum(Path file, String algorithm) throws IOException {
		var buffer = new byte[4192];
		try (var in = Files.newInputStream(file, READ_OPTIONS)) {
			var digest = MessageDigest.getInstance(algorithm);
			int result;
			while ((result = in.read(buffer, 0, buffer.length)) != -1) {
				digest.update(buffer, 0, result);
			}
			var bytes = digest.digest();
			return String.format("%0" + (bytes.length * 2) + "X", new BigInteger(1, bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Finds the code name for the given operating system used by the Biome
	 * executable download URL.
	 *
	 * @param architecture Desired operating system architecture.
	 * @return Code name for the Biome download URL.
	 * @throws IOException When the given OS is not supported by Biome.
	 */
	private String getArchitectureCodeName(Architecture architecture) throws IOException {
		switch (architecture) {
		case ARM64:
			return "arm64";
		case X64:
			return "x64";
		default:
			throw new IOException("Unsupported architecture: " + architecture);
		}
	}

	/**
	 * Derives a path for the file which contains the checksum of the given file.
	 *
	 * @param file A file for which to derive the checksum file path.
	 * @return The path with the checksum for the given file.
	 */
	private Path getChecksumPath(Path file) {
		var parent = file.getParent();
		var base = parent != null ? parent : file;
		var fileName = file.getFileName();
		var checksumName = fileName != null ? fileName + ".sha256" : "checksum.sha256";
		return base.resolve(checksumName);
	}

	/**
	 * Finds the URL from which the Biome executable can be downloaded.
	 *
	 * @param version  Desired Biome version.
	 * @param platform Desired platform.
	 * @return The URL for the Biome executable.
	 * @throws IOException When the platform is not supported by Biome.
	 */
	private String getDownloadUrl(String version, Platform platform) throws IOException {
		var osCodeName = getOsCodeName(platform.getOs());
		var architectureCodeName = getArchitectureCodeName(platform.getArchitecture());
		var extension = getDownloadUrlExtension(platform.getOs());
		var platformString = PLATFORM_PATTERN.formatted(osCodeName, architectureCodeName, extension);
		return BiomeSettings.getUrlPattern(version).formatted(version, platformString);
	}

	/**
	 * Finds the file extension of the Biome download URL for the given operating
	 * system.
	 *
	 * @param os Desired operating system.
	 * @return Extension for the Biome download URL.
	 * @throws IOException When the given OS is not supported by Biome.
	 */
	private String getDownloadUrlExtension(OS os) throws IOException {
		switch (os) {
		case LINUX:
			return "";
		case MAC_OS:
			return "";
		case WINDOWS:
			return ".exe";
		default:
			throw new IOException("Unsupported OS: " + os);
		}
	}

	/**
	 * Finds the path on the file system for the Biome executable with a given
	 * version and platform.
	 *
	 * @param version  Desired Biome version.
	 * @param platform Desired platform.
	 * @return The path for the Biome executable.
	 */
	private Path getExecutablePath(String version, Platform platform) {
		var os = platform.getOs().name().toLowerCase(Locale.ROOT);
		var arch = platform.getArchitecture().name().toLowerCase(Locale.ROOT);
		var fileName = BiomeSettings.getDownloadFilePattern().formatted(os, arch, version);
		return downloadDir.resolve(fileName);
	}

	/**
	 * Finds the code name for the given operating system used by the Biome
	 * executable download URL.
	 *
	 * @param os Desired operating system.
	 * @return Code name for the Biome download URL.
	 * @throws IOException When the given OS is not supported by Biome.
	 */
	private String getOsCodeName(OS os) throws IOException {
		switch (os) {
		case LINUX:
			return "linux";
		case MAC_OS:
			return "darwin";
		case WINDOWS:
			return "win32";
		default:
			throw new IOException("Unsupported OS: " + os);
		}
	}

	/**
	 * Reads a plain text file with the given encoding into a string.
	 *
	 * @param file    File to read.
	 * @param charset Encoding to use.
	 * @return The contents of the file as a string.
	 * @throws IOException When the file could not be read.
	 */
	private String readTextFile(Path file, Charset charset) throws IOException {
		try (var in = Files.newInputStream(file, READ_OPTIONS)) {
			return new String(in.readAllBytes(), charset);
		}
	}

	/**
	 * Computes the checksum of the given file and writes it to the target checksum
	 * file, using the {@code ISO_8859_1} encoding.
	 *
	 * @param file
	 * @param checksumPath
	 * @throws IOException
	 */
	private void writeChecksumFile(Path file, Path checksumPath) throws IOException {
		var checksum = computeChecksum(file, CHECKSUM_ALGORITHM);
		try (var out = Files.newOutputStream(checksumPath, WRITE_OPTIONS)) {
			out.write(checksum.getBytes(ISO_8859_1));
		}
	}
}
