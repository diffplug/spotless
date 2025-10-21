/*
 * Copyright 2020-2025 DiffPlug
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shelling out to a process is harder than it ought to be in Java.
 * If you don't read stdout and stderr on their own threads, you risk
 * deadlock on a clogged buffer.
 * <p>
 * ProcessRunner allocates two threads specifically for the purpose of
 * flushing stdout and stderr to buffers.  These threads will remain alive until
 * the ProcessRunner is closed, so it is especially useful for repeated
 * calls to an external process.
 */
public class ProcessRunner implements AutoCloseable {
	private final ExecutorService threadStdOut = Executors.newSingleThreadExecutor();
	private final ExecutorService threadStdErr = Executors.newSingleThreadExecutor();
	private final ByteArrayOutputStream bufStdOut;
	private final ByteArrayOutputStream bufStdErr;

	public ProcessRunner() {
		this(-1);
	}

	public static ProcessRunner usingRingBuffersOfCapacity(int limit) {
		return new ProcessRunner(limit);
	}

	private ProcessRunner(int limitedBuffers) {
		this.bufStdOut = limitedBuffers >= 0 ? new RingBufferByteArrayOutputStream(limitedBuffers) : new ByteArrayOutputStream();
		this.bufStdErr = limitedBuffers >= 0 ? new RingBufferByteArrayOutputStream(limitedBuffers) : new ByteArrayOutputStream();
	}

	/** Executes the given shell command (using {@code cmd} on windows and {@code sh} on unix). */
	public Result shell(String cmd) throws IOException, InterruptedException {
		return shellWinUnix(cmd, cmd);
	}

	/** Executes the given shell command (using {@code cmd} on windows and {@code sh} on unix). */
	public Result shellWinUnix(String cmdWin, String cmdUnix) throws IOException, InterruptedException {
		return shellWinUnix(null, null, cmdWin, cmdUnix);
	}

	/** Executes the given shell command (using {@code cmd} on windows and {@code sh} on unix). */
	public Result shellWinUnix(@Nullable File cwd, @Nullable Map<String, String> environment, String cmdWin, String cmdUnix) throws IOException, InterruptedException {
		List<String> args;
		if (FileSignature.machineIsWin()) {
			args = Arrays.asList("cmd", "/c", cmdWin);
		} else {
			args = Arrays.asList("sh", "-c", cmdUnix);
		}
		return exec(cwd, environment, null, args);
	}

	/** Creates a process with the given arguments. */
	public Result exec(String... args) throws IOException, InterruptedException {
		return exec(Arrays.asList(args));
	}

	/** Creates a process with the given arguments, the given byte array is written to stdin immediately. */
	public Result exec(@Nullable byte[] stdin, String... args) throws IOException, InterruptedException {
		return exec(stdin, Arrays.asList(args));
	}

	/** Creates a process with the given arguments. */
	public Result exec(List<String> args) throws IOException, InterruptedException {
		return exec(null, args);
	}

	/** Creates a process with the given arguments, the given byte array is written to stdin immediately. */
	public Result exec(@Nullable byte[] stdin, List<String> args) throws IOException, InterruptedException {
		return exec(null, null, stdin, args);
	}

	/** Creates a process with the given arguments, the given byte array is written to stdin immediately. */
	public Result exec(@Nullable File cwd, @Nullable Map<String, String> environment, @Nullable byte[] stdin, List<String> args) throws IOException, InterruptedException {
		LongRunningProcess process = start(cwd, environment, stdin, args);
		try {
			// wait for the process to finish
			process.waitFor();
			// collect the output
			return process.result();
		} catch (ExecutionException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	/**
	 * Creates a process with the given arguments, the given byte array is written to stdin immediately.
	 * <br>
	 * Delegates to {@link #start(File, Map, byte[], boolean, List)} with {@code false} for {@code redirectErrorStream}.
	 */
	public LongRunningProcess start(@Nullable File cwd, @Nullable Map<String, String> environment, @Nullable byte[] stdin, List<String> args) throws IOException {
		return start(cwd, environment, stdin, false, args);
	}

	/**
	 * Creates a process with the given arguments, the given byte array is written to stdin immediately.
	 * <br>
	 * The process is not waited for, so the caller is responsible for calling {@link LongRunningProcess#waitFor()} (if needed).
	 * <br>
	 * To dispose this {@code ProcessRunner} instance, either call {@link #close()} or {@link LongRunningProcess#close()}. After
	 * {@link #close()} or {@link LongRunningProcess#close()} has been called, this {@code ProcessRunner} instance must not be used anymore.
	 */
	public LongRunningProcess start(@Nullable File cwd, @Nullable Map<String, String> environment, @Nullable byte[] stdin, boolean redirectErrorStream, List<String> args) throws IOException {
		checkState();
		ProcessBuilder builder = new ProcessBuilder(args);
		if (cwd != null) {
			builder.directory(cwd);
		}
		if (environment != null) {
			builder.environment().putAll(environment);
		}
		if (stdin == null) {
			stdin = new byte[0];
		}
		if (redirectErrorStream) {
			builder.redirectErrorStream(true);
		}

		Process process = builder.start();
		Future<byte[]> outputFut = threadStdOut.submit(() -> drainToBytes(process.getInputStream(), bufStdOut));
		Future<byte[]> errorFut = null;
		if (!redirectErrorStream) {
			errorFut = threadStdErr.submit(() -> drainToBytes(process.getErrorStream(), bufStdErr));
		}
		// write stdin
		process.getOutputStream().write(stdin);
		process.getOutputStream().flush();
		process.getOutputStream().close();
		return new LongRunningProcess(process, args, outputFut, errorFut);
	}

	private static void drain(InputStream input, OutputStream output) throws IOException {
		byte[] buf = new byte[1024];
		int numRead;
		while ((numRead = input.read(buf)) != -1) {
			output.write(buf, 0, numRead);
		}
	}

	private static byte[] drainToBytes(InputStream input, ByteArrayOutputStream buffer) throws IOException {
		buffer.reset();
		drain(input, buffer);
		return buffer.toByteArray();
	}

	@Override
	public void close() {
		threadStdOut.shutdown();
		threadStdErr.shutdown();
	}

	/** Checks if this {@code ProcessRunner} instance is still usable. */
	private void checkState() {
		if (threadStdOut.isShutdown() || threadStdErr.isShutdown()) {
			throw new IllegalStateException("ProcessRunner has been closed and must not be used anymore.");
		}
	}

	public static class Result {
		private final List<String> args;
		private final int exitCode;
		private final byte[] stdOut;
		private final byte[] stdErr;

		public Result(@Nonnull List<String> args, int exitCode, @Nonnull byte[] stdOut, @Nullable byte[] stdErr) {
			this.args = args;
			this.exitCode = exitCode;
			this.stdOut = stdOut;
			this.stdErr = stdErr == null ? new byte[0] : stdErr;
		}

		public List<String> args() {
			return args;
		}

		public int exitCode() {
			return exitCode;
		}

		public byte[] stdOut() {
			return stdOut;
		}

		public byte[] stdErr() {
			return stdErr;
		}

		public String stdOutUtf8() {
			return new String(stdOut, UTF_8);
		}

		public String stdErrUtf8() {
			return new String(stdErr, UTF_8);
		}

		/** Returns true if the exit code was not zero. */
		public boolean exitNotZero() {
			return exitCode != 0;
		}

		/**
		 * Asserts that the exit code was zero, and if so, returns
		 * the content of stdout encoded with the given charset.
		 * <p>
		 * If the exit code was not zero, throws an exception
		 * with useful debugging information.
		 */
		public String assertExitZero(Charset charset) {
			if (exitCode == 0) {
				return new String(stdOut, charset);
			} else {
				throw new RuntimeException(toString());
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("> arguments: ").append(args).append("\n");
			builder.append("> exit code: ").append(exitCode).append("\n");
			BiConsumer<String, byte[]> perStream = (name, content) -> {
				String string = new String(content, Charset.defaultCharset()).trim();
				if (string.isEmpty()) {
					builder.append("> ").append(name).append(": (empty)\n");
				} else {
					String[] lines = string.replace("\r", "").split("\n");
					if (lines.length == 1) {
						builder.append("> " + name + ": " + lines[0] + "\n");
					} else {
						builder.append("> ").append(name).append(": (below)\n");
						for (String line : lines) {
							builder.append("> ");
							builder.append(line);
							builder.append('\n');
						}
					}
				}
			};
			perStream.accept("   stdout", stdOut);
			if (stdErr.length > 0) {
				perStream.accept("   stderr", stdErr);
			}
			return builder.toString();
		}
	}

	/**
	 * A long-running process that can be waited for.
	 */
	public class LongRunningProcess extends Process implements AutoCloseable {

		private final Process delegate;
		private final List<String> args;
		private final Future<byte[]> outputFut;
		private final Future<byte[]> errorFut;

		public LongRunningProcess(@Nonnull Process delegate, @Nonnull List<String> args, @Nonnull Future<byte[]> outputFut, @Nullable Future<byte[]> errorFut) {
			this.delegate = requireNonNull(delegate);
			this.args = args;
			this.outputFut = outputFut;
			this.errorFut = errorFut;
		}

		@Override
		public OutputStream getOutputStream() {
			return delegate.getOutputStream();
		}

		@Override
		public InputStream getInputStream() {
			return delegate.getInputStream();
		}

		@Override
		public InputStream getErrorStream() {
			return delegate.getErrorStream();
		}

		@Override
		public int waitFor() throws InterruptedException {
			return delegate.waitFor();
		}

		@Override
		public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
			return delegate.waitFor(timeout, unit);
		}

		@Override
		public int exitValue() {
			return delegate.exitValue();
		}

		@Override
		public void destroy() {
			delegate.destroy();
		}

		@Override
		public Process destroyForcibly() {
			return delegate.destroyForcibly();
		}

		@Override
		public boolean isAlive() {
			return delegate.isAlive();
		}

		public Result result() throws ExecutionException, InterruptedException {
			int exitCode = waitFor();
			return new Result(args, exitCode, this.outputFut.get(), (this.errorFut != null ? this.errorFut.get() : null));
		}

		@Override
		public void close() {
			if (isAlive()) {
				destroy();
			}
			ProcessRunner.this.close();
		}
	}
}
