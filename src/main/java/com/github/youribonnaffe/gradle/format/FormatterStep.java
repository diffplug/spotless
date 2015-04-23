package com.github.youribonnaffe.gradle.format;

public abstract class FormatterStep {
	public boolean isClean(String content) throws Exception {
		return format(content).equals(content);
	}

	public abstract String format(String content) throws Exception;

	public static FormatterStep NO_OP = new FormatterStep() {
		@Override
		public String format(String content) throws Exception {
			return content;
		}
	};
}
