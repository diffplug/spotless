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
package com.diffplug.spotless.yaml;

import java.io.File;
import java.io.Serial;

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterStep;

/**
 * This step is a flag which marks that `ConfigurationCacheHackList` should
 * serialize each item individually into `byte[]` array, rather than using normal
 * serialization.
 *
 * The reason to use this is if you are using `toggleOffOn` *and* two kinds of
 * google-java-format (e.g. one for format and the other for imports), then
 * problems with Java's handling of object graphs will cause your up-to-date checks
 * to always fail. `CombinedJavaFormatStepTest` recreates this situation. By adding
 * this step, it will trigger this workaround which fixes the up-to-dateness bug.
 *
 * But, turning it on will break all `custom` steps that use Groovy closures. So
 * by default you get regular serialization. If you're using `toggleOffOn` and having
 * problems with up-to-dateness, then adding this step can be a workaround.
 */
public class SerializeToByteArrayHack implements FormatterStep {
	@Serial
	private static final long serialVersionUID = 8071047581828362545L;

	@Override
	public String getName() {
		return "hack to force serializing objects to byte array";
	}

	@Nullable @Override
	public String format(String rawUnix, File file) throws Exception {
		return null;
	}

	@Override
	public void close() throws Exception {

	}
}
