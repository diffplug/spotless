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
package com.diffplug.spotless.npm;

import static com.diffplug.spotless.LazyArgLogger.lazy;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.diffplug.spotless.ThrowingEx;

/**
 * A logger that logs the time it took to execute a block of code.
 */
final class TimedLogger {

	public static final String MESSAGE_PREFIX_BEGIN = "[BEGIN] ";

	public static final String MESSAGE_PREFIX_END = "[END] ";

	public static final String MESSAGE_SUFFIX_TOOK = " (took {})";

	private final Logger logger;
	private final Ticker ticker;

	private TimedLogger(@Nonnull Logger logger, Ticker ticker) {
		this.logger = Objects.requireNonNull(logger);
		this.ticker = ticker;
	}

	public static TimedLogger forLogger(@Nonnull Logger logger) {
		return forLogger(logger, Ticker.systemTicker());
	}

	public static TimedLogger forLogger(@Nonnull Logger logger, Ticker ticker) {
		return new TimedLogger(logger, ticker);
	}

	public TimedExec withInfo(@Nonnull String message, Object... args) {
		return new TimedExec(logger::isInfoEnabled, logger::info, ticker, message, args);
	}

	public TimedExec withDebug(@Nonnull String message, Object... args) {
		return new TimedExec(logger::isDebugEnabled, logger::debug, ticker, message, args);
	}

	public TimedExec withTrace(@Nonnull String message, Object... args) {
		return new TimedExec(logger::isTraceEnabled, logger::trace, ticker, message, args);
	}

	public TimedExec withWarn(@Nonnull String message, Object... args) {
		return new TimedExec(logger::isWarnEnabled, logger::warn, ticker, message, args);
	}

	public TimedExec withError(@Nonnull String message, Object... args) {
		return new TimedExec(logger::isErrorEnabled, logger::error, ticker, message, args);
	}

	public static class Timed implements AutoCloseable {

		@Nonnull
		private final String msg;

		@Nonnull
		private final List<Object> params;
		@Nonnull
		private final LogToLevelMethod delegatedLogger;
		@Nonnull
		private final Ticker ticker;

		private final long startedAt;

		public Timed(@Nonnull Ticker ticker, @Nonnull String msg, @Nonnull List<Object> params, @Nonnull LogToLevelMethod delegatedLogger) {
			this.ticker = Objects.requireNonNull(ticker);
			this.msg = Objects.requireNonNull(msg);
			this.params = List.copyOf(Objects.requireNonNull(params));
			this.delegatedLogger = Objects.requireNonNull(delegatedLogger);
			this.startedAt = ticker.read();
			logStart();
		}

		private void logStart() {
			delegatedLogger.log(MESSAGE_PREFIX_BEGIN + msg, params.toArray());
		}

		private void logEnd() {
			delegatedLogger.log(MESSAGE_PREFIX_END + msg + MESSAGE_SUFFIX_TOOK, paramsForEnd());
		}

		@Override
		public final void close() {
			logEnd();
		}

		private Object[] paramsForEnd() {
			if (params.isEmpty() || !(params.get(params.size() - 1) instanceof Throwable)) {
				// if the last element is not a throwable, we can add the duration as the last element
				return Stream.concat(params.stream(), Stream.of(lazy(this::durationString))).toArray();
			}
			// if the last element is a throwable, we have to add the duration before the last element
			return Stream.concat(
					params.stream().limit(params.size() - 1),
					Stream.of(lazy(this::durationString),
							params.get(params.size() - 1)))
					.toArray();
		}

		private String durationString() {
			long duration = ticker.read() - startedAt;
			if (duration < 1000) {
				return duration + "ms";
			} else if (duration < 1000 * 60) {
				long seconds = duration / 1000;
				long millis = duration - seconds * 1000;
				return seconds + "." + millis + "s";
			} else {
				// output in the format 3m 4.321s
				long minutes = duration / (1000 * 60);
				long seconds = (duration - minutes * 1000 * 60) / 1000;
				long millis = duration - minutes * 1000 * 60 - seconds * 1000;
				return minutes + "m" + (seconds + millis > 0 ? " " + seconds + "." + millis + "s" : "");
			}
		}
	}

	public static final class NullStopWatchLogger extends Timed {
		private static final NullStopWatchLogger INSTANCE = new NullStopWatchLogger();

		private NullStopWatchLogger() {
			super(Ticker.systemTicker(), "", List.of(), (m, a) -> {});
		}
	}

	interface Ticker {
		long read();

		static Ticker systemTicker() {
			return System::currentTimeMillis;
		}
	}

	static class TestTicker implements Ticker {
		private long time;

		@Override
		public long read() {
			return time;
		}

		public void tickMillis(long millis) {
			time += millis;
		}
	}

	public static class TimedExec {
		@Nonnull
		private final LogActiveMethod logActiveMethod;
		@Nonnull
		private final LogToLevelMethod logMethod;
		@Nonnull
		private final Ticker ticker;
		@Nonnull
		private final String message;
		@Nonnull
		private final Object[] args;

		public TimedExec(LogActiveMethod logActiveMethod, LogToLevelMethod logMethod, Ticker ticker, String message, Object... args) {
			this.logActiveMethod = Objects.requireNonNull(logActiveMethod);
			this.logMethod = Objects.requireNonNull(logMethod);
			this.ticker = Objects.requireNonNull(ticker);
			this.message = Objects.requireNonNull(message);
			this.args = Objects.requireNonNull(args);
		}

		public void run(ThrowingEx.Runnable r) {
			try (Timed ignore = timed()) {
				ThrowingEx.run(r);
			}
		}

		public <T> T call(ThrowingEx.Supplier<T> s) {
			try (Timed ignore = timed()) {
				return ThrowingEx.get(s);
			}
		}

		public void runChecked(ThrowingEx.Runnable r) throws Exception {
			try (Timed ignore = timed()) {
				r.run();
			}
		}

		private Timed timed() {
			if (logActiveMethod.isLogLevelActive()) {
				return new Timed(ticker, message, List.of(args), logMethod);
			}
			return NullStopWatchLogger.INSTANCE;
		}
	}

	@FunctionalInterface
	private interface LogActiveMethod {
		boolean isLogLevelActive();
	}

	@FunctionalInterface
	private interface LogToLevelMethod {
		void log(String message, Object... args);
	}
}
