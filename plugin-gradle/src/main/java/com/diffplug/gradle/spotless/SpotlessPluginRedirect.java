/*
 * Copyright 2020-2023 DiffPlug
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
package com.diffplug.gradle.spotless;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.diffplug.common.base.StringPrinter;

public class SpotlessPluginRedirect implements Plugin<Project> {
	private static final Pattern BAD_SEMVER = Pattern.compile("(\\d+)\\.(\\d+)");

	private static int badSemver(String input) {
		Matcher matcher = BAD_SEMVER.matcher(input);
		if (!matcher.find() || matcher.start() != 0) {
			throw new IllegalArgumentException("Version must start with " + BAD_SEMVER.pattern());
		}
		String major = matcher.group(1);
		String minor = matcher.group(2);
		return badSemver(Integer.parseInt(major), Integer.parseInt(minor));
	}

	/** Ambiguous after 2147.483647.blah-blah */
	private static int badSemver(int major, int minor) {
		return major * 1_000_000 + minor;
	}

	static Boolean gradleIsTooOld;

	static boolean gradleIsTooOld(Project project) {
		if (gradleIsTooOld == null) {
			gradleIsTooOld = badSemver(project.getGradle().getGradleVersion()) < badSemver(SpotlessPlugin.VER_GRADLE_min);
		}
		return gradleIsTooOld.booleanValue();
	}

	@Override
	public void apply(Project project) {
		String errorMsg = StringPrinter.buildStringFromLines(
				"We have moved from 'com.diffplug.gradle.spotless'",
				"                to 'com.diffplug.spotless'",
				"To migrate:",
				"- Test your build with: id 'com.diffplug.gradle.spotless' version '4.5.1'",
				"- Fix any deprecation warnings (shouldn't be many / any)",
				"- Now you can use:      id 'com.diffplug.spotless' version '5.0.0'",
				"",
				"That's all you really need to know, but as always, there are more details in the changelog:",
				"https://github.com/diffplug/spotless/blob/main/plugin-gradle/CHANGES.md",
				"",
				"While you're at it, you might want to search for \"target '**/\".  We used",
				"to  recommend that in our README, but it's a lot slower than something",
				"more specific like \"target 'src/**\".  Also, if you haven't tried them yet,",
				"take a look at our IDE integration and 'ratchetFrom'.  We've found them",
				"to be useful, hope you do too.",
				"",
				"If you like the idea behind 'ratchetFrom', you should checkout spotless-changelog",
				"https://github.com/diffplug/spotless-changelog");
		if (gradleIsTooOld(project)) {
			errorMsg = errorMsg.replace("To migrate:\n", "To migrate:\n- Upgrade Gradle to " + SpotlessPlugin.VER_GRADLE_min + " or newer (you're on " + project.getGradle().getGradleVersion() + ")\n");
		}
		throw new GradleException(errorMsg);
	}
}
