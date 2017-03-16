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
package com.diffplug.gradle.spotless;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Action;

import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.markdown.FreshMarkStep;

public class FreshMarkExtension extends FormatExtension {
	static final String NAME = "freshmark";

	public List<Action<Map<String, Object>>> propertyActions = new ArrayList<>();

	public FreshMarkExtension(SpotlessExtension root) {
		super(root);
		addStep(FreshMarkStep.create(() -> {
			Map<String, Object> map = new HashMap<>();
			for (Action<Map<String, Object>> action : propertyActions) {
				action.execute(map);
			}
			return map;
		}, GradleProvisioner.fromProject(getProject())));
	}

	public void properties(Action<Map<String, Object>> action) {
		propertyActions.add(action);
	}

	public void propertiesFile(Object... files) {
		propertyActions.add(map -> {
			FormatterProperties preferences = FormatterProperties.from(getProject().files(files));
			/* FreshMarkStep.State serializes the properties and not the files.
			 * Therefore they must be stored in a hash-map like used by Properties.*/
			preferences.getProperties().forEach((key, value) -> map.put(key.toString(), value));
		});
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		// defaults to all markdown files
		if (target == null) {
			target = parseTarget("**/*.md");
		}
		super.setupTask(task);
	}
}
