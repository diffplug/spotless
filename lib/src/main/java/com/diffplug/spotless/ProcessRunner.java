/*
 * Copyright 2020 DiffPlug
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Shelling out to a process is harder than it ought to be in Java.
 * If you don't read stdout and stderr on their own threads, you risk
 * deadlock on a clogged buffer.
 *
 * ProcessRunner allocates two threads specifically for the purpose of
 * flushing stdout and stderr to buffers.  These threads will remain alive until
 * the ProcessRunner is closed, so it is especially useful for repeated
 * calls to an external process.
 */
public class ProcessRunner implements AutoCloseable {
	private ExecutorService executor;

	public ProcessRunner() {
		this.executor = Executors.newFixedThreadPool(2);
	}

	/** Executes the given shell command (using `cmd` on windows and `sh` on unix). */
	public Result shell(String cmd) throws IOException, InterruptedException {
		return shellWinUnix(cmd, cmd);
	}

	/** Executes the given shell command (using `cmd` on windows and `sh` on unix). */
	public Result shellWinUnix(String cmdWin, String cmdUnix) throws IOException, InterruptedException {
		List<String> args;
		if (FileSignature.machineIsWin()) {
			args = Arrays.asList("cmd", "/c", cmdWin);
		} else {
			args = Arrays.asList("sh", "-c", cmdUnix);
		}
		return exec(args);
	}

	/** Creates a process with the given arguments. */
	public Result exec(String... args) throws IOException, InterruptedException {
		return exec(Arrays.asList(args));
	}

	/** Creates a process with the given arguments, the given byte array is written to stdin immediately. */
	public Result exec(byte[] stdin, String... args) throws IOException, InterruptedException {
		return exec(stdin, Arrays.asList(args));
	}

	/** Creates a process with the given arguments. */
	public Result exec(List<String> args) throws IOException, InterruptedException {
		return exec(new byte[0], args);
	}

	/** Creates a process with the given arguments, the given byte array is written to stdin immediately. */
	public Result exec(byte[] stdin, List<String> args) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(args);
		Process process = builder.start();
		Future<byte[]> outputFut = executor.submit(() -> drainToBytes(process.getInputStream()));
		Future<byte[]> errorFut = executor.submit(() -> drainToBytes(process.getErrorStream()));
		// write stdin
		process.getOutputStream().write(stdin);
		process.getOutputStream().close();
		// wait for the process to finish
		int exitCode = process.waitFor();
		try {
			// collect the output
			return new Result(args, exitCode, outputFut.get(), errorFut.get());
		} catch (ExecutionException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	private static void drain(InputStream input, OutputStream output) throws IOException {
		byte[] buf = new byte[1024];
		int numRead;
		while ((numRead = input.read(buf)) != -1) {
			output.write(buf, 0, numRead);
		}
	}

	private static byte[] drainToBytes(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		drain(input, output);
		return output.toByteArray();
	}

	@Override
	public void close() {
		executor.shutdown();
	}

	public static class Result {
		private final List<String> args;
		private final int exitCode;
		private final byte[] output, error;

		public Result(List<String> args, int exitCode, byte[] output, byte[] error) {
			this.args = args;
			this.exitCode = exitCode;
			this.output = output;
			this.error = error;
		}

		public List<String> args() {
			return args;
		}

		public int exitCode() {
			return exitCode;
		}

		public byte[] output() {
			return output;
		}

		public byte[] error() {
			return error;
		}

		public String assertNoError(Charset charset) {
			if (exitCode == 0) {
				return new String(output, charset);
			} else {
				throw new RuntimeException("exit code: " + exitCode + "\nstderr: " + new String(error, charset));
			}
		}
	}
}
