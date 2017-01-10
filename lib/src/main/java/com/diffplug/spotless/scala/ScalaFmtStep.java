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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Wraps up [scalafmt](https://github.com/olafurpg/scalafmt) as a FormatterStep. */
public class ScalaFmtStep {
	// prevent direct instantiation
	private ScalaFmtStep() {}

	private static final String DEFAULT_VERSION = "0.5.1";
	static final String NAME = "scalafmt";
	static final String MAVEN_COORDINATE = "com.geirsson:scalafmt_2.11:";

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return FormatterStep.createLazy(NAME,
				() -> new State(version, provisioner),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		final JarState jarState;

		State(String version, Provisioner provisioner) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
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

			return input -> {
				Object resultInsideFormatted = formatMethod.invoke(null, input, defaultScalaFmtConfig, emptyRange);
				String result = (String) formattedGet.invoke(resultInsideFormatted);
				return result;
			};
		}
	}
}
