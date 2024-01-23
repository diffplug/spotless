package com.diffplug.spotless.java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.ProcessRunner;

public final class IdeaFormatterFunc implements FormatterFunc.NeedsFile {

	private static final Logger LOGGER =
			LoggerFactory.getLogger(IdeaStep.class);

	private static final String DEFAULT_IDEA = "idea";

	private String binaryPath;
	private String configPath;
	private boolean withDefaults;

	private IdeaFormatterFunc(boolean withDefaults, String binaryPath,
			String configPath) {
		this.withDefaults = withDefaults;
		this.configPath = configPath;
		this.binaryPath = Objects.requireNonNullElse(binaryPath, DEFAULT_IDEA);
		resolveFullBinaryPathAndCheckVersion();
	}

	private void resolveFullBinaryPathAndCheckVersion() {
		var exe = ForeignExe.nameAndVersion(this.binaryPath, "IntelliJ IDEA")
				.versionRegex(Pattern.compile("(IntelliJ IDEA) .*"))
				.fixCantFind("IDEA executable cannot be found on your machine, "
						+ "please install it and put idea binary to PATH; or report the problem")
				.fixWrongVersion("Provided binary is not IDEA, "
						+ "please check it and fix the problem; or report the problem");
		try {
			this.binaryPath = exe.confirmVersionAndGetAbsolutePath();
		} catch (IOException e) {
			throw new IllegalArgumentException("binary cannot be found", e);
		} catch (InterruptedException e) {
			throw new IllegalArgumentException(
					"binary cannot be found, process was interrupted", e);
		}
	}

	public static IdeaFormatterFunc allowingDefaultsWithCustomBinary(
			String binaryPath, String configPath) {
		return new IdeaFormatterFunc(true, binaryPath, configPath);
	}

	public static IdeaFormatterFunc noDefaultsWithCustomBinary(
			String binaryPath, String configPath) {
		return new IdeaFormatterFunc(false, binaryPath, configPath);
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		List<String> params = getParams(file);

		try (ProcessRunner runner = new ProcessRunner()) {
			var result = runner.exec(params);

			LOGGER.debug("command finished with stdout: {}",
					result.assertExitZero(StandardCharsets.UTF_8));

			return Files.readString(file.toPath());
		}
	}

	private List<String> getParams(File file) {
		var builder = Stream.<String>builder();
		builder.add(binaryPath);
		builder.add("format");
		if (withDefaults) {
			builder.add("-allowDefaults");
		}
		if (configPath != null) {
			builder.add("-s");
			builder.add(configPath);
		}
		builder.add(file.toString());
		return builder.build().collect(Collectors.toList());
	}

}
