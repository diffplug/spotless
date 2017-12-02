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

import java.io.File;
import java.io.Serializable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.FormatterStep;

/** Wraps up [BasicFormatterImpl](https://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/engine/jdbc/internal/BasicFormatterImpl.html) as a FormatterStep. */
public class DBeaverSQLFormatterStep {

	static final String NAME = "dbeaverSql";

	// prevent direct instantiation
	private DBeaverSQLFormatterStep() {}

	public static FormatterStep create(Iterable<File> files) {
		return FormatterStep.createLazy(NAME,
				() -> new State(files),
				State::createFormat);
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The signature of the settings file. */
		final FileSignature settings;

		State(final Iterable<File> settingsFiles) throws Exception {
			this.settings = FileSignature.signAsList(settingsFiles);
		}

		FormatterFunc createFormat() throws Exception {
			FormatterProperties preferences = FormatterProperties.from(settings.files());
			DBeaverSQLFormatter DBeaverSqlFormatter = new DBeaverSQLFormatter(preferences.getProperties());
			return DBeaverSqlFormatter::format;
		}
	}
}
