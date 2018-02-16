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
package com.diffplug.spotless.maven.scala;

import com.diffplug.spotless.maven.generic.Format;

import java.util.Set;

import static com.diffplug.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableSet;

public class Scala extends Format {
	private static final Set<String> DEFAULT_INCLUDES = unmodifiableSet(newHashSet("src/main/scala/**/*.scala",
			"src/test/scala/**/*.scala", "src/main/scala/**/*.sc", "src/test/scala/**/*.sc"));
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	public void addScalafmt(Scalafmt scalafmt) {
		addStepFactory(scalafmt);
	}
}
