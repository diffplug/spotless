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

/*
 * Forked from
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Based on FormatterToken from https://github.com/serge-rider/dbeaver,
 * which itself is licensed under the Apache 2.0 license.
 */
class FormatterToken {

	private TokenType fType;
	private String fString;
	private int fPos = -1;

	FormatterToken(final TokenType argType, final String argString, final int argPos) {
		fType = argType;
		fString = argString;
		fPos = argPos;
	}

	FormatterToken(final TokenType argType, final String argString) {
		this(argType, argString, -1);
	}

	public void setType(final TokenType argType) {
		fType = argType;
	}

	public TokenType getType() {
		return fType;
	}

	public void setString(final String argString) {
		fString = argString;
	}

	public String getString() {
		return fString;
	}

	public void setPos(final int argPos) {
		fPos = argPos;
	}

	public int getPos() {
		return fPos;
	}

	public String toString() {
		final var buf = new StringBuilder();
		buf.append(getClass().getName());
		buf.append("type=").append(fType);
		buf.append(",string=").append(fString);
		buf.append(",pos=").append(fPos);
		buf.append("]");
		return buf.toString();
	}
}
