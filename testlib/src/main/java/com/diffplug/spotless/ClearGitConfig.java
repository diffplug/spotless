/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(ClearGitConfig.GitConfigExtension.class)
@ResourceLock(value = "GIT", mode = ResourceAccessMode.READ_WRITE)
public @interface ClearGitConfig {

	class GitConfigExtension implements BeforeAllCallback, AfterAllCallback {

		@Override
		public void beforeAll(ExtensionContext extensionContext) throws Exception {
			for (var config : getConfigs()) {
				config.clear();
			}
		}

		@Override
		public void afterAll(ExtensionContext extensionContext) throws Exception {
			for (var config : getConfigs()) {
				config.load();
			}
		}

		private List<StoredConfig> getConfigs() throws Exception {
			var reader = SystemReader.getInstance();
			return Arrays.asList(reader.getUserConfig(), reader.getSystemConfig());
		}
	}
}
