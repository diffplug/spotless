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
package com.diffplug.spotless.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signifies that a {@code public} API is actually an implementation detail, and should be treated as if it
 * were {@code private}.
 *
 * The user of the API should be warned that it may unexpectedly disappear in future versions of
 * Spotless. Usually the best place to put this warning is in the API's class JavaDoc.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.ANNOTATION_TYPE,
		ElementType.CONSTRUCTOR,
		ElementType.FIELD,
		ElementType.METHOD,
		ElementType.TYPE
})
@Documented
public @interface Internal {

}
