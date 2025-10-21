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
package com.diffplug.spotless.glue.java;

import static org.assertj.core.api.Assertions.assertThat;

import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaCleanthatRefactorerFuncTest {
	@Test
	public void testMutatorsDetection() {
		assertThat(JavaRefactorer.getAllIncluded()).isNotEmpty();
	}
}
