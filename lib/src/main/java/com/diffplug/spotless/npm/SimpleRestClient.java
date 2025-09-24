/*
 * Copyright 2016-2025 DiffPlug
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

import static java.util.Objects.requireNonNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class SimpleRestClient {
	private final String baseUrl;

	private SimpleRestClient(String baseUrl) {
		this.baseUrl = requireNonNull(baseUrl);
	}

	static SimpleRestClient forBaseUrl(String baseUrl) {
		return new SimpleRestClient(baseUrl);
	}

	String postJson(String endpoint, Map<String, Object> jsonParams) throws SimpleRestException {
		final JsonWriter jsonWriter = JsonWriter.of(jsonParams);
		final String jsonString = jsonWriter.toJsonString();

		return postJson(endpoint, jsonString);
	}

	String post(String endpoint) throws SimpleRestException {
		return postJson(endpoint, (String) null);
	}

	String postJson(String endpoint, @Nullable String rawJson) throws SimpleRestException {
		try {
			URL url = new URL(this.baseUrl + endpoint);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(60 * 1000); // one minute
			con.setReadTimeout(2 * 60 * 1000); // two minutes - who knows how large those files can actually get
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setDoOutput(true);
			if (rawJson != null) {
				try (OutputStream out = con.getOutputStream()) {
					NpmResourceHelper.writeUtf8StringToOutputStream(rawJson, out);
					out.flush();
				}
			}

			int status = con.getResponseCode();

			if (status != 200) {
				throw new SimpleRestResponseException(status, readError(con), "Unexpected response status code at " + endpoint);
			}

			String response = readResponse(con);
			return response;
		} catch (IOException e) {
			throw new SimpleRestIOException(e);
		}
	}

	private String readError(HttpURLConnection con) throws IOException {
		return readInputStream(con.getErrorStream());
	}

	private String readResponse(HttpURLConnection con) throws IOException {
		return readInputStream(con.getInputStream());
	}

	private String readInputStream(InputStream inputStream) throws IOException {
		try (BufferedInputStream input = new BufferedInputStream(inputStream)) {
			return NpmResourceHelper.readUtf8StringFromInputStream(input);
		}
	}

	static abstract class SimpleRestException extends RuntimeException {
		private static final long serialVersionUID = -8260821395756603787L;

		public SimpleRestException() {}

		public SimpleRestException(Throwable cause) {
			super(cause);
		}
	}

	static class SimpleRestResponseException extends SimpleRestException {
		private static final long serialVersionUID = -7637152299410053401L;

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
			return "%s [HTTP %s] -- (%s)".formatted(getExceptionMessage(), getStatusCode(), getResponseMessage());
		}
	}

	static class SimpleRestIOException extends SimpleRestException {
		private static final long serialVersionUID = -7909757660531122308L;

		public SimpleRestIOException(Throwable cause) {
			super(cause);
		}
	}
}
