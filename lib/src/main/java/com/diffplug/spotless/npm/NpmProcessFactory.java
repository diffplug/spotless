/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.npm;

public interface NpmProcessFactory {

	enum OnlinePreferrence {
		PREFER_OFFLINE("--prefer-offline"), PREFER_ONLINE("--prefer-online");

		private final String option;

		OnlinePreferrence(String option) {
			this.option = option;
		}

		public String option() {
			return option;
		}
	}

	NpmProcess createNpmInstallProcess(NodeServerLayout nodeServerLayout, NpmFormatterStepLocations formatterStepLocations, OnlinePreferrence onlinePreferrence);

	NpmLongRunningProcess createNpmServeProcess(NodeServerLayout nodeServerLayout, NpmFormatterStepLocations formatterStepLocations);

	default String describe() {
		return getClass().getSimpleName();
	}

}
