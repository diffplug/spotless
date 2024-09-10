package com.diffplug.spotless.rdf;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class RdfFormatterConfig implements Serializable{
	private static final long serialVersionId = 1L;
	private boolean failOnWarning = true;
	private boolean useTurtleFormatter = true;
	private String turtleFormatterVersion = RdfFormatterStep.LATEST_TURTLE_FORMATTER_VERSION;
	private boolean verify = true;

	public RdfFormatterConfig() {
	}

	public void setFailOnWarning(boolean failOnWarning) {
		this.failOnWarning = failOnWarning;
	}

	public boolean isFailOnWarning() {
		return failOnWarning;
	}

	public boolean isVerify() {
		return verify;
	}

	public void setVerify(boolean verify) {
		this.verify = verify;
	}

	public static Builder builder(){
		return new Builder();
	}

	public boolean isUseTurtleFormatter() {
		return useTurtleFormatter;
	}

	public void setUseTurtleFormatter(boolean useTurtleFormatter) {
		this.useTurtleFormatter = useTurtleFormatter;
	}

	public String getTurtleFormatterVersion() {
		return turtleFormatterVersion;
	}

	public void setTurtleFormatterVersion(String turtleFormatterVersion) {
		this.turtleFormatterVersion = turtleFormatterVersion;
	}

	public static class Builder {
		RdfFormatterConfig config = new RdfFormatterConfig();

		public Builder() {
		}

		public Builder failOnWarning(){
			return this.failOnWarning(true);
		}

		public Builder failOnWarning(boolean fail){
			this.config.setFailOnWarning(fail);
			return this;
		}

		public Builder useTurtleFormatter(){
			return this.useTurtleFormatter(true);
		}

		public Builder useTurtleFormatter(boolean useTurtleFormatter){
			this.config.setUseTurtleFormatter(useTurtleFormatter);
			return this;
		}

		public Builder turtleFormatterVersion(String version){
			this.config.turtleFormatterVersion = version;
			return this;
		}

		public RdfFormatterConfig build(){
			return config;
		}

		public Builder verify(boolean verify) {
			this.config.verify = verify;
			return this;
		}

		public Builder verify() {
			this.config.verify = true;
			return this;
		}
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RdfFormatterConfig))
			return false;
		RdfFormatterConfig that = (RdfFormatterConfig) o;
		return isFailOnWarning() == that.isFailOnWarning() && isUseTurtleFormatter() == that.isUseTurtleFormatter()
			&& Objects.equals(turtleFormatterVersion, that.turtleFormatterVersion);
	}

	@Override public int hashCode() {
		return Objects.hash(isFailOnWarning(), isUseTurtleFormatter(), turtleFormatterVersion);
	}
}
