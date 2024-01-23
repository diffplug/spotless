package com.diffplug.spotless.java;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.ProcessRunner;

public final class IdeaFormatterFunc implements FormatterFunc.NeedsFile {

	private static final Logger LOGGER =
			LoggerFactory.getLogger(IdeaStep.class);

	private static final String DEFAULT_IDEA = "idea";

	// TODO: Use ForeignExe to ensure file
	private final String binaryPath;
	private final boolean withDefaults;

	private IdeaFormatterFunc(boolean withDefaults, String binaryPath) {
		this.withDefaults = withDefaults;
		this.binaryPath = Objects.requireNonNullElse(binaryPath, DEFAULT_IDEA);
	}

	public static IdeaFormatterFunc allowingDefaultsWithCustomBinary(
			String binaryPath) {
		return new IdeaFormatterFunc(true, binaryPath);
	}

	public static IdeaFormatterFunc noDefaultsWithCustomBinary(
			String binaryPath) {
		return new IdeaFormatterFunc(false, binaryPath);
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
		builder.add(file.toString());
		return builder.build().collect(Collectors.toList());
	}

}
