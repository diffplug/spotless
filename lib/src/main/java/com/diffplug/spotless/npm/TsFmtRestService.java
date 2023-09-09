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

public class TsFmtRestService extends BaseNpmRestService {

	TsFmtRestService(String baseUrl) {
		super(baseUrl);
	}

	public String format(String fileContent, Map<String, Object> configOptions) {
		Map<String, Object> jsonProperties = new LinkedHashMap<>();
		jsonProperties.put("file_content", fileContent);
		if (configOptions != null && !configOptions.isEmpty()) {
			jsonProperties.put("config_options", JsonWriter.of(configOptions).toJsonRawValue());
		}

		return restClient.postJson("/tsfmt/format", jsonProperties);
	}
}
