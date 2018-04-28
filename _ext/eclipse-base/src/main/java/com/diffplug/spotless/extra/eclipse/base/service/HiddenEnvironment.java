/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.gradle.spotless.eclipse.service;

import java.util.Locale;

import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

/** Empty default Eclipse environment. No system information is accessible. */
public class HiddenEnvironment implements EnvironmentInfo {

	@Override
	public String[] getCommandLineArgs() {
		return new String[0];
	}

	@Override
	public String[] getFrameworkArgs() {
		return new String[0];
	}

	@Override
	public String[] getNonFrameworkArgs() {
		return new String[0];
	}

	@Override
	public String getOSArch() {
		return System.getProperty("os.arch");
	}

	@Override
	public String getNL() {
		return Locale.getDefault().getLanguage();
	}

	@Override
	public String getOS() {
		return Constants.OS_UNKNOWN;
	}

	@Override
	public String getWS() {
		return null; //No window system
	}

	@Override
	public boolean inDebugMode() {
		return false;
	}

	@Override
	public boolean inDevelopmentMode() {
		return false;
	}

	@Override
	public String setProperty(String key, String value) {
		return value; //Launcher information is not stored
	}

	@Override
	public String getProperty(String key) {
		return null; //Launcher information/configuration is not required
	}

}
