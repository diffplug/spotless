/*
 * Copyright (c) 2020 Ergon Informatik AG
 * Merkurstrasse 43, 8032 Zuerich, Switzerland
 * All rights reserved.
 */

package com.diffplug.spotless.npm;

import java.util.LinkedHashMap;
import java.util.Map;

public class TsFmtRestService {

    private final SimpleRestClient restClient;

    TsFmtRestService(String baseUrl) {
        this.restClient = SimpleRestClient.forBaseUrl(baseUrl);
    }


    public String format(String fileContent, Map<String, Object> configOptions) {
        Map<String, Object> jsonProperties = new LinkedHashMap<>();
        jsonProperties.put("file_content", fileContent);
        if (configOptions != null && !configOptions.isEmpty()) {
            jsonProperties.put("config_options", SimpleJsonWriter.of(configOptions).toRawJsonValue());
        }

        return restClient.postJson("/tsfmt/format", jsonProperties);
    }

}
