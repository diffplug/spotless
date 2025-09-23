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
package selfie;

import java.util.stream.Collectors;

import com.diffplug.selfie.Selfie;
import com.diffplug.selfie.StringSelfie;
import com.diffplug.spotless.ProcessRunner;

public class MavenSelfie {
	private static final String ERROR_PREFIX = "[ERROR] ";

	public static StringSelfie expectSelfieErrorMsg(ProcessRunner.Result result) {
		String concatenatedError = result.stdOutUtf8().lines()
				.map(line -> line.startsWith(ERROR_PREFIX) ? line.substring(ERROR_PREFIX.length()) : null)
				.filter(line -> line != null)
				.collect(Collectors.joining("\n"));

		String sanitizedVersion = concatenatedError.replaceFirst("com\\.diffplug\\.spotless:spotless-maven-plugin:([^:]+):", "com.diffplug.spotless:spotless-maven-plugin:VERSION:");

		int help1 = sanitizedVersion.indexOf("-> [Help 1]");
		String trimTrailingString = sanitizedVersion.substring(0, help1);

		return Selfie.expectSelfie(trimTrailingString);
	}
}
