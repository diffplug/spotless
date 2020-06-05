/*
 * Copyright (c) 2020 Ergon Informatik AG
 * Merkurstrasse 43, 8032 Zuerich, Switzerland
 * All rights reserved.
 */

package com.diffplug.spotless.npm;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static java.util.Objects.requireNonNull;

class SimpleRestClient {
    private final String baseUrl;

    private SimpleRestClient(String baseUrl) {
        this.baseUrl = requireNonNull(baseUrl);
    }

    static SimpleRestClient forBaseUrl(String baseUrl) {
        return new SimpleRestClient(baseUrl);
    }

    String postJson(String endpoint, Map<String, Object> jsonParams) throws SimpleRestException {
        final SimpleJsonWriter jsonWriter = SimpleJsonWriter.of(jsonParams);
        final String jsonString = jsonWriter.toJsonString();

        return postJson(endpoint, jsonString);
    }

    String postJson(String endpoint, String rawJson) throws SimpleRestException {
        try {
            URL url = new URL(this.baseUrl + endpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(rawJson);
            out.flush();
            out.close();

            int status = con.getResponseCode();

            if (status != 200) {
                throw new SimpleRestResponseException(status, con.getResponseMessage(), "Unexpected response status code.");
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            return content.toString();
        } catch (IOException e) {
            throw new SimpleRestIOException(e);
        }
    }


    static abstract class SimpleRestException extends RuntimeException {
        public SimpleRestException() {
        }

        public SimpleRestException(Throwable cause) {
            super(cause);
        }
    }

    static class SimpleRestResponseException extends SimpleRestException {
        private final int statusCode;

        private final String responseMessage;

        private final String exceptionMessage;

        public SimpleRestResponseException(int statusCode, String responseMessage, String exceptionmessage) {
            this.statusCode = statusCode;
            this.responseMessage = responseMessage;
            this.exceptionMessage = exceptionmessage;
        }

        @Nonnull
        public int getStatusCode() {
            return statusCode;
        }

        @Nonnull
        public String getResponseMessage() {
            return responseMessage;
        }

        @Nonnull
        public String getExceptionMessage() {
            return exceptionMessage;
        }

        @Override
        public String getMessage() {
            return String.format("%s: %s (%s)", getStatusCode(), getResponseMessage(), getExceptionMessage());
        }
    }

    static class SimpleRestIOException extends SimpleRestException {
        public SimpleRestIOException(Throwable cause) {
            super(cause);
        }
    }
}
