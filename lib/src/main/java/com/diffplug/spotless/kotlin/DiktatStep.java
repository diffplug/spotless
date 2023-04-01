/*
 * Copyright 2021-2022 DiffPlug
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
package com.diffplug.spotless.kotlin;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.*;

import javax.annotation.Nullable;

import com.diffplug.spotless.*;

/** Wraps up <a href="https://github.com/cqfn/diKTat">diktat</a> as a FormatterStep. */
public class DiktatStep {

	// prevent direct instantiation
	private DiktatStep() {}

	private static final String MIN_SUPPORTED_VERSION = "1.2.1";

	private static final String DEFAULT_VERSION = "1.2.4.2";
	static final String NAME = "diktat";
	static final String PACKAGE_DIKTAT = "org.cqfn.diktat";
	static final String MAVEN_COORDINATE = PACKAGE_DIKTAT + ":diktat-rules:";

	public static String defaultVersionDiktat() {
		return DEFAULT_VERSION;
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersionDiktat(), provisioner);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner) {
		return create(versionDiktat, provisioner, null);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner, @Nullable FileSignature config) {
		return create(versionDiktat, provisioner, false, config);
	}

	public static FormatterStep createForScript(String versionDiktat, Provisioner provisioner, @Nullable FileSignature config) {
		return create(versionDiktat, provisioner, true, config);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner, boolean isScript, @Nullable FileSignature config) {
		if (BadSemver.version(versionDiktat) < BadSemver.version(MIN_SUPPORTED_VERSION)) {
			throw new IllegalStateException("Minimum required Diktat version is " + MIN_SUPPORTED_VERSION + ", you tried " + versionDiktat + " which is too old");
		}
		Objects.requireNonNull(versionDiktat, "versionDiktat");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new DiktatStep.State(versionDiktat, provisioner, isScript, config),
				DiktatStep.State::createFormat);
	}

	static final class State implements Serializable {

		private static final long serialVersionUID = 1L;

		/** Are the files being linted Kotlin script files. */
		private final boolean isScript;
		private final @Nullable FileSignature config;
		final JarState jar;

		State(String versionDiktat, Provisioner provisioner, boolean isScript, @Nullable FileSignature config) throws IOException {

			HashSet<String> pkgSet = new HashSet<>();
			pkgSet.add(MAVEN_COORDINATE + versionDiktat);

			this.jar = JarState.from(pkgSet, provisioner);
			this.isScript = isScript;
			this.config = config;
		}

		FormatterFunc createFormat() throws Exception {
			if (config != null) {
				System.setProperty("diktat.config.path", config.getOnlyFile().getAbsolutePath());
			}

			Class<?> formatterFunc = jar.getClassLoader().loadClass("com.diffplug.spotless.glue.diktat.DiktatFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(boolean.class);
			return (FormatterFunc.NeedsFile) constructor.newInstance(isScript);
		}
	}
}
