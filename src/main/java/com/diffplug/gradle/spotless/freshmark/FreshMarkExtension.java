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
package com.diffplug.gradle.spotless.freshmark;

import java.util.Map;

import com.diffplug.freshmark.FreshMark;
import com.diffplug.gradle.spotless.BaseFormatTask;
import com.diffplug.gradle.spotless.FormatExtension;
import com.diffplug.gradle.spotless.SpotlessExtension;

public class FreshMarkExtension extends FormatExtension {
	public static final String NAME = "freshmark";

	public Map<String, ?> properties;

	public FreshMarkExtension(SpotlessExtension root) {
		super(NAME, root);
		customLazy(NAME, () -> {
			// defaults to all project properties
			if (properties == null) {
				properties = getProject().getProperties();
			}
			FreshMark freshMark = new FreshMark(properties, getProject().getLogger()::warn);
			return freshMark::compile;
		});
	}

	public void properties(Map<String, ?> properties) {
		this.properties = properties;
	}

	@Override
	protected void setupTask(BaseFormatTask task) {
		// defaults to all markdown files
		if (target == null) {
			target = parseTarget("**/*.md");
		}
		super.setupTask(task);
	}
}
