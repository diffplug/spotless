/*
 * Copyright (c) 2020 Ergon Informatik AG
 * Merkurstrasse 43, 8032 Zuerich, Switzerland
 * All rights reserved.
 */

package com.diffplug.spotless.npm;

import com.diffplug.spotless.npm.SimpleJsonWriter.RawJsonValue;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class PrettierRestService {

    private final SimpleRestClient restClient;

    PrettierRestService(String baseUrl) {
        this.restClient = SimpleRestClient.forBaseUrl(baseUrl);
    }

    public String resolveConfig(File prettierConfigPath, Map<String, Object> prettierConfigOptions) {
        Map<String, Object> jsonProperties = new LinkedHashMap<>();
        if (prettierConfigPath != null) {
            jsonProperties.put("prettier_config_path", prettierConfigPath.getAbsolutePath());
        }
        if (prettierConfigOptions != null) {
            jsonProperties.put("prettier_config_options", SimpleJsonWriter.of(prettierConfigOptions).toRawJsonValue());

        }
        return restClient.postJson("/prettier/config-options", jsonProperties);
    }

    public String format(String fileContent, String configOptionsJsonString) {
        Map<String, Object> jsonProperties = new LinkedHashMap<>();
        jsonProperties.put("file_content", fileContent);
        if (configOptionsJsonString != null) {
            jsonProperties.put("config_options", RawJsonValue.wrap(configOptionsJsonString));
        }

        return restClient.postJson("/prettier/format", jsonProperties);
    }

}
