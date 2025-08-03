/*
 * Copyright 2022-2025 DiffPlug
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
package com.diffplug.spotless.glue.scalafmt;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.scalafmt.Scalafmt;
import org.scalafmt.Versions;
import org.scalafmt.config.ScalafmtConfig;
import org.scalafmt.config.ScalafmtConfig$;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;

import scala.collection.immutable.Set$;

public class ScalafmtFormatterFunc implements FormatterFunc.NeedsFile {
	private final ScalafmtConfig config;

	public ScalafmtFormatterFunc(FileSignature configSignature) throws Exception {
		if (configSignature.files().isEmpty()) {
			// Note that reflection is used here only because Scalafmt has a method called
			// default which happens to be a reserved Java keyword. The only way to call
			// such methods is by reflection, see
			// https://vlkan.com/blog/post/2015/11/20/scala-method-with-java-reserved-keyword/
			Method method = ScalafmtConfig$.MODULE$.getClass().getDeclaredMethod("default");
			config = (ScalafmtConfig) method.invoke(ScalafmtConfig$.MODULE$);
		} else {
			File file = configSignature.getOnlyFile();
			String configStr = Files.readString(file.toPath());
			config = Scalafmt.parseHoconConfig(configStr).get();
		}

		// This check is to raise awareness to the user that the version of the config file is currently not used.
		// Context: https://github.com/diffplug/spotless/issues/2460
		// This check should be removed when Spotless dynamically loads the proper version of the Scalafmt library.
		String scalafmtLibraryVersion = Versions.version();
		if (!config.version().equals(scalafmtLibraryVersion)) {
			throw new IllegalArgumentException(
					"Spotless is using " + scalafmtLibraryVersion + " but the config file declares " + config.version() +
							". Both must match. Update the version declared in the plugin's settings and/or the config file.");
		}
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		return Scalafmt.format(unix, config, Set$.MODULE$.empty(), file.getAbsolutePath()).get();
	}
}
