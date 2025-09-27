/*
 * Copyright 2023-2025 DiffPlug
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class RingBufferByteArrayOutputStream extends ByteArrayOutputStream {

	private final int limit;

	private int zeroIndexPointer;

	private boolean isOverLimit;

	public RingBufferByteArrayOutputStream(int limit) {
		this(limit, 32);
	}

	public RingBufferByteArrayOutputStream(int limit, int initialCapacity) {
		super(initialCapacity);
		if (limit < initialCapacity) {
			throw new IllegalArgumentException("Limit must be greater than initial capacity. Limit: " + limit + ", initial capacity: " + initialCapacity);
		}
		if (limit < 2) {
			throw new IllegalArgumentException("Limit must be greater than or equal to 2 but is " + limit);
		}
		if (limit % 2 != 0) {
			throw new IllegalArgumentException("Limit must be an even number but is " + limit); // to fit 16 bit unicode chars
		}
		this.limit = limit;
	}

	// ---- writing
	@Override
	public synchronized void write(int b) {
		if (count < limit) {
			super.write(b);
			return;
		}
		isOverLimit = true;
		buf[zeroIndexPointer] = (byte) b;
		zeroIndexPointer = (zeroIndexPointer + 1) % limit;
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) {
		int remaining = limit - count;
		if (remaining >= len) {
			super.write(b, off, len);
			return;
		}
		if (remaining > 0) {
			// write what we can "normally"
			super.write(b, off, remaining);
			// rest delegated
			write(b, off + remaining, len - remaining);
			return;
		}
		// we are over the limit
		isOverLimit = true;
		// write till limit is reached
		int writeTillLimit = Math.min(len, limit - zeroIndexPointer);
		System.arraycopy(b, off, buf, zeroIndexPointer, writeTillLimit);
		zeroIndexPointer = (zeroIndexPointer + writeTillLimit) % limit;
		if (writeTillLimit < len) {
			// write rest
			write(b, off + writeTillLimit, len - writeTillLimit);
		}
	}

	@Override
	public synchronized void reset() {
		super.reset();
		zeroIndexPointer = 0;
		isOverLimit = false;
	}

	// ---- output
	@Override
	public synchronized void writeTo(OutputStream out) throws IOException {
		if (!isOverLimit) {
			super.writeTo(out);
			return;
		}
		out.write(buf, zeroIndexPointer, limit - zeroIndexPointer);
		out.write(buf, 0, zeroIndexPointer);
	}

	@Override
	public synchronized byte[] toByteArray() {
		if (!isOverLimit) {
			return super.toByteArray();
		}
		byte[] result = new byte[limit];
		System.arraycopy(buf, zeroIndexPointer, result, 0, limit - zeroIndexPointer);
		System.arraycopy(buf, 0, result, limit - zeroIndexPointer, zeroIndexPointer);
		return result;
	}

	@SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "We want to use the default encoding here since this is contract on ByteArrayOutputStream")
	@Override
	public synchronized String toString() {
		if (!isOverLimit) {
			return super.toString();
		}
		return new String(buf, zeroIndexPointer, limit - zeroIndexPointer) + new String(buf, 0, zeroIndexPointer);
	}

	@Override
	public synchronized String toString(String charsetName) throws UnsupportedEncodingException {
		if (!isOverLimit) {
			return super.toString(charsetName);
		}
		return new String(buf, zeroIndexPointer, limit - zeroIndexPointer, charsetName) + new String(buf, 0, zeroIndexPointer, charsetName);
	}

}
