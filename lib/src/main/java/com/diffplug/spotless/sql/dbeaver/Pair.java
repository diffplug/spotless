/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.sql.dbeaver;

/**
 * Forked from
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * <p>
 * Based on Pair from https://github.com/serge-rider/dbeaver,
 * which itself is licensed under the Apache 2.0 license.
 */
class Pair<T1, T2> {
	private T1 first;
	private T2 second;

	Pair(T1 first, T2 second) {
		this.first = first;
		this.second = second;
	}

	T1 getFirst() {
		return first;
	}

	void setFirst(T1 first) {
		this.first = first;
	}

	T2 getSecond() {
		return second;
	}

	void setSecond(T2 second) {
		this.second = second;
	}

	@Override
	public String toString() {
		return first + "=" + second;
	}
}
