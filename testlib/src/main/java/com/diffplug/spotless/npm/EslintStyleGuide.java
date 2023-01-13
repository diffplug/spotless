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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * A helper class to create dev dependencies for eslint when using one of the popular styleguides in testing.
 */
public enum EslintStyleGuide {
	TS_STANDARD_WITH_TYPESCRIPT("standard-with-typescript") {
		@Override
		public @Nonnull Map<String, String> devDependencies() {
			Map<String, String> dependencies = new LinkedHashMap<>();
			dependencies.put("eslint-config-standard-with-typescript", "^24.0.0");
			dependencies.put("eslint-plugin-import", "^2.26.0");
			dependencies.put("eslint-plugin-n", "^15.6.0");
			dependencies.put("eslint-plugin-promise", "^6.1.1");
			return dependencies;
		}
	},
	TS_XO_TYPESCRIPT("xo-typescript") {
		@Override
		public @Nonnull Map<String, String> devDependencies() {
			Map<String, String> dependencies = new LinkedHashMap<>();
			dependencies.put("eslint-config-xo", "^0.43.1");
			dependencies.put("eslint-config-xo-typescript", "^0.55.1");
			return dependencies;
		}
	},
	JS_AIRBNB("airbnb") {
		@Override
		public @Nonnull Map<String, String> devDependencies() {
			Map<String, String> dependencies = new LinkedHashMap<>();
			dependencies.put("eslint-config-airbnb-base", "^15.0.0");
			dependencies.put("eslint-plugin-import", "^2.26.0");
			return dependencies;
		}
	},
	JS_GOOGLE("google") {
		@Override
		public @Nonnull Map<String, String> devDependencies() {
			Map<String, String> dependencies = new LinkedHashMap<>();
			dependencies.put("eslint-config-google", "^0.14.0");
			return dependencies;
		}
	},
	JS_STANDARD("standard") {
		@Override
		public @Nonnull Map<String, String> devDependencies() {
			Map<String, String> dependencies = new LinkedHashMap<>();
			dependencies.put("eslint-config-standard", "^17.0.0");
			dependencies.put("eslint-plugin-import", "^2.26.0");
			dependencies.put("eslint-plugin-n", "^15.6.0");
			dependencies.put("eslint-plugin-promise", "^6.1.1");
			return dependencies;
		}
	},
	JS_XO("xo") {
		@Override
		public @Nonnull Map<String, String> devDependencies() {
			Map<String, String> dependencies = new LinkedHashMap<>();
			dependencies.put("eslint-config-xo", "^0.43.1");
			return dependencies;
		}
	};

	private final String popularStyleGuideName;

	EslintStyleGuide(String popularStyleGuideName) {
		this.popularStyleGuideName = popularStyleGuideName;
	}

	public abstract @Nonnull Map<String, String> devDependencies();

	public static EslintStyleGuide fromNameOrNull(String popularStyleGuideName) {
		for (EslintStyleGuide popularStyleGuide : EslintStyleGuide.values()) {
			if (popularStyleGuide.popularStyleGuideName.equals(popularStyleGuideName)) {
				return popularStyleGuide;
			}
		}
		return null;
	}

	public Map<String, String> mergedWith(Map<String, String> devDependencies) {
		Map<String, String> merged = new LinkedHashMap<>(devDependencies);
		merged.putAll(devDependencies());
		return merged;
	}

	public String asGradleMapStringMergedWith(Map<String, String> devDependencies) {
		return mergedWith(devDependencies).entrySet().stream()
				.map(entry -> "'" + entry.getKey() + "': '" + entry.getValue() + "'")
				.collect(Collectors.joining(", ", "[", "]"));
	}

	public String asMavenXmlStringMergedWith(Map<String, String> devDependencies) {
		return mergedWith(devDependencies).entrySet().stream()
				.map(entry -> "<" + entry.getKey() + ">" + entry.getValue() + "</" + entry.getKey() + ">")
				.collect(Collectors.joining("", "<devDependencies>", "</devDependencies>"));
	}

}
