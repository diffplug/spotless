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
package com.diffplug.spotless.npm;

import static com.diffplug.selfie.Selfie.expectSelfie;

import org.junit.jupiter.api.Test;

class NpmResourceHelperTest {

	@Test
	void itCalculatesMd5ForSingleString() {
		String input = "Hello, World!";

		expectSelfie(NpmResourceHelper.md5(input)).toBe("65a8e27d8879283831b664bd8b7f0ad4");
	}

	@Test
	void itCalculatesMd5ForMultipleStrings() {
		String input1 = "Hello, World!";
		String input2 = "Hello, Spencer!";

		expectSelfie(NpmResourceHelper.md5(input1, input2)).toBe("371ba0fbf3d73b33e71b4af8dc6afe00");
	}
}
