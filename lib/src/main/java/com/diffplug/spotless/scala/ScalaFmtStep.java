/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.scala;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Objects;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Wraps up [scalafmt](https://github.com/olafurpg/scalafmt) as a FormatterStep. */
public class ScalaFmtStep {
	// prevent direct instantiation
	private ScalaFmtStep() {}

	private static final String DEFAULT_VERSION = "1.1.0";
	static final String NAME = "scalafmt";
	static final String MAVEN_COORDINATE_PRE_2 = "com.geirsson:scalafmt-core_2.11:";
	static final String MAVEN_COORDINATE = "org.scalameta:scalafmt-core_2.11:";

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner, null);
	}

	public static FormatterStep create(String version, Provisioner provisioner, @Nullable File configFile) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(version, provisioner, configFile),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		final JarState jarState;
		final FileSignature configSignature;

		State(String version, Provisioner provisioner, @Nullable File configFile) throws IOException {
			String MAVEN_COORDINATE_FOR_VERSION;
			if (Integer.parseInt(version.substring(0,1)) < 2) MAVEN_COORDINATE_FOR_VERSION = MAVEN_COORDINATE_PRE_2;
			else MAVEN_COORDINATE_FOR_VERSION = MAVEN_COORDINATE;

			this.jarState = JarState.from(MAVEN_COORDINATE_FOR_VERSION + version, provisioner);
			this.configSignature = FileSignature.signAsList(configFile == null ? Collections.emptySet() : Collections.singleton(configFile));
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			// scalafmt returns instances of formatted, we get result by calling get()
			Class<?> formatted = classLoader.loadClass("org.scalafmt.Formatted");
			Method formattedGet = formatted.getMethod("get");

			// this is how we actually do a format
			Class<?> scalafmt = classLoader.loadClass("org.scalafmt.Scalafmt");
			Class<?> scalaSet = classLoader.loadClass("scala.collection.immutable.Set");

			Object defaultScalaFmtConfig = scalafmt.getMethod("format$default$2").invoke(null);
			Object emptyRange = scalafmt.getMethod("format$default$3").invoke(null);
			Method formatMethod = scalafmt.getMethod("format", String.class, defaultScalaFmtConfig.getClass(), scalaSet);

			// now we just need to parse the config, if any
			Object config;
			if (configSignature.files().isEmpty()) {
				config = defaultScalaFmtConfig;
			} else {
				File file = configSignature.getOnlyFile();

				Class<?> optionCls = classLoader.loadClass("scala.Option");
				Class<?> configCls = classLoader.loadClass("org.scalafmt.config.Config");
				Class<?> scalafmtCls = classLoader.loadClass("org.scalafmt.Scalafmt");

				Object either;

				try {
					// scalafmt >= 1.6.0
					Method parseHoconConfig = scalafmtCls.getMethod("parseHoconConfig", String.class);

					String configStr = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

					Object configured = parseHoconConfig.invoke(null, configStr);
					either = invokeNoArg(configured, "toEither");
				} catch (NoSuchMethodException e) {
					// scalafmt >= v0.7.0-RC1 && scalafmt < 1.6.0
					Method fromHocon = configCls.getMethod("fromHoconString", String.class, optionCls);
					Object fromHoconEmptyPath = configCls.getMethod("fromHoconString$default$2").invoke(null);

					String configStr = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

					Object configured = fromHocon.invoke(null, configStr, fromHoconEmptyPath);
					either = invokeNoArg(configured, "toEither");
				}

				config = invokeNoArg(invokeNoArg(either, "right"), "get");
			}
			return input -> {
				Object resultInsideFormatted = formatMethod.invoke(null, input, config, emptyRange);
				return (String) formattedGet.invoke(resultInsideFormatted);
			};
		}
	}

	private static Object invokeNoArg(Object obj, String toInvoke) throws Exception {
		Class<?> clazz = obj.getClass();
		Method method = clazz.getMethod(toInvoke);
		return method.invoke(obj);
	}
}
