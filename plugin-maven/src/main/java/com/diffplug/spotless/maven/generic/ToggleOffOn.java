/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.spotless.maven.generic;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.generic.PipeStepPair;

public class ToggleOffOn {
	@Parameter
	public String off = PipeStepPair.defaultToggleOff();

	@Parameter
	public String on = PipeStepPair.defaultToggleOn();

	@Parameter
	public String regex;

	public PipeStepPair createPair() {
		if (regex != null) {
			return PipeStepPair.named(PipeStepPair.defaultToggleName()).regex(regex).buildPair();
		} else {
			return PipeStepPair.named(PipeStepPair.defaultToggleName()).openClose(off, on).buildPair();
		}
	}
}
