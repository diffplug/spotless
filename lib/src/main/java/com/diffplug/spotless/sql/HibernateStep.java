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
package com.diffplug.spotless.sql;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Wraps up [BasicFormatterImpl](https://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/engine/jdbc/internal/BasicFormatterImpl.html) as a FormatterStep. */
public class HibernateStep {
	// prevent direct instantiation
	private HibernateStep() {}

	private static final String DEFAULT_VERSION = "5.2.12.Final";
	static final String NAME = "hibernateSql";
	static final String MAVEN_COORDINATE = "org.hibernate:hibernate-core:";

	public enum Kind {
		BASIC("org.hibernate.engine.jdbc.internal.BasicFormatterImpl"), DDL("org.hibernate.engine.jdbc.internal.DDLFormatterImpl");

		private final String className;

		Kind(String className) {
			this.className = className;
		}
	}

	public static FormatterStep create(Provisioner provisioner, Kind kind) {
		return create(defaultVersion(), provisioner, kind);
	}

	public static FormatterStep create(String version, Provisioner provisioner, Kind kind) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(version, provisioner, kind.className),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		final String formatterClassname;
		final JarState jarState;

		State(String version, Provisioner provisioner, String formatterClassname) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
			this.formatterClassname = formatterClassname;
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			// this is how we actually do a format
			Class<?> formatterClazz = classLoader.loadClass(formatterClassname);
			Object formatter = formatterClazz.newInstance();
			Method formatMethod = formatterClazz.getMethod("format", String.class);
			return input -> {
				return (String) formatMethod.invoke(formatter, input);
			};
		}
	}
}
