/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.extra.wtp;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class EclipseWtpFormatterStepTestOld extends EclipseWtpFormatterStepTest {

	@Override
	protected String[] getSupportedVersions() {
		return new String[]{"4.7.3a", "4.7.3b", "4.8.0", "4.12.0", "4.13.0", "4.14.0", "4.15.0", "4.16.0", "4.17.0", "4.18.0"};
	}

	@Override
	protected void makeAssumptions() {}
}
