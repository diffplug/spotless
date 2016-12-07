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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.Action;

import com.diffplug.common.base.Errors;
import com.diffplug.common.io.Files;
import com.diffplug.spotless.markdown.FreshMarkStep;

public class FreshMarkExtension extends FormatExtension {
	public static final String NAME = "freshmark";

	public List<Action<Map<String, Object>>> propertyActions = new ArrayList<>();

	public FreshMarkExtension(SpotlessExtension root) {
		super(NAME, root);
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

	public void propertiesFile(Object file) {
		propertyActions.add(map -> {
			File propFile = getProject().file(file);
			try (InputStream input = Files.asByteSource(propFile).openBufferedStream()) {
				Properties props = new Properties();
				props.load(input);
				for (String key : props.stringPropertyNames()) {
					map.put(key, props.getProperty(key));
				}
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		});
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
