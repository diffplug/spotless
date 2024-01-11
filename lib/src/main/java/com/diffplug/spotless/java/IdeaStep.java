package com.diffplug.spotless.java;

import java.io.File;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

public class IdeaStep {

	private static final Logger LOGGER =
			LoggerFactory.getLogger(IdeaStep.class);

	public static FormatterStep create() {
		// TODO: make it lazy
		return FormatterStep.createNeverUpToDate("IDEA",
				new FormatterFunc.NeedsFile() {

					// TODO: parameterize so user is able to provide it's own file
					// TODO: Use ForeignExe to ensure file
					private final String binaryPath =
							"idea";

					@Override
					public String applyWithFile(String unix, File file)
							throws Exception {
						try (ProcessRunner runner = new ProcessRunner()) {
							var result = runner.exec(binaryPath, "format",
									"-allowDefaults", file.toString());
							LOGGER.debug("command finished with stdout: {}",
									result.stdOutUtf8());
							return Files.readString(file.toPath());
						}
					}
				});
	}

}
