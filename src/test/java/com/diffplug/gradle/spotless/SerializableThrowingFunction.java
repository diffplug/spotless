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
package com.diffplug.gradle.spotless;

import java.io.Serializable;

import com.diffplug.common.base.Throwing;

interface SerializableThrowingFunction<T, R>
    extends Throwing.Function<T, R>, ToByteArray, Serializable {
  @SuppressWarnings("unchecked") // Safe as identity function always returns its argument
  static <T, R> SerializableThrowingFunction<T, R> identity() {
    return (SerializableThrowingFunction<T, R>) SerializableThrowingFunctionImpl.Identity.INSTANCE;
  }
}
