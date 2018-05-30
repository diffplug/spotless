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
package com.diffplug.spotless.extra.config;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

/** UserArgumentException shall contain information about the missing argument or its value.*/
public class UserArgumentExceptionTest {
	private static final Integer USER_VALUE = 5;
	private static final String USER_VALUE_VERBOOSE = IntStream.range(1, 20).mapToObj(i -> String.format("%02d", i)).collect(Collectors.joining(""));
	private static final String ERROR_MESSAER = "My error message";

	@Test
	public void userValuePartOfMessage() {
		UserArgumentException e = new UserArgumentException(USER_VALUE, ERROR_MESSAER);
		assertThat(e.getMessage()).contains(USER_VALUE.toString(), ERROR_MESSAER);
	}

	@Test
	public void missingValueReported() {
		UserArgumentException e = new UserArgumentException(null, ERROR_MESSAER);
		assertThat(e.getMessage()).contains("'null'", "not set", ERROR_MESSAER);
	}

	@Test
	public void verboseValueWrapped() {
		UserArgumentException e = new UserArgumentException(USER_VALUE_VERBOOSE, ERROR_MESSAER);
		assertThat(e.getMessage()).contains(String.format("%n'01"), String.format("19':%n"));
	}

}
