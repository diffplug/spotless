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
package com.diffplug.spotless;

import static com.diffplug.spotless.TimedLogger.MESSAGE_PREFIX_BEGIN;
import static com.diffplug.spotless.TimedLogger.MESSAGE_PREFIX_END;
import static com.diffplug.spotless.TimedLogger.MESSAGE_SUFFIX_TOOK;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;

import com.diffplug.spotless.TimedLogger.TestTicker;

class TimedLoggerTest {

	private TestLogger testLogger;

	private TestTicker testTicker;

	private TimedLogger timedLogger;

	@BeforeEach
	void setUp() {
		testLogger = new TestLogger();
		testTicker = new TestTicker();
		timedLogger = TimedLogger.forLogger(testLogger, testTicker);
	}

	@Test
	void itDoesNotLogWhenLevelDisabled() throws InterruptedException {

		TestLogger logger = new TestLogger() {
			@Override
			public boolean isInfoEnabled() {
				return false;
			}

			@Override
			public boolean isDebugEnabled() {
				return false;
			}

			@Override
			public boolean isTraceEnabled() {
				return false;
			}
		};
		TimedLogger timedLogger = TimedLogger.forLogger(logger);

		timedLogger.withInfo("This should not be logged").run(() -> Thread.sleep(1));
		logger.assertNoEvents();
	}

	@Test
	void itLogsMillisWhenTakingMillis() {
		timedLogger.withInfo("This should be logged").run(() -> testTicker.tickMillis(999));

		testLogger.assertEvents(2);
		testLogger.assertHasEventWithMessageAndArguments(MESSAGE_SUFFIX_TOOK, "999ms");
	}

	@Test
	void itLogsSecondsOnlyWhenTakingSeconds() {
		timedLogger.withInfo("This should be logged").run(() -> testTicker.tickMillis(2_000));

		testLogger.assertEvents(2);
		testLogger.assertHasEventWithMessageAndArguments(MESSAGE_SUFFIX_TOOK, "2s");
	}

	@Test
	void itLogsMinutesOnlyWhenTakingMinutes() {
		timedLogger.withInfo("This should be logged").run(() -> testTicker.tickMillis(2 * 60 * 1_000));

		testLogger.assertEvents(2);
		testLogger.assertHasEventWithMessageAndArguments(MESSAGE_SUFFIX_TOOK, "2m");
	}

	@Test
	void itLogsMinutesAndSecondsWhenTakingMinutesAndSeconds() {
		timedLogger.withInfo("This should be logged").run(() -> testTicker.tickMillis(2 * 60 * 1_000 + 3 * 1_000));

		testLogger.assertEvents(2);
		testLogger.assertHasEventWithMessageAndArguments(MESSAGE_SUFFIX_TOOK, "2m 3.0s");
	}

	@Test
	void itLogsBeginAndEndPrefixes() {
		timedLogger.withInfo("This should be logged").run(() -> testTicker.tickMillis(1));

		testLogger.assertHasEventWithMessageAndArguments(MESSAGE_PREFIX_BEGIN);
		testLogger.assertHasEventWithMessageAndArguments(MESSAGE_PREFIX_END, "1ms");
	}

	@Test
	void itThrowsExceptionsInChecked() {
		Assertions.assertThatThrownBy(() -> timedLogger.withInfo("This should be logged").runChecked(() -> {
			throw new Exception("This is an exception");
		})).isInstanceOf(Exception.class).hasMessage("This is an exception");
	}

	@Test
	void itLogsEvenWhenExceptionsAreThrown() {
		Assertions.assertThatThrownBy(() -> timedLogger.withInfo("This should be logged").run(() -> {
			testTicker.tickMillis(2);
			throw new Exception("This is an exception");
		})).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("This is an exception")
				.hasCauseInstanceOf(Exception.class);

		testLogger.assertEvents(2);
		testLogger.assertHasEventWithMessageAndArguments(MESSAGE_PREFIX_BEGIN);
		testLogger.assertHasEventWithMessageAndArguments(MESSAGE_PREFIX_END, "2ms");
	}

	private static class TestLogger extends LegacyAbstractLogger {

		private final List<TestLoggingEvent> events = new LinkedList<>();

		@Override
		protected String getFullyQualifiedCallerName() {
			return TestLogger.class.getName();
		}

		@Override
		protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
			events.add(new TestLoggingEvent(level, marker, msg, arguments, throwable));
		}

		@Override
		public boolean isTraceEnabled() {
			return true;
		}

		@Override
		public boolean isDebugEnabled() {
			return true;
		}

		@Override
		public boolean isInfoEnabled() {
			return true;
		}

		@Override
		public boolean isWarnEnabled() {
			return true;
		}

		@Override
		public boolean isErrorEnabled() {
			return true;
		}

		public List<TestLoggingEvent> getEvents() {
			return events;
		}

		public void assertNoEvents() {
			Assertions.assertThat(getEvents()).isEmpty();
		}

		public void assertEvents(int eventCount) {
			Assertions.assertThat(getEvents()).hasSize(eventCount);
		}

		public void assertHasEventWithMessageAndArguments(String message, Object... arguments) {

			Assertions.assertThat(getEvents()).haveAtLeastOne(new Condition<>(event -> {
				if (!event.msg().contains(message)) {
					return false;
				}
				if (event.arguments().length != arguments.length) {
					return false;
				}
				for (int i = 0; i < arguments.length; i++) {
					if (!String.valueOf(event.arguments()[i]).equals(arguments[i])) {
						return false;
					}
				}
				return true;
			}, "Event with message containing '%s' and arguments '%s'", message, Arrays.toString(arguments)));
		}
	}

	private static class TestLoggingEvent {

		private final Level level;
		private final Marker marker;
		private final String msg;
		private final Object[] arguments;
		private final Throwable throwable;

		public TestLoggingEvent(Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
			this.level = level;
			this.marker = marker;
			this.msg = msg;
			this.arguments = arguments;
			this.throwable = throwable;
		}

		public Level level() {
			return level;
		}

		public Marker marker() {
			return marker;
		}

		public String msg() {
			return msg;
		}

		public Object[] arguments() {
			return arguments;
		}

		public Throwable throwable() {
			return throwable;
		}

		@Override
		public String toString() {
			return String.format(
					"TestLoggingEvent[level=%s, marker=%s, msg=%s, arguments=%s, throwable=%s]",
					this.level,
					this.marker,
					this.msg,
					Arrays.toString(this.arguments),
					this.throwable);
		}
	}

}
