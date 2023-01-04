/*
 * Copyright 2016-2023 DiffPlug
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EslintRestService extends BaseNpmRestService {

	EslintRestService(String baseUrl) {
		super(baseUrl);
	}

	public String format(String fileContent, Map<FormatOption, Object> formatOptions) {
		Map<String, Object> jsonProperties = new LinkedHashMap<>();
		jsonProperties.put("file_content", fileContent);
		for (Entry<FormatOption, Object> option : formatOptions.entrySet()) {
			jsonProperties.put(option.getKey().backendName, option.getValue());
		}
		return restClient.postJson("/eslint/format", jsonProperties);
	}

	enum FormatOption {
		ESLINT_OVERRIDE_CONFIG("eslint_override_config"), ESLINT_OVERRIDE_CONFIG_FILE("eslint_override_config_file"), FILE_PATH("file_path"), TS_CONFIG_ROOT_DIR("ts_config_root_dir");

		private final String backendName;

		FormatOption(String backendName) {
			this.backendName = backendName;
		}
	}
}
