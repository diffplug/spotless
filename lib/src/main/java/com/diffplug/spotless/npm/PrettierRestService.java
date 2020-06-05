/*
 * Copyright (c) 2020 Ergon Informatik AG
 * Merkurstrasse 43, 8032 Zuerich, Switzerland
 * All rights reserved.
 */

package com.diffplug.spotless.npm;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PrettierRestService {

    PrettierRestService() {
    }


    // /prettier/config-options
    public String resolveConfig(File prettierConfigPath, Map<String, Object> prettierConfigOptions) {
        try {
            URL url = new URL("http://localhost:52429/prettier/config-options");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");

            Map<String, Object> jsonProperties = new LinkedHashMap<>();
            if (prettierConfigPath != null) {
                jsonProperties.put("prettier_config_path", prettierConfigPath.getAbsolutePath());
            }
            if (prettierConfigOptions != null) {
                jsonProperties.put("prettier_config_options", SimpleJsonWriter.RawJsonValue.asRawJson(SimpleJsonWriter.of(prettierConfigOptions).toJsonString()));

            }

            final SimpleJsonWriter jsonWriter = SimpleJsonWriter.of(jsonProperties);
            final String jsonString = jsonWriter.toJsonString();

            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(jsonString);
            out.flush();
            out.close();

            int status = con.getResponseCode();

            if (status != 200) {
                throw new RuntimeException("Received status " + status + " instead of 200. " + con.getResponseMessage());
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            return content.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String format(String fileContent, String configOptionsJsonString) {
        try {
            URL url = new URL("http://localhost:52429/prettier/format");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");


            Map<String, Object> jsonProperties = new LinkedHashMap<>();
            jsonProperties.put("file_content", fileContent);
            if (configOptionsJsonString != null) {
                jsonProperties.put("config_options", SimpleJsonWriter.RawJsonValue.asRawJson(configOptionsJsonString));
            }

            final SimpleJsonWriter jsonWriter = SimpleJsonWriter.of(jsonProperties);
            final String jsonString = jsonWriter.toJsonString();
            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(jsonString);
            out.flush();
            out.close();

            int status = con.getResponseCode();

            if (status != 200) {
                throw new RuntimeException("Received status " + status + " instead of 200 " + con.getResponseMessage());
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream input = con.getInputStream();

            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, numRead);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ParameterStringBuilder {
        public static String getParamsString(Map<String, String> params)
                throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                result.append("&");
            }

            String resultString = result.toString();
            return resultString.length() > 0
                    ? resultString.substring(0, resultString.length() - 1)
                    : resultString;
        }
    }

}
