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

public class LineEndingStep {

	private final EOLNormalizer normalizer;

	public LineEndingStep(LineEnding lineEnding) {
		this(lineEnding, new LineEndingService());
	}

	LineEndingStep(LineEnding lineEnding, LineEndingService lineEndingService) {
		normalizer = forLineEnding(lineEnding, lineEndingService);
	}

	private static EOLNormalizer forLineEnding(LineEnding lineEnding, LineEndingService lineEndingService) {
		switch (lineEnding) {
		case UNIX:
			return new UnixEOLNormalizer();
		case WINDOWS:
			return new WindowsEOLNormalizer();
		case PLATFORM_NATIVE:
			return new PlatformNativeEOLNormalizer(lineEndingService);
		case DERIVED:
			return new DerivedEOLNormalizer(lineEndingService);
		case UNCERTAIN:
			return new NoEOLNormalizer();
		default:
			throw new IllegalArgumentException("No EOLNormalizer specified for LineEnding");
		}
	}

	public String format(String raw) {
		return normalizer.format(raw);
	}

	public LineEnding getConcreteLineEnding() {
		return normalizer.getConcreteLineEnding();
	}

	static interface EOLNormalizer {
		String format(String input);

		/**
		 * @deprecated Only used for interim purpose.
		 */
		LineEnding getConcreteLineEnding();
	}

	static class UnixEOLNormalizer implements EOLNormalizer {
		@Override
		public String format(String input) {
			return input.replace("\r\n", "\n");
		}

		@Override
		public LineEnding getConcreteLineEnding() {
			return LineEnding.UNIX;
		}

	}

	static class WindowsEOLNormalizer implements EOLNormalizer {
		@Override
		public String format(String input) {
			String unix = input.replace("\r\n", "\n");
			return unix.replace("\n", "\r\n");
		}

		@Override
		public LineEnding getConcreteLineEnding() {
			return LineEnding.WINDOWS;
		}
	}

	static class NoEOLNormalizer implements EOLNormalizer {
		@Override
		public String format(String input) {
			return input;
		}

		@Override
		public LineEnding getConcreteLineEnding() {
			throw new UnsupportedOperationException();
		}
	}

	static class DerivedEOLNormalizer implements EOLNormalizer {
		private EOLNormalizer delegate;
		private LineEndingService lineEndingService;

		public DerivedEOLNormalizer(LineEndingService lineEndingService) {
			this.lineEndingService = lineEndingService;
		}

		@Override
		public String format(String input) {
			LineEnding lineEnding = lineEndingService.determineLineEnding(input);
			switch (lineEnding) {
			case UNCERTAIN:
				delegate = new NoEOLNormalizer();
				break;
			case UNIX:
				delegate = new UnixEOLNormalizer();
				break;
			case WINDOWS:
				delegate = new WindowsEOLNormalizer();
				break;
			default:
				throw new IllegalStateException("Unexpected result from LineEndingService");
			}
			return delegate.format(input);
		}

		@Override
		public LineEnding getConcreteLineEnding() {
			throw new UnsupportedOperationException();
		}
	}

	static class PlatformNativeEOLNormalizer implements EOLNormalizer {
		private EOLNormalizer delegate;

		public PlatformNativeEOLNormalizer(LineEndingService lineEndingService) {
			LineEnding lineEnding = lineEndingService.getPlatformLineEnding();
			switch (lineEnding) {
			case UNIX:
				delegate = new UnixEOLNormalizer();
				break;
			case WINDOWS:
				delegate = new WindowsEOLNormalizer();
				break;
			default:
				throw new IllegalStateException("Unexpected result from LineEndingService");
			}
		}

		@Override
		public String format(String input) {
			return delegate.format(input);
		}

		@Override
		public LineEnding getConcreteLineEnding() {
			return delegate.getConcreteLineEnding();
		}
	}

}
