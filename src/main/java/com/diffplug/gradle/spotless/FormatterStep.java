package com.diffplug.gradle.spotless;

/**
 * An implementation of this class specifies a single step in a formatting process.
 * 
 * The input is guaranteed to have unix-style newlines, and the output is required
 * to not introduce any windows-style newlines as well.
 */
public interface FormatterStep {
	/** The name of the step, for debugging purposes. */
	String getName();

	/**
	 * Returns a formatted version of the given content.
	 * 
	 * @param content File's content, guaranteed to have unix-style newlines ('\n')
	 * @return The formatted content, required to only have unix-style newlines 
	 * @throws Exception
	 */
	String format(String content) throws Exception;
}
