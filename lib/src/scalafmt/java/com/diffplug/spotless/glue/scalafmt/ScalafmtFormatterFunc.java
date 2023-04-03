/*
 * Copyright 2022-2023 DiffPlug
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
import org.scalafmt.config.ScalafmtConfig;
import org.scalafmt.config.ScalafmtConfig$;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;

import scala.collection.immutable.Set$;

public class ScalafmtFormatterFunc implements FormatterFunc {
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
			var configStr = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			config = Scalafmt.parseHoconConfig(configStr).get();
		}
	}

	@Override
	public String apply(String input) {
		return Scalafmt.format(input, config, Set$.MODULE$.empty()).get();
	}
}
