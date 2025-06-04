/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.gradle.spotless;

import java.nio.charset.Charset;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import com.diffplug.spotless.ConfigurationCacheHackList;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.LineEnding;

final class FormatterSpec {

	private final Property<String> encoding;

	@Input
	Property<String> getEncoding() {
		return encoding;
	}

	private final Property<LineEnding.Policy> lineEndingsPolicy;

	@Input
	Property<LineEnding.Policy> getLineEndingsPolicy() {
		return lineEndingsPolicy;
	}

	private final ConfigurationCacheHackList stepsInternalRoundtrip = ConfigurationCacheHackList.forRoundtrip();

	@Internal
	public ConfigurationCacheHackList getStepsInternalRoundtrip() {
		return stepsInternalRoundtrip;
	}

	FormatterSpec(ObjectFactory objects) {
		encoding = objects.property(String.class);
		lineEndingsPolicy = objects.property(LineEnding.Policy.class);

		// set by SpotlessExtension, but possibly overridden by FormatExtension
		getEncoding().convention("UTF-8");
	}

	Formatter buildFormatter() {
		return Formatter.builder()
				.lineEndingsPolicy(getLineEndingsPolicy().get())
				.encoding(Charset.forName(getEncoding().get()))
				.steps(stepsInternalRoundtrip.getSteps())
				.build();
	}

}
