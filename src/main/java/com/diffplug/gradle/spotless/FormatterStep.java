package com.diffplug.gradle.spotless;


/**
 * An implementation of this class specifies a single step in a formatting process.
 * 
 * The input is guaranteed to have unix-style newlines, and the output is required
 * to not introduce any windows-style newlines as well.
 */
public class FormatterStep {
	private final String name;
	private final Throwing.Function<String, String> formatter;

	private FormatterStep(String name, Throwing.Function<String, String> formatter) {
		this.name = name;
		this.formatter = formatter;
	}

	/** The name of the step, for debugging purposes. */
	public String getName() {
		return name;
	}

	/**
	 * Returns a formatted version of the given content.
	 * 
	 * @param raw File's content, guaranteed to have unix-style newlines ('\n')
	 * @return The formatted content, guaranteed to only have unix-style newlines 
	 * @throws Throwable 
	 */
	public String format(String raw) throws Throwable {
		return formatter.apply(raw);
	}

	/** Creates a FormatterStep from the given function. */
	public static FormatterStep create(String name, Throwing.Function<String, String> formatter) {
		return new FormatterStep(name, formatter);
	}

	/** Creates a FormatterStep lazily from the given formatterSupplier function. */
	public static FormatterStep createLazy(String name, Throwing.Supplier<Throwing.Function<String, String>> formatterSupplier) {
		return new FormatterStep(name, new Throwing.Function<String, String>() {
			private Throwing.Function<String, String> formatter;

			@Override
			public String apply(String content) throws Throwable {
				if (formatter == null) {
					formatter = formatterSupplier.get();
				}
				return formatter.apply(content);
			}
		});
	}
}
