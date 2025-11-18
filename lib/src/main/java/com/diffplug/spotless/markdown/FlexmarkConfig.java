/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless.markdown;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FlexmarkConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The emulation profile is used by both the parser and the formatter and generally determines the markdown flavor.
	 * COMMONMARK is the default defined by flexmark-java.
	 */
	private String emulationProfile = "COMMONMARK";
	private List<String> pegdownExtensions = List.of("ALL");
	private List<String> extensions = new ArrayList<>();

	public String getEmulationProfile() {
		return emulationProfile;
	}

	public void setEmulationProfile(String emulationProfile) {
		this.emulationProfile = emulationProfile;
	}

	public List<String> getPegdownExtensions() {
		return pegdownExtensions;
	}

	public void setPegdownExtensions(List<String> pegdownExtensions) {
		this.pegdownExtensions = pegdownExtensions;
	}

	public List<String> getExtensions() {
		return extensions;
	}

	public void setExtensions(List<String> extensions) {
		this.extensions = extensions;
	}

}
