/*
 * Copyright 2021 DiffPlug
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import com.diffplug.spotless.Formatter;

public abstract class SpotlessTaskService implements BuildService<BuildServiceParameters.None> {
	private Map<String, SpotlessApply> apply = new HashMap<>();
	private Map<String, SpotlessTask> source = new HashMap<>();

	public void registerSource(SpotlessTask task) {
		source.put(task.getPath(), task);
	}

	public void registerApply(SpotlessApply task) {
		apply.put(task.getPath(), task);
	}

	public boolean getSourceDidWork(String sourceTaskPath) {
		SpotlessTask sourceTask = source.get(sourceTaskPath);
		if (sourceTask != null) {
			return sourceTask.getDidWork();
		} else {
			return false;
		}
	}

	public boolean getApplyDidWork(String applyTaskPath) {
		SpotlessApply applyTask = apply.get(applyTaskPath);
		if (applyTask != null) {
			return applyTask.getDidWork();
		} else {
			return false;
		}
	}

	public boolean applyWasInGraph(String applyTaskPath) {
		return apply.containsKey(applyTaskPath);
	}

	public Formatter buildFormatter(String sourceTaskPath) {
		SpotlessTask task = Objects.requireNonNull(source.get(sourceTaskPath));
		return task.buildFormatter();
	}
}
