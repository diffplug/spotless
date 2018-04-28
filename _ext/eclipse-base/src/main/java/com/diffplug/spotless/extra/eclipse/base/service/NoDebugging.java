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
package com.diffplug.spotless.extra.eclipse.base.service;

import java.io.File;
import java.util.Map;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;

/** No debugging shall be performed */
public class NoDebugging implements DebugOptions {

	@Override
	public boolean getBooleanOption(String option, boolean defaultValue) {
		return false;
	}

	@Override
	public String getOption(String option) {
		return null;
	}

	@Override
	public String getOption(String option, String defaultValue) {
		return null;
	}

	@Override
	public int getIntegerOption(String option, int defaultValue) {
		return 0;
	}

	@Override
	public Map<String, String> getOptions() {
		return null;
	}

	@Override
	public void setOption(String option, String value) {}

	@Override
	public void setOptions(Map<String, String> options) {}

	@Override
	public void removeOption(String option) {}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public void setDebugEnabled(boolean value) {}

	@Override
	public void setFile(File newFile) {}

	@Override
	public File getFile() {
		return null;
	}

	@Override
	public DebugTrace newDebugTrace(String bundleSymbolicName) {
		return null;
	}

	@Override
	public DebugTrace newDebugTrace(String bundleSymbolicName, Class<?> traceEntryClass) {
		return null;
	}

}
