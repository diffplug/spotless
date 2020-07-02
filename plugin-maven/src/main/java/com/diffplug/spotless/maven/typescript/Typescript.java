/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.maven.typescript;

import java.util.Collections;
import java.util.Set;

import com.diffplug.spotless.maven.FormatterFactory;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <typescript>...</typescript>} configuration element.
 * <p>
 * It defines a formatter for typescript source files.
 */
public class Typescript extends FormatterFactory {
	private static final String LICENSE_HEADER_DELIMITER = null;

	@Override
	public Set<String> defaultIncludes() {
		return Collections.emptySet();
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	public void addTsfmt(Tsfmt tsfmt) {
		addStepFactory(tsfmt);
	}
}
